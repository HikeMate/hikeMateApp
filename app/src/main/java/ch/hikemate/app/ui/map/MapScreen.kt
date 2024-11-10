package ch.hikemate.app.ui.map

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.hikemate.app.R
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.ListOfHikeRoutesViewModel
import ch.hikemate.app.ui.components.HikeCard
import ch.hikemate.app.ui.components.HikeCardStyleProperties
import ch.hikemate.app.ui.navigation.LIST_TOP_LEVEL_DESTINATIONS
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.navigation.SideBarNavigation
import ch.hikemate.app.utils.PermissionUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

object MapScreen {
  /**
   * (Config) Arbitrary number defined empirically to avoid performance issues caused by drawing too
   * many hikes on the map when the user zoomed out too much and launched a search.
   *
   * The number is arbitrary and can be adjusted based on the performance of the app. As an
   * indication, it was tested with 149 hikes and already skipped a few frames, enough for a user to
   * be surprised by the lag.
   */
  const val MAX_HIKES_DRAWN_ON_MAP = 100

  /**
   * (Config) Height of the bottom sheet when it is collapsed. The height is defined empirically to
   * show a few items of the list of hikes and allow the user to expand it to see more.
   */
  val BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT = 400.dp

  /**
   * (Config) Initial zoom level of the map. The zoom level is defined empirically to show a
   * reasonable area of the map when the user opens the screen.
   */
  const val MAP_INITIAL_ZOOM = 12.0

  /**
   * (Config) Maximum zoom level of the map. The zoom level is defined empirically to avoid the user
   * zooming in too much and seeing the map tiles pixelated.
   */
  const val MAP_MAX_ZOOM = 18.0

  /**
   * (Config) Minimum zoom level of the map. The zoom level is defined empirically to avoid the user
   * zooming out too much and seeing too much of the blank background behind the map tiles while
   * still being able to see a reasonable area of the world map.
   */
  const val MAP_MIN_ZOOM = 3.0

  /** (Config) Initial position of the center of the map. */
  val MAP_INITIAL_CENTER = GeoPoint(46.5, 6.6)

  /** (Config) Width of the stroke of the lines that represent the hikes on the map. */
  const val STROKE_WIDTH = 10f

  /**
   * (Config) Interval in milliseconds between updates of the user's location. The interval is
   * defined empirically to avoid draining the battery too much while still providing a good
   * experience to the user.
   */
  private const val USER_LOCATION_UPDATE_INTERVAL = 5000L

  /**
   * (Config) Duration in milliseconds of the animation when centering the map on the user's
   * location. The duration is defined empirically to make the transition smooth and not too fast.
   * The value is arbitrary and can be adjusted based on the user's experience.
   */
  private const val CENTER_MAP_ANIMATION_TIME = 500L

  /**
   * (Config) Duration in milliseconds of the animation when centering the map on a marker that was
   * clicked. This duration is shorter than the center animation, because if the marker was clicked,
   * it means it is already on the screen, hence the distance is minimal.
   */
  private const val CENTER_MAP_ON_MARKER_ANIMATION_TIME = 200L

  /**
   * (Config) Size of the icon representing the user's location on the map. The size is defined
   * empirically to make the icon visible and not too big.
   */
  private const val USER_LOCATION_MARKER_ICON_SIZE = 40

  // These are the limits of the map. They are defined by the
  // latitude values that the map can display.
  // The latitude goes from -85 to 85, because going beyond
  // that would (weirdly) let the user see the blank
  const val MAP_MAX_LATITUDE = 85.0
  const val MAP_MIN_LATITUDE = -85.0

  const val MIN_HUE = 0
  const val MAX_HUE = 360
  const val MIN_SATURATION = 42
  const val MAX_SATURATION = 98
  const val MIN_LIGHTNESS = 40
  const val MAX_LIGHTNESS = 90

  const val LOG_TAG = "MapScreen"

