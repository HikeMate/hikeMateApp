package ch.hikemate.app.ui.map

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.hikemate.app.R
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.model.profile.HikingLevel
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.ListOfHikeRoutesViewModel
import ch.hikemate.app.ui.components.AsyncStateHandler
import ch.hikemate.app.ui.components.HikeCard
import ch.hikemate.app.ui.components.HikeCardStyleProperties
import ch.hikemate.app.ui.navigation.BottomBarNavigation
import ch.hikemate.app.ui.navigation.LIST_TOP_LEVEL_DESTINATIONS
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.utils.LocationUtils
import ch.hikemate.app.utils.MapUtils
import ch.hikemate.app.utils.PermissionUtils
import ch.hikemate.app.utils.RouteUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

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
  const val USER_LOCATION_UPDATE_INTERVAL = 5000L

  /**
   * (Config) Duration in milliseconds of the animation when centering the map on the user's
   * location. The duration is defined empirically to make the transition smooth and not too fast.
   * The value is arbitrary and can be adjusted based on the user's experience.
   */
  const val CENTER_MAP_ANIMATION_TIME = 500L

  /**
   * (Config) Duration in milliseconds of the animation when centering the map on a marker that was
   * clicked. This duration is shorter than the center animation, because if the marker was clicked,
   * it means it is already on the screen, hence the distance is minimal.
   */
  const val CENTER_MAP_ON_MARKER_ANIMATION_TIME = 200L

  /**
   * (Config) Size of the icon representing the user's location on the map. The size is defined
   * empirically to make the icon visible and not too big.
   */
  const val USER_LOCATION_MARKER_ICON_SIZE = 40

  // These are the limits of the map. They are defined by the
  // latitude values that the map can display.
  // The latitude goes from -85 to 85, because going beyond
  // that would (weirdly) let the user see the blank
  const val MAP_MAX_LATITUDE = 85.0
  const val MAP_MIN_LATITUDE = -85.0

  const val LOG_TAG = "MapScreen"

  const val TEST_TAG_MAP = "MapScreenMap"
  const val TEST_TAG_SEARCH_BUTTON = "MapScreenSearchButton"
  const val TEST_TAG_HIKES_LIST = "MapScreenHikesList"
  const val TEST_TAG_HIKE_ITEM = "MapScreenHikeItem"
  const val TEST_TAG_EMPTY_HIKES_LIST_MESSAGE = "MapScreenEmptyHikesListMessage"
  const val TEST_TAG_SEARCHING_MESSAGE = "MapScreenSearchingMessage"
  const val TEST_TAG_SEARCH_LOADING_ANIMATION = "MapScreenSearchLoadingAnimation"
  const val TEST_TAG_CENTER_MAP_BUTTON = "MapScreenCenterMapButton"
  const val TEST_TAG_LOCATION_PERMISSION_ALERT = "MapScreenLocationPermissionAlert"
  const val TEST_TAG_NO_THANKS_ALERT_BUTTON = "MapScreenNoThanksAlertButton"
  const val TEST_TAG_GRANT_ALERT_BUTTON = "MapScreenGrantAlertButton"

  const val MINIMAL_SEARCH_TIME_IN_MS = 500 // ms

  /**
   * Launches a search for hikes in the area displayed on the map. The search is launched only if it
   * is not already ongoing.
   *
   * @param isSearching Whether a search is already ongoing
   * @param hikingRoutesViewModel The view model to use to search for hikes
   * @param mapView The map view where the search area is defined
   * @param context The context where the search is launched
   */
  fun launchSearch(
      isSearching: MutableState<Boolean>,
      hikingRoutesViewModel: ListOfHikeRoutesViewModel,
      mapView: MapView,
      context: Context
  ) {
    if (isSearching.value) return
    isSearching.value = true
    val startTime = System.currentTimeMillis()
    hikingRoutesViewModel.setArea(
        mapView.boundingBox,
        onSuccess = {
          if (System.currentTimeMillis() - startTime < MINIMAL_SEARCH_TIME_IN_MS) {
            Thread.sleep(MINIMAL_SEARCH_TIME_IN_MS - (System.currentTimeMillis() - startTime))
          }
          isSearching.value = false
        },
        onFailure = {
          isSearching.value = false
          Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Error while searching for hikes", Toast.LENGTH_SHORT).show()
          }
        })
  }
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

/**
 * Properties to initialize the map with. These properties are used to set the initial state of the
 * map, such as the zoom level, the center, and the limits of the zoom.
 *
 * @param mapInitialZoomLevel The initial zoom level of the map
 * @param mapMaxZoomLevel The maximum zoom level of the map
 * @param mapMinZoomLevel The minimum zoom level of the map
 * @param mapInitialCenter The initial center of the map
 */
