package ch.hikemate.app.utils

import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import android.widget.Toast
import ch.hikemate.app.R
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.LatLong
import ch.hikemate.app.model.route.RouteProjectionResponse
import ch.hikemate.app.model.route.RouteSegment
import ch.hikemate.app.ui.map.MapScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

object LocationUtils {
  private const val LOG_TAG = "LocationUtils"

  /**
   * Util function to check if the user has granted the location permission.
   *
   * Does not check whether fine location permission has been granted, checks that at least coarse
   * location was granted.
   *
   * @param locationPermissionState The state of the location permission
   * @return True if the user has granted the location permission, false otherwise
   */
  @OptIn(ExperimentalPermissionsApi::class)
  fun hasLocationPermission(locationPermissionState: MultiplePermissionsState): Boolean {
    return PermissionUtils.anyPermissionGranted(locationPermissionState)
  }

  fun getUserLocation(
      context: Context,
      onLocation: (Location?) -> Unit,
      priority: Int = Priority.PRIORITY_BALANCED_POWER_ACCURACY,
      cancellationToken: CancellationToken = CancellationTokenSource().token
  ) {
    // Create a client to work with user location
    val fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    try {
      fusedLocationProviderClient
          // Ask for the user's current location
          .getCurrentLocation(priority, cancellationToken)
          // In case of success, call the callback with the location
          .addOnSuccessListener { onLocation(it) }
          // In case of failure, call the callback with null
          .addOnFailureListener {
            Log.e(LOG_TAG, "Error while accessing location", it)
            onLocation(null)
          }
    }
    // If an error occurs (lack of permission), call the callback with null
    catch (e: SecurityException) {
      Log.e(LOG_TAG, "Security exception while accessing location", e)
      onLocation(null)
    }
  }

  /**
   * Starts listening for user location updates. Make sure to stop the updates when they are no
   * longer needed to avoid draining the battery, or when the permission is revoked.
   *
   * This function handles exceptions when the location permission is not granted, but won't disable
   * updates even if a [SecurityException] is caught. It is up to the caller to handle the exception
   * and stop the updates if needed.
   *
   * @param context The context where the location updates will be requested
   * @param locationCallback The callback that will be called when the user's location is updated
   * @see [stopUserLocationUpdates]
   */
  fun startUserLocationUpdates(context: Context, locationCallback: LocationCallback): Boolean {
    // Create a client to work with user location
    val fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Calls can produce SecurityExceptions if the permission is not granted
    try {
      // Request periodic updates of the user's location when it changes
      fusedLocationProviderClient.requestLocationUpdates(
          LocationRequest.Builder(
                  // Whether to prioritize battery or position accuracy
                  Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                  // Update interval in milliseconds
                  MapScreen.USER_LOCATION_UPDATE_INTERVAL)
              .build(),
          locationCallback,
          Looper.getMainLooper())

      Log.d(LOG_TAG, "User location updates requested through FusedLocationProviderClient")

      // Periodic updates were started successfully
      return true
    } catch (e: SecurityException) {
      // Periodic updates could not be started
      return false
    }
  }

  /**
   * Stops listening for user location updates. Make sure to stop the updates when they are no
   * longer needed to avoid draining the battery, or when the permission is revoked.
   *
   * @param context The context where the location updates were requested
   * @param locationCallback The callback that was called when the user's location was updated
   * @see [startUserLocationUpdates]
   */
  fun stopUserLocationUpdates(context: Context, locationCallback: LocationCallback) {
    // Create a client to work with user location
    val fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Notify the client we don't want location updates anymore
    fusedLocationProviderClient.removeLocationUpdates(locationCallback)

    Log.d(LOG_TAG, "User location updates stopped through FusedLocationProviderClient")
  }

  /**
   * Handles a user location update received by the location callback.
   *
   * Does not display any feedback to the user (toast, ...).
   *
   * If no location is available, the user's position is cleared from the map.
   *
   * @param locationResult The location update result received by the callback
   * @param mapView The map view where the user's location will be displayed
   * @param userLocationMarker The marker representing the user's location on the map
   * @return The updated user location marker, if any, or null if no position was available.
   */
  fun onUserLocationUpdate(
      locationResult: LocationResult,
      mapView: MapView,
      userLocationMarker: Marker?
  ): Marker? {
    // Extract the actual location from the update result
    val location = locationResult.lastLocation

    Log.d(LOG_TAG, "User location update received: $locationResult")

    // The result might be null if the location is not available
    var result: Marker? = null
    if (location != null) {
      // If a new position is available, show it on the map
      result = MapUtils.updateUserPosition(userLocationMarker, mapView, location)
    } else {
      Log.e(LOG_TAG, "User location update contains null location")

      // If no location is available, clear the user's position from the map
      MapUtils.clearUserPosition(userLocationMarker, mapView, invalidate = true)
    }

    // Return the updated user location marker, if any
    return result
  }