  const val TEST_TAG_MAP = "map"
  const val TEST_TAG_SEARCH_BUTTON = "searchButton"
  const val TEST_TAG_HIKES_LIST = "hikesList"
  const val TEST_TAG_HIKE_ITEM = "hikeItem"
  const val TEST_TAG_EMPTY_HIKES_LIST_MESSAGE = "emptyHikesListMessage"
  const val TEST_TAG_SEARCHING_MESSAGE = "searchingMessage"
  const val TEST_TAG_SEARCH_LOADING_ANIMATION = "searchLoadingAnimation"

  const val MINIMAL_SEARCH_TIME_IN_MS = 500 // ms

  /**
   * Clears the marker representing the user's position from the map.
   *
   * The map is NOT invalidated. If needed, use [org.osmdroid.views.MapView.invalidate].
   *
   * To update the user's position to a new location, use [updateUserPosition].
   *
   * @param previous The previous marker representing the user's position
   * @param mapView The map view where the marker is displayed
   */
  private fun clearUserPosition(previous: Marker?, mapView: MapView, invalidate: Boolean = false) {
    previous?.let { mapView.overlays.remove(it) }
    if (invalidate) {
      mapView.invalidate()
    }
  }

  /**
   * Get the icon to use for the user's location marker.
   *
   * @param context The context where the icon will be used
   * @return The icon to use for the user's location marker
   */
  private fun getUserLocationMarkerIcon(context: Context): Drawable {
    // Retrieve the actual drawable resource
    val originalDrawable = AppCompatResources.getDrawable(context, R.drawable.user_location)

    // Resize the vector resource to look good on the map
    val bitmap =
        Bitmap.createBitmap(
            USER_LOCATION_MARKER_ICON_SIZE, USER_LOCATION_MARKER_ICON_SIZE, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    originalDrawable?.setBounds(0, 0, canvas.width, canvas.height)
    originalDrawable?.draw(canvas)

    return BitmapDrawable(context.resources, bitmap)
  }

  /**
   * Draws a new marker on the map representing the user's position. The previous marker is cleared
   * to avoid duplicates. The map is invalidated to redraw the map with the new marker.
   *
   * @param previous The previous marker representing the user's position
   * @param mapView The map view where the marker will be displayed
   * @param location The new location of the user
   * @return The new marker representing the user's position
   */
  private fun updateUserPosition(previous: Marker?, mapView: MapView, location: Location): Marker {
    // Clear the previous marker to avoid duplicates
    clearUserPosition(previous, mapView)

    // Create a new marker with the new position
    val newMarker =
        Marker(mapView).apply {
          icon = getUserLocationMarkerIcon(mapView.context)
          position = GeoPoint(location.latitude, location.longitude)
          setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
          setOnMarkerClickListener { marker, mapView ->
            mapView.controller.animateTo(
                marker.position, mapView.zoomLevelDouble, CENTER_MAP_ON_MARKER_ANIMATION_TIME)
            true
          }
        }

    // Add the new marker to the map
    mapView.overlays.add(newMarker)
    mapView.invalidate()

    return newMarker
  }

  /**
   * Centers the map on the user's location.
   *
   * Animates the map so that the transition is smooth and not instant.
   *
   * @param mapView The map view to center
   * @param location The user's location to center the map on
   */
  private fun centerMapOnUserLocation(mapView: MapView, location: Location) {
    mapView.controller.animateTo(
        GeoPoint(location.latitude, location.longitude),
        mapView.zoomLevelDouble,
        CENTER_MAP_ANIMATION_TIME)
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
  private fun startUserLocationUpdates(
      context: Context,
      locationCallback: LocationCallback
  ): Boolean {
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
                  USER_LOCATION_UPDATE_INTERVAL)
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
      result = updateUserPosition(userLocationMarker, mapView, location)
    } else {
      Log.e(LOG_TAG, "User location update contains null location")

      // If no location is available, clear the user's position from the map
      clearUserPosition(userLocationMarker, mapView, invalidate = true)
    }

    // Return the updated user location marker, if any
    return result
  }

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
    return locationPermissionState.revokedPermissions.size !=
        locationPermissionState.permissions.size
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
        LOG_TAG,
        "Location permission changed (revoked: ${locationPermissionState.revokedPermissions})")
    val hasLocationPermission = hasLocationPermission(locationPermissionState)

    // The user just enabled location permission for the app, start location features
    if (hasLocationPermission) {
      Log.d(LOG_TAG, "Location permission granted, requesting location updates")
      PermissionUtils.setFirstTimeAskingPermission(
          context, android.Manifest.permission.ACCESS_FINE_LOCATION, true)
      PermissionUtils.setFirstTimeAskingPermission(
          context, android.Manifest.permission.ACCESS_COARSE_LOCATION, true)
      val featuresEnabledSuccessfully = startUserLocationUpdates(context, locationUpdatedCallback)
      if (!featuresEnabledSuccessfully) {
        Log.e(LOG_TAG, "Failed to enable location features")
        Toast.makeText(
                context,
                context.getString(R.string.map_screen_location_features_failed),
                Toast.LENGTH_LONG)
            .show()
      }
      if (centerMapOnUserPosition) {
        centerMapOnUserLocation(context, mapView)
      }
    }

    // The user just revoked location permission for the app, stop location features
    else {
      stopUserLocationUpdates(context, locationUpdatedCallback)
      clearUserPosition(userLocationMarker, mapView, invalidate = true)
    }
  }

  fun centerMapOnUserLocation(context: Context, mapView: MapView) {
    val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    try {
      fusedLocationClient
          .getCurrentLocation(
              Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
          .addOnSuccessListener {
            if (it != null) {
              centerMapOnUserLocation(mapView, it)
            } else {
              Log.e(LOG_TAG, "Location obtained in centerMapOnUserLocation is null")
            }
          }
          .addOnFailureListener {
            Log.e(LOG_TAG, "Error while accessing location in centerMapOnUserLocation", it)
          }
    } catch (e: SecurityException) {
      Log.e(LOG_TAG, "Security exception while accessing location in centerMapOnUserLocation", e)
    }
  }

  /**
   * Launches a search for hikes in the area displayed on the map. The search is launched only if it
   * is not already ongoing.
   *
   * @param isSearching Whether a search is already ongoing
   * @param setIsSearchingToTrue Function to set the search state to true
   * @param setIsSearchingToFalse Function to set the search state to false
   * @param hikingRoutesViewModel The view model to use to search for hikes
   * @param mapView The map view where the search area is defined
   * @param context The context where the search is launched
   */
  fun launchSearch(
      isSearching: Boolean,
      setIsSearchingToTrue: () -> Unit,
      setIsSearchingToFalse: () -> Unit,
      hikingRoutesViewModel: ListOfHikeRoutesViewModel,
      mapView: MapView,
      context: Context
  ) {
    if (isSearching) return
    setIsSearchingToTrue()
    val startTime = System.currentTimeMillis()
    hikingRoutesViewModel.setArea(
        mapView.boundingBox,
        onSuccess = {
          if (System.currentTimeMillis() - startTime < MINIMAL_SEARCH_TIME_IN_MS) {
            Thread.sleep(MINIMAL_SEARCH_TIME_IN_MS - (System.currentTimeMillis() - startTime))
          }
          setIsSearchingToFalse()
        },
        onFailure = {
          setIsSearchingToFalse()
          Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Error while searching for hikes", Toast.LENGTH_SHORT).show()
          }
        })
  }
}

