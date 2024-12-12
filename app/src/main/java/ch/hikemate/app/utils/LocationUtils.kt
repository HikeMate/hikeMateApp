package ch.hikemate.app.utils

import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import android.widget.Toast
import ch.hikemate.app.R
import ch.hikemate.app.model.route.DetailedHike
import ch.hikemate.app.model.route.LatLong
import ch.hikemate.app.model.route.RouteSegment
import ch.hikemate.app.ui.map.MapScreen
import ch.hikemate.app.utils.RouteUtils.RouteProjectionResponse
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
        MapUtils.centerMapOnLocation(context, mapView, userLocationMarker)
      }
    }

    // The user just revoked location permission for the app, stop location features
    else {
      stopUserLocationUpdates(context, locationUpdatedCallback)
      MapUtils.clearUserPosition(userLocationMarker, mapView, invalidate = true)
    }
  }

  /**
   * Projects a location onto the nearest point of a hiking route. This is a greedy approach that
   * finds the closest segment.
   *
   * @param location The current location to project
   * @param route The hiking route to project onto
   * @return RouteProjectionResponse containing projection details
   */
  fun projectLocationOnHike(location: LatLong, route: DetailedHike): RouteProjectionResponse? {
    // Validate input
    if (route.waypoints.size < 2) return null

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
    var projectedPoint = route.waypoints[0]
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
   * Projects a point onto a route segment.
   *
   * @param point
   * @param segment
   * @return projection
   */
  private inline fun projectPointOntoSegment(point: LatLong, segment: RouteSegment): LatLong {
    return point.projectPointOntoLine(segment.start, segment.end)
  }
}