  /**
   * To be called when the user grants a new location permission or revokes one.
   *
   * @param context The context where the location permission was updated
   * @param locationPermissionState State about the location revoked/granted permissions
   * @param mapView Map view used to update the user's location or center the map if needed
   * @param locationUpdatedCallback Callback used to start or stop listening for location updates
   * @param centerMapOnUserPosition Whether the map should be centered on the user's position
   * @param userLocationMarker The marker representing the user's location on the map
   * @return The new value of centerMapOnUserPosition. If the map was centered on the user's
   *   position, the value will be false, otherwise the same value as provided in parameters will be
   *   returned.
   */
  @OptIn(ExperimentalPermissionsApi::class)
  fun onLocationPermissionsUpdated(
      context: Context,
      locationPermissionState: MultiplePermissionsState,
      mapView: MapView,
      locationUpdatedCallback: LocationCallback,
      centerMapOnUserPosition: Boolean,
      userLocationMarker: Marker?
  ) {
    Log.d(
        MapScreen.LOG_TAG,
        "Location permission changed (revoked: ${locationPermissionState.revokedPermissions})")
    val hasLocationPermission = hasLocationPermission(locationPermissionState)

    // The user just enabled location permission for the app, start location features
    if (hasLocationPermission) {
      Log.d(MapScreen.LOG_TAG, "Location permission granted, requesting location updates")
      PermissionUtils.setFirstTimeAskingPermission(
          context, android.Manifest.permission.ACCESS_FINE_LOCATION, true)
      PermissionUtils.setFirstTimeAskingPermission(
          context, android.Manifest.permission.ACCESS_COARSE_LOCATION, true)
      val featuresEnabledSuccessfully = startUserLocationUpdates(context, locationUpdatedCallback)
      if (!featuresEnabledSuccessfully) {
        Log.e(MapScreen.LOG_TAG, "Failed to enable location features")
        Toast.makeText(
                context,
                context.getString(R.string.map_screen_location_features_failed),
                Toast.LENGTH_LONG)
            .show()
      }
      if (centerMapOnUserPosition) {
        MapUtils.centerMapOnUserLocation(context, mapView, userLocationMarker)
      }
    }

    // The user just revoked location permission for the app, stop location features
    else {
      stopUserLocationUpdates(context, locationUpdatedCallback)
      MapUtils.clearUserPosition(userLocationMarker, mapView, invalidate = true)
    }
  }

  /**
   * Projects a location onto the nearest point of a hiking route when first starting tracking. This
   * is a greedy approach that finds the closest segment.
   *
   * @param location The current location to project
   * @param route The hiking route to project onto
   * @return RouteProjectionResponse containing projection details
   */
  fun projectLocationOnStart(location: LatLong, route: HikeRoute): RouteProjectionResponse? {
    // Validate input
    if (route.ways.size < 2) return null

    val segments = route.segments

    // Handle simple case of just 2 points
    if (segments.size == 1) {
      val segment = segments[0]
      val projected = projectPointOntoSegment(location, segment)
      return RouteProjectionResponse(
          projectedLocation = projected,
          progressDistance = projected.distanceTo(segment.start),
          distanceFromRoute = location.distanceTo(projected),
          segment = segment,
          indexToSegment = 0)
    }

    // Find closest segment by checking all segment
    var closestSegment = segments[0]
    var closestSegmentIndex = 0
    var minDistance = Double.MAX_VALUE
    var projectedPoint = route.ways[0]
    // The distance that has been traveled according to the projection
    var progressDistance = 0.0
    var distanceCovered = 0.0

    segments.forEachIndexed { index, segment ->
      val projected = projectPointOntoSegment(location, segment)
      val distance = location.distanceTo(projected)

      // If the distance to that projection is smaller than update the projection for the new one.
      if (distance < minDistance) {
        minDistance = distance
        projectedPoint = projected
        progressDistance = distanceCovered + projected.distanceTo(segment.start)
        closestSegment = segment
        closestSegmentIndex = index
      }
      distanceCovered += segment.length
    }

    return RouteProjectionResponse(
        projectedLocation = projectedPoint,
        progressDistance = progressDistance,
        distanceFromRoute = minDistance,
        segment = closestSegment,
        indexToSegment = closestSegmentIndex)
  }