/**
 * Generates a random color in the HSL color space. The function uses HSL to generate a color
 * instead of ARGB because it makes it easier to have a range of colors that we know will render
 * well on the map.
 *
 * @return The generated color as an [Int].
 */
fun getRandomColor(): Int {
  fun randomInt(min: Int, max: Int): Int {
    return (min..max).random()
  }

  val h = randomInt(MapScreen.MIN_HUE, MapScreen.MAX_HUE).toFloat() // All colors
  val s =
      randomInt(MapScreen.MIN_SATURATION, MapScreen.MAX_SATURATION) /
          100.0f // Saturation between 42% and 98%
  val l =
      randomInt(MapScreen.MIN_LIGHTNESS, MapScreen.MAX_LIGHTNESS) /
          100.0f // Lightness between 40% and 90%

  return Color.hsl(h, s, l).toArgb()
}

/**
 * Shows a hike on the map.
 *
 * @param mapView The map view where the hike will be shown.
 * @param hike The hike to be shown.
 * @param color The color of the hike.
 */
fun showHikeOnMap(mapView: MapView, hike: HikeRoute, color: Int) {
  val line = Polyline()

  line.setPoints(hike.ways.map { GeoPoint(it.lat, it.lon) })
  line.outlinePaint.color = color
  line.outlinePaint.strokeWidth = MapScreen.STROKE_WIDTH

  line.setOnClickListener { _, _, _ ->
    Toast.makeText(
            mapView.context,
            "Hike details not implemented yet. Hike ID: ${hike.id}",
            Toast.LENGTH_SHORT)
        .show()
    true
  }

  mapView.overlays.add(line)
}