data class MapInitialValues(
    val mapInitialZoomLevel: Double = MapScreen.MAP_INITIAL_ZOOM,
    val mapMaxZoomLevel: Double = MapScreen.MAP_MAX_ZOOM,
    val mapMinZoomLevel: Double = MapScreen.MAP_MIN_ZOOM,
    val mapInitialCenter: GeoPoint = MapScreen.MAP_INITIAL_CENTER,
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    navigationActions: NavigationActions,
    hikingRoutesViewModel: ListOfHikeRoutesViewModel =
        viewModel(factory = ListOfHikeRoutesViewModel.Factory),
    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
    authViewModel: AuthViewModel,
    mapInitialValues: MapInitialValues = MapInitialValues()
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

    if (authViewModel.currentUser.value == null) {
      Log.e("MapScreen", "User is not signed in")
      return@LaunchedEffect
    }
    profileViewModel.getProfileById(authViewModel.currentUser.value!!.uid)
  }

  // Avoid re-creating the MapView on every recomposition
  val mapView = remember {
    MapView(context).apply {
      // Set map's initial state
      controller.setZoom(mapInitialValues.mapInitialZoomLevel)
      controller.setCenter(mapInitialValues.mapInitialCenter)
      // Limit the zoom to avoid the user zooming out or out too much
      minZoomLevel = mapInitialValues.mapMinZoomLevel
      maxZoomLevel = mapInitialValues.mapMaxZoomLevel
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
            LocationUtils.onUserLocationUpdate(locationResult, mapView, userLocationMarker)
      }
    }
  }

  LaunchedEffect(locationPermissionState.revokedPermissions) {
    // Update the map and start/stop listening for location updates
    LocationUtils.onLocationPermissionsUpdated(
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
  val isSearching = remember { mutableStateOf(false) }

  // Show hikes on the map
  val routes by hikingRoutesViewModel.hikeRoutes.collectAsState()
  val selectedRoute = hikingRoutesViewModel.selectedHikeRoute.collectAsState().value

  LaunchedEffect(routes, isSearching) {
    if (isSearching.value) return@LaunchedEffect
    clearHikesFromMap(mapView, userLocationMarker)
    if (routes.size <= MapScreen.MAX_HIKES_DRAWN_ON_MAP) {
      routes.forEach {
        MapUtils.showHikeOnMap(
            mapView, it, it.getColor(), onLineClick = { hikingRoutesViewModel.selectRoute(it) })
      }
      Log.d(MapScreen.LOG_TAG, "Displayed ${routes.size} hikes on the map")
    } else {
      routes.subList(0, MapScreen.MAX_HIKES_DRAWN_ON_MAP).forEach {
        MapUtils.showHikeOnMap(
            mapView, it, it.getColor(), onLineClick = { hikingRoutesViewModel.selectRoute(it) })
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

  LaunchedEffect(selectedRoute) {
    if (selectedRoute != null) {
      navigationActions.navigateTo(Screen.HIKE_DETAILS)
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      LocationUtils.stopUserLocationUpdates(context, locationUpdatedCallback)
      mapView.overlays.clear()
      mapView.onPause()
      mapView.onDetach()
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
      LocationUtils.stopUserLocationUpdates(context, locationUpdatedCallback)
      mapView.overlays.clear()
      mapView.onPause()
      mapView.onDetach()
    }
  }

  val errorMessageIdState = profileViewModel.errorMessageId.collectAsState()
  val profileState = profileViewModel.profile.collectAsState()

  AsyncStateHandler(
      errorMessageIdState = errorMessageIdState,
      actionContentDescriptionStringId = R.string.go_back,
      actionOnErrorAction = { navigationActions.navigateTo(Route.MAP) },
      valueState = profileState) { profile ->
        BottomBarNavigation(
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
                      val hasLocationPermission =
                          LocationUtils.hasLocationPermission(locationPermissionState)
                      // If the user has granted at least one of the two permissions, center the map
                      // on
                      // the user's location
                      if (hasLocationPermission) {
                        MapUtils.centerMapOnUserLocation(context, mapView, userLocationMarker)
                      }
                      // If the user yet needs to grant the permission, show a custom educational
                      // alert
                      else {
                        showLocationPermissionDialog = true
                      }
                    },
                    modifier =
                        Modifier.align(Alignment.BottomStart)
                            .padding(bottom = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT + 8.dp)
                            .testTag(MapScreen.TEST_TAG_CENTER_MAP_BUTTON))
                // Search button to request OSM for hikes in the displayed area
                if (!isSearching.value) {
                  MapSearchButton(
                      onClick = {
                        MapScreen.launchSearch(isSearching, hikingRoutesViewModel, mapView, context)
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
                CollapsibleHikesList(hikingRoutesViewModel, profile.hikingLevel, isSearching.value)
                // Put SideBarNavigation after to make it appear on top of the map and HikeList
              }
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
      modifier = Modifier.testTag(MapScreen.TEST_TAG_LOCATION_PERMISSION_ALERT),
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
            modifier = Modifier.testTag(MapScreen.TEST_TAG_GRANT_ALERT_BUTTON),
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
        Button(
            modifier = Modifier.testTag(MapScreen.TEST_TAG_NO_THANKS_ALERT_BUTTON),
            onClick = onDismiss) {
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

/**
 * Composable for the "Center on my location" button.
 *
 * Represents an icon button with a "My Location" icon. The onclick callback is provided as a
 * parameter, but the button is meant to be used with the location permission to center the map on
 * the user's location.
 */
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
fun CollapsibleHikesList(
    hikingRoutesViewModel: ListOfHikeRoutesViewModel,
    userHikingLevel: HikingLevel,
    isSearching: Boolean
) {
  val scaffoldState = rememberBottomSheetScaffoldState()
  val routes = hikingRoutesViewModel.hikeRoutes.collectAsState()
  val context = LocalContext.current

  // Map of route ID to the elevation data of the route
  val elevationDataMappings = remember { mutableStateOf(emptyMap<String, List<Double>>()) }

  // BottomSheetScaffold adds a layout at the bottom of the screen that the user can expand to view
  // the list of hikes
  BottomSheetScaffold(
      scaffoldState = scaffoldState,
      sheetContainerColor = MaterialTheme.colorScheme.surface,
      sheetContent = {
        Column(modifier = Modifier.fillMaxSize().testTag(MapScreen.TEST_TAG_HIKES_LIST)) {
          if (isSearching) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()) {
                  Text(
                      text = stringResource(R.string.map_search_for_hikes),
                      style = MaterialTheme.typography.bodyLarge,
                      textAlign = TextAlign.Center,
                      modifier =
                          Modifier.padding(bottom = 16.dp)
                              .testTag(MapScreen.TEST_TAG_SEARCHING_MESSAGE))
                  CircularProgressIndicator(
                      modifier = Modifier.testTag(MapScreen.TEST_TAG_SEARCH_LOADING_ANIMATION))
                }
          } else if (routes.value.isEmpty()) {
            // Use a box to center the Text composable of the empty list message
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
              Text(
                  text = context.getString(R.string.map_screen_empty_hikes_list_message),
                  style = MaterialTheme.typography.bodyLarge,
                  // Align the text within the Text composable to the center
                  textAlign = TextAlign.Center,
                  modifier = Modifier.testTag(MapScreen.TEST_TAG_EMPTY_HIKES_LIST_MESSAGE))
            }
          } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
              items(routes.value.size, key = { routes.value[it].id }) { index: Int ->
                val route = routes.value[index]

                val distance = RouteUtils.computeTotalDistance(route.ways)
                val elevation =
                    RouteUtils.calculateElevationGain(
                        elevationDataMappings.value[route.id] ?: emptyList())
                val difficulty = RouteUtils.determineDifficulty(distance, elevation)

                val isSuitable = difficulty.ordinal <= userHikingLevel.ordinal

                hikingRoutesViewModel.getRoutesElevation(
                    route, { elevationDataMappings.value += (route.id to it) })
                Log.d("MapScreen", "Loading hike card for route: ${route.name}")
                HikeCardFor(
                    route, isSuitable, hikingRoutesViewModel, elevationDataMappings.value[route.id])
              }
            }
          }
        }
      },
      sheetPeekHeight = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT) {}
}

@Composable
fun HikeCardFor(
    route: HikeRoute,
    isSuitable: Boolean,
    viewModel: ListOfHikeRoutesViewModel,
    elevationData: List<Double>?,
) {
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
      elevationData = elevationData,
      onClick = {
        // The user clicked on the route to select it
        viewModel.selectRoute(route)
      },
      messageContent = suitableLabelText,
      styleProperties =
          HikeCardStyleProperties(
              messageIcon = painterResource(suitableLabelIcon),
              messageColor = suitableLabelColor,
              graphColor = Color(route.getColor())))
}