  /**
   * Wrapping function to call the projectLocationOnGoingHelper with the correct attributes.
   *
   * @param location
   * @param route
   * @param lastProjectionResponse
   * @param threshold
   * @return the RouteProjectionResponse containing the best projection found
   */
  fun projectLocationOngoing(
      location: LatLong,
      route: HikeRoute,
      lastProjectionResponse: RouteProjectionResponse,
      threshold: Double
  ): RouteProjectionResponse {
    val segment = lastProjectionResponse.segment
    val projection = lastProjectionResponse.projectedLocation
    val distanceInSegment = projection.distanceTo(segment.start)
    return projectLocationOngoingHelper(
        location,
        route,
        projection,
        segment,
        lastProjectionResponse.indexToSegment,
        lastProjectionResponse.progressDistance - distanceInSegment,
        threshold)
  }

  /**
   * Projects a location onto a hiking route during ongoing tracking by recursively checking nearby
   * segments.
   *
   * The algorithm works as follows:
   * 1. Projects the current location onto the current segment and checks if it's within the
   *    threshold distance. If so, updates the projection while staying on the same segment.
   * 2. If beyond threshold, determines movement direction by comparing distances:
   *     - Forward movement: New point is further from segment start AND closer to segment end
   *     - Backward movement: New point is closer to segment start AND further from segment end
   * 3. For forward movement:
   *     - Recursively checks the next segment
   *     - Compares distances and keeps the closer projection (current or next segment)
   * 4. For backward movement:
   *     - Recursively checks the previous segment
   *     - Compares distances and keeps the closer projection (current or previous segment)
   * 5. If no better projection is found, maintains the current projection
   *
   * @param location Current location to project onto the route
   * @param route The hiking route containing all segments
   * @param lastProjection The last known projection result
   * @param currentSegment The segment for the point to be projected
   * @param indexToSegment The index of the segment in the list.
   * @param distanceToLastSegmentStart The distance of all the summed segments from the start of the
   *   route
   * @param threshold Maximum allowed distance (in meters) between location and projected point
   * @return Updated RouteProjectionResponse containing the best projection found
   */
  private fun projectLocationOngoingHelper(
      location: LatLong,
      route: HikeRoute,
      lastProjection: LatLong,
      currentSegment: RouteSegment,
      indexToSegment: Int,
      distanceToLastSegmentStart: Double,
      threshold: Double
  ): RouteProjectionResponse {

    val projected = projectPointOntoSegment(location, currentSegment)
    val distanceToProjected = location.distanceTo(projected)

    val currentProjection =
        RouteProjectionResponse(
            projectedLocation = projected,
            segment = currentSegment,
            progressDistance =
                distanceToLastSegmentStart + projected.distanceTo(currentSegment.start),
            distanceFromRoute = distanceToProjected,
            indexToSegment = indexToSegment)

    // If within the threshold return the projection
    if (distanceToProjected < threshold) return currentProjection

    // Check if the user is moving forward along route according to the current projection
    if (projected.distanceTo(currentSegment.start) >=
        lastProjection.distanceTo(currentSegment.start) &&
        projected.distanceTo(currentSegment.end) <= lastProjection.distanceTo(currentSegment.end)) {
      // If it's the last segment there will not be better projections
      if (indexToSegment == route.segments.lastIndex) return currentProjection

      // Recursive call
      val nextProjection =
          projectLocationOngoingHelper(
              location,
              route,
              currentSegment.end,
              route.segments[indexToSegment + 1],
              indexToSegment + 1,
              distanceToLastSegmentStart + currentSegment.length,
              threshold)

      // Keep whichever projection is closer
      return if (nextProjection.distanceFromRoute < currentProjection.distanceFromRoute) {
        nextProjection
      } else {
        currentProjection
      }
    }

    // Check if user moving backwards along route according to the current projection.
    else if (projected.distanceTo(currentSegment.start) <=
        lastProjection.distanceTo(currentSegment.start) &&
        projected.distanceTo(currentSegment.end) >= lastProjection.distanceTo(currentSegment.end)) {

      // If it's the first segment there will not be better projections
      if (indexToSegment == 0) return currentProjection

      // Recursive call
      val nextProjection =
          projectLocationOngoingHelper(
              location,
              route,
              currentSegment.start,
              route.segments[indexToSegment - 1],
              indexToSegment - 1,
              distanceToLastSegmentStart - currentSegment.length,
              threshold)

      // Keep whichever projection is closer
      return if (nextProjection.distanceFromRoute < currentProjection.distanceFromRoute) {
        nextProjection
      } else {
        currentProjection
      }
    }

    return currentProjection
  }

  /**
   * Projects a point onto a route segment.
   *
   * @param point
   * @param segment
   * @return projection
   */
  private fun projectPointOntoSegment(point: LatLong, segment: RouteSegment): LatLong {
    return point.projectPointIntoLine(segment.start, segment.end)
  }
}