/**
 * Clears all hikes that are displayed on the map. Intended to be used when the list of hikes
 * changes and new hikes need to be drawn.
 */
fun clearHikesFromMap(mapView: MapView, userLocationMarker: Marker?) {
  mapView.overlays.clear()

  // If there was a user location marker, do not clear this
  userLocationMarker?.let { mapView.overlays.add(it) }

  mapView.invalidate()
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    navigationActions: NavigationActions,
    hikingRoutesViewModel: ListOfHikeRoutesViewModel =
        viewModel(factory = ListOfHikeRoutesViewModel.Factory),
    mapInitialZoomLevel: Double = MapScreen.MAP_INITIAL_ZOOM,
    mapMaxZoomLevel: Double = MapScreen.MAP_MAX_ZOOM,
    mapMinZoomLevel: Double = MapScreen.MAP_MIN_ZOOM,
    mapInitialCenter: GeoPoint = MapScreen.MAP_INITIAL_CENTER,
) {
  val context = LocalContext.current
  val locationPermissionState =
      rememberMultiplePermissionsState(
          permissions =
              listOf(
                  android.Manifest.permission.ACCESS_FINE_LOCATION,
                  android.Manifest.permission.ACCESS_COARSE_LOCATION))
  var showLocationPermissionDialog by remember { mutableStateOf(false) }
  var centerMapOnUserPosition by remember { mutableStateOf(false) }

  // Only do the configuration on the first composition, not on every recomposition
  LaunchedEffect(Unit) {
    Configuration.getInstance().apply {
      // Set user-agent to avoid rejected requests
      userAgentValue = context.packageName

      // Allow for faster loading of tiles. Default OSMDroid value is 2.
      tileDownloadThreads = 4

      // Maximum number of tiles that can be downloaded at once. Default is 40.
      tileDownloadMaxQueueSize = 40

      // Maximum number of bytes that can be used by the tile file system cache. Default is 600MB.
      tileFileSystemCacheMaxBytes = 600L * 1024L * 1024L
    }
  }

  // Avoid re-creating the MapView on every recomposition
  val mapView = remember {
    MapView(context).apply {
      // Set map's initial state
      controller.setZoom(mapInitialZoomLevel)
      controller.setCenter(mapInitialCenter)
      // Limit the zoom to avoid the user zooming out or out too much
      minZoomLevel = mapMinZoomLevel
      maxZoomLevel = mapMaxZoomLevel
      // Avoid repeating the map when the user reaches the edge or zooms out
      // We keep the horizontal repetition enabled to allow the user to scroll the map
      // horizontally without limits (from Asia to America, for example)
      isHorizontalMapRepetitionEnabled = true
      isVerticalMapRepetitionEnabled = false
      // Disable built-in zoom controls since we have our own
      zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
      // Enable touch-controls such as pinch to zoom
      setMultiTouchControls(true)
      // Limit the vertical scrollable area to avoid the user scrolling too far
      setScrollableAreaLimitLatitude(MapScreen.MAP_MAX_LATITUDE, MapScreen.MAP_MIN_LATITUDE, 0)
    }
  }
  var userLocationMarker: Marker? by remember { mutableStateOf(null) }

  // We need to keep a reference to the instance of location callback, this way we can unregister
  // it using the same reference, for example when the permission is revoked.
  val locationUpdatedCallback = remember {
    object : LocationCallback() {
      override fun onLocationResult(locationResult: LocationResult) {
        userLocationMarker =
            MapScreen.onUserLocationUpdate(locationResult, mapView, userLocationMarker)
      }
    }
  }

  LaunchedEffect(locationPermissionState.revokedPermissions) {
    // Update the map and start/stop listening for location updates
    MapScreen.onLocationPermissionsUpdated(
        context,
        locationPermissionState,
        mapView,
        locationUpdatedCallback,
        centerMapOnUserPosition,
        userLocationMarker)

    // Once the update has been made, reset the flag to avoid re-centering the map
    centerMapOnUserPosition = false
  }

  // Keep track of whether a search for hikes is ongoing
  var isSearching by remember { mutableStateOf(false) }

  // Show hikes on the map
  val routes by hikingRoutesViewModel.hikeRoutes.collectAsState()
  LaunchedEffect(routes, isSearching) {
    if (isSearching) return@LaunchedEffect
    clearHikesFromMap(mapView, userLocationMarker)
    if (routes.size <= MapScreen.MAX_HIKES_DRAWN_ON_MAP) {
      routes.forEach { showHikeOnMap(mapView, it, getRandomColor()) }
      Log.d(MapScreen.LOG_TAG, "Displayed ${routes.size} hikes on the map")
    } else {
      routes.subList(0, MapScreen.MAX_HIKES_DRAWN_ON_MAP).forEach {
        showHikeOnMap(mapView, it, getRandomColor())
      }
      Toast.makeText(
              context,
              context.getString(
                  R.string.map_screen_too_many_hikes_message, MapScreen.MAX_HIKES_DRAWN_ON_MAP),
              Toast.LENGTH_LONG)
          .show()
      Log.d(MapScreen.LOG_TAG, "Too many hikes (${routes.size}) to display on the map")
    }
  }

  // Show a dialog to explain the user why the location permission is needed
  // Only shows when the user has clicked on the "center map on my position" button
  LocationPermissionAlertDialog(
      show = showLocationPermissionDialog,
      onConfirm = {
        showLocationPermissionDialog = false
        centerMapOnUserPosition = true
      },
      onDismiss = {
        showLocationPermissionDialog = false
        centerMapOnUserPosition = false
      },
      simpleMessage = !locationPermissionState.shouldShowRationale,
      locationPermissionState = locationPermissionState,
      context = context)

  DisposableEffect(Unit) {
    onDispose {
      MapScreen.stopUserLocationUpdates(context, locationUpdatedCallback)
      mapView.overlays.clear()
      mapView.onPause()
      mapView.onDetach()
    }
  }

  SideBarNavigation(
      onTabSelect = { navigationActions.navigateTo(it) },
      tabList = LIST_TOP_LEVEL_DESTINATIONS,
      selectedItem = Route.MAP) {
        Box(modifier = Modifier.fillMaxSize().testTag(Screen.MAP)) {
          // Jetpack Compose is a relatively recent framework for implementing Android UIs.
          // OSMDroid
          // is
          // an older library that uses Activities, the previous way of doing. The composable
          // AndroidView
          // allows us to use OSMDroid's legacy MapView in a Jetpack Compose layout.
          AndroidView(
              factory = { mapView },
              modifier =
                  Modifier.fillMaxSize()
                      .testTag(MapScreen.TEST_TAG_MAP)
                      .padding(bottom = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT))

          // Button to center the map on the user's location
          MapMyLocationButton(
              onClick = {
                val hasLocationPermission = MapScreen.hasLocationPermission(locationPermissionState)
                // If the user has granted at least one of the two permissions, center the map on
                // the user's location
                if (hasLocationPermission) {
                  MapScreen.centerMapOnUserLocation(context, mapView)
                }
                // If the user yet needs to grant the permission, show a custom educational alert
                else {
                  showLocationPermissionDialog = true
                }
              },
              modifier =
                  Modifier.align(Alignment.BottomStart)
                      .padding(bottom = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT + 8.dp))
          // Search button to request OSM for hikes in the displayed area
          if (!isSearching) {
            MapSearchButton(
                onClick = {
                  MapScreen.launchSearch(
                      isSearching,
                      { isSearching = true },
                      { isSearching = false },
                      hikingRoutesViewModel,
                      mapView,
                      context)
                },
                modifier =
                    Modifier.align(Alignment.BottomCenter)
                        .padding(bottom = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT + 8.dp))
          }
          // The zoom buttons are displayed on the bottom left of the screen
          ZoomMapButton(
              onZoomIn = { mapView.controller.zoomIn() },
              onZoomOut = { mapView.controller.zoomOut() },
              modifier =
                  Modifier.align(Alignment.BottomEnd)
                      .padding(bottom = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT + 8.dp))
          CollapsibleHikesList(hikingRoutesViewModel, isSearching)
          // Put SideBarNavigation after to make it appear on top of the map and HikeList
        }
      }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionAlertDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    simpleMessage: Boolean,
    locationPermissionState: MultiplePermissionsState,
    context: Context = LocalContext.current
) {
  if (!show) return

  AlertDialog(
      icon = {
        Icon(painter = painterResource(id = R.drawable.my_location), contentDescription = null)
      },
      title = { Text(text = stringResource(R.string.map_screen_location_rationale_title)) },
      text = {
        Text(
            text =
                stringResource(
                    if (simpleMessage) R.string.map_screen_location_rationale_simple
                    else R.string.map_screen_location_rationale))
      },
      onDismissRequest = onDismiss,
      confirmButton = {
        Button(
            onClick = {
              onConfirm()
              // If should show rationale is true, it is safe to launch permission requests
              if (locationPermissionState.shouldShowRationale) {
                locationPermissionState.launchMultiplePermissionRequest()
              }

              // If the user is asked for the first time, it is safe to launch permission requests
              else if (PermissionUtils.firstTimeAskingPermission(
                  context, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                PermissionUtils.setFirstTimeAskingPermission(
                    context, android.Manifest.permission.ACCESS_FINE_LOCATION, false)
                PermissionUtils.setFirstTimeAskingPermission(
                    context, android.Manifest.permission.ACCESS_COARSE_LOCATION, false)
                locationPermissionState.launchMultiplePermissionRequest()
              }

              // Otherwise, the user should be brought to the settings page
              else {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)))
              }
            }) {
              Text(text = stringResource(R.string.map_screen_location_rationale_grant_button))
            }
      },
      dismissButton = {
        Button(onClick = onDismiss) {
          Text(text = stringResource(R.string.map_screen_location_rationale_cancel_button))
        }
      })
}

@Composable
fun MapSearchButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
  Button(
      onClick = onClick,
      modifier = modifier.testTag(MapScreen.TEST_TAG_SEARCH_BUTTON),
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Text(
            text = LocalContext.current.getString(R.string.map_screen_search_button_text),
            color = MaterialTheme.colorScheme.onSurface)
      }
}

@Composable
fun MapMyLocationButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
  IconButton(
      onClick = onClick,
      modifier = modifier,
      colors =
          IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Icon(
            painter = painterResource(id = R.drawable.my_location),
            tint = MaterialTheme.colorScheme.onSurface,
            contentDescription =
                stringResource(R.string.map_screen_center_on_pos_content_description))
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsibleHikesList(hikingRoutesViewModel: ListOfHikeRoutesViewModel, isSearching: Boolean) {
  val scaffoldState = rememberBottomSheetScaffoldState()
  val routes = hikingRoutesViewModel.hikeRoutes.collectAsState()
  val context = LocalContext.current

  // BottomSheetScaffold adds a layout at the bottom of the screen that the user can expand to view
  // the list of hikes
  BottomSheetScaffold(
      scaffoldState = scaffoldState,
      sheetContainerColor = MaterialTheme.colorScheme.surface,
      sheetContent = {
        Column(modifier = Modifier.fillMaxSize().testTag(MapScreen.TEST_TAG_HIKES_LIST)) {
          LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (isSearching) {
              item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()) {
                      Text(
                          text = "Searching for hikes...",
                          style = MaterialTheme.typography.bodyLarge,
                          textAlign = TextAlign.Center,
                          modifier =
                              Modifier.padding(bottom = 16.dp)
                                  .testTag(MapScreen.TEST_TAG_SEARCHING_MESSAGE))
                      CircularProgressIndicator(
                          modifier = Modifier.testTag(MapScreen.TEST_TAG_SEARCH_LOADING_ANIMATION))
                    }
              }
            } else if (routes.value.isEmpty()) {
              item {
                // Use a box to center the Text composable of the empty list message
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                  Text(
                      text = context.getString(R.string.map_screen_empty_hikes_list_message),
                      style = MaterialTheme.typography.bodyLarge,
                      // Align the text within the Text composable to the center
                      textAlign = TextAlign.Center,
                      modifier = Modifier.testTag(MapScreen.TEST_TAG_EMPTY_HIKES_LIST_MESSAGE))
                }
              }
            } else {
              items(routes.value.size, key = { routes.value[it].id }) { index: Int ->
                val route = routes.value[index]
                val isSuitable = index % 2 == 0
                HikeCardFor(route, isSuitable, hikingRoutesViewModel)
              }
            }
          }
        }
      },
      sheetPeekHeight = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT) {}
}

@Composable
fun HikeCardFor(route: HikeRoute, isSuitable: Boolean, viewModel: ListOfHikeRoutesViewModel) {
  // The context is needed to display a toast when the user clicks on the route
  val context = LocalContext.current

  // The color of the card's message is chosen based on whether the hike is suitable or not
  val suitableLabelColor = if (isSuitable) Color(0xFF4CAF50) else Color(0xFFFFC107)

  // The text and icon of the card's message are chosen based on whether the hike is suitable or not
  val suitableLabelText =
      if (isSuitable) LocalContext.current.getString(R.string.map_screen_suitable_hike_label)
      else LocalContext.current.getString(R.string.map_screen_challenging_hike_label)

  // The icon of the card's message is chosen based on whether the hike is suitable or not
  val suitableLabelIcon = if (isSuitable) R.drawable.check_circle else R.drawable.warning

  HikeCard(
      title = route.name ?: stringResource(R.string.map_screen_hike_title_default),
      // This generates a random list of elevation data for the hike
      // with a random number of points and altitude between 0 and 1000
      elevationData = (0..(0..1000).random()).map { it.toDouble() }.shuffled(),
      onClick = {
        // The user clicked on the route to select it
        viewModel.selectRoute(route)
        Toast.makeText(context, "Hike details not implemented yet", Toast.LENGTH_SHORT).show()
      },
      messageContent = suitableLabelText,
      styleProperties =
          HikeCardStyleProperties(
              messageIcon = painterResource(suitableLabelIcon), messageColor = suitableLabelColor))
}
