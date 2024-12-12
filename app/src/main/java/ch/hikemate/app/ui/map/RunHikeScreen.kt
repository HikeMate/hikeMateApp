package ch.hikemate.app.ui.map

import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ch.hikemate.app.R
import ch.hikemate.app.model.route.DetailedHike
import ch.hikemate.app.model.route.HikesViewModel
import ch.hikemate.app.model.route.LatLong
import ch.hikemate.app.ui.components.BackButton
import ch.hikemate.app.ui.components.BigButton
import ch.hikemate.app.ui.components.ButtonType
import ch.hikemate.app.ui.components.CenteredErrorAction
import ch.hikemate.app.ui.components.DetailRow
import ch.hikemate.app.ui.components.ElevationGraph
import ch.hikemate.app.ui.components.ElevationGraphStyleProperties
import ch.hikemate.app.ui.components.LocationPermissionAlertDialog
import ch.hikemate.app.ui.components.WithDetailedHike
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.utils.LocationUtils
import ch.hikemate.app.utils.MapUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import kotlin.math.roundToInt
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

object RunHikeScreen {
  const val TEST_TAG_MAP = "runHikeScreenMap"
  const val TEST_TAG_BACK_BUTTON = "runHikeScreenBackButton"
  const val TEST_TAG_ZOOM_BUTTONS = "runHikeScreenZoomInButton"
  const val TEST_TAG_BOTTOM_SHEET = "runHikeScreenBottomSheet"
  const val TEST_TAG_HIKE_NAME = "runHikeScreenHikeName"
  const val TEST_TAG_ELEVATION_GRAPH = "runHikeScreenElevationGraph"
  const val TEST_TAG_STOP_HIKE_BUTTON = "runHikeScreenStopHikeButton"
  const val TEST_TAG_TOTAL_DISTANCE_TEXT = "runHikeScreenTotalDistanceText"
  const val TEST_TAG_PROGRESS_TEXT = "runHikeScreenProgressText"
  const val TEST_TAG_CENTER_MAP_BUTTON = "runHikeScreenCenterMapButton"

  // The maximum distance to be considered on a hike
  const val MAX_DISTANCE_TO_CONSIDER_HIKE = 50.0 // meters
}

@Composable
fun RunHikeScreen(
    hikesViewModel: HikesViewModel,
    navigationActions: NavigationActions,
) {
  val selectedHike by hikesViewModel.selectedHike.collectAsState()

  LaunchedEffect(selectedHike) {
    if (selectedHike == null) {
      navigationActions.goBack()
    }
  }

  // Can't display the details without a selected hike
  if (selectedHike == null) {
    return
  }

  val hike = selectedHike!!

  WithDetailedHike(
      hike = hike,
      hikesViewModel = hikesViewModel,
      withDetailedHike = { RunHikeContent(it, navigationActions) },
      whenError = {
        val loadingErrorMessageId by hikesViewModel.loadingErrorMessageId.collectAsState()

        CenteredErrorAction(
            errorMessageId = loadingErrorMessageId ?: R.string.loading_hike_error,
            actionIcon = Icons.AutoMirrored.Filled.ArrowBack,
            actionContentDescriptionStringId = R.string.go_back,
            onAction = { navigationActions.goBack() })
      })
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RunHikeContent(hike: DetailedHike, navigationActions: NavigationActions) {
  val context = LocalContext.current
  val locationPermissionState =
      rememberMultiplePermissionsState(
          permissions =
              listOf(
                  android.Manifest.permission.ACCESS_FINE_LOCATION,
                  android.Manifest.permission.ACCESS_COARSE_LOCATION))
  var showLocationPermissionDialog by remember { mutableStateOf(false) }
  var centerMapOnUserPosition by remember { mutableStateOf(false) }

  // Avoids the app crashing when spamming the back button
  var wantToNavigateBack by remember { mutableStateOf(false) }
  LaunchedEffect(wantToNavigateBack) {
    if (wantToNavigateBack) {
      navigationActions.goBack()
      wantToNavigateBack = false
    }
  }

  // Display the map
  val mapView = runHikeMap(hike)

  var userLocationMarker: Marker? by remember { mutableStateOf(null) }

  // We need to keep a reference to the instance of location callback, this way we can unregister
  // it using the same reference, for example when the permission is revoked.
  val locationUpdatedCallback = remember {
    object : LocationCallback() {
      override fun onLocationResult(locationResult: LocationResult) {
        userLocationMarker = parseLocationUpdate(locationResult, userLocationMarker, mapView, hike)
        if (centerMapOnUserPosition &&
            userLocationMarker != null &&
            userLocationMarker?.position != null)
            MapUtils.centerMapOnLocation(mapView, userLocationMarker!!.position)
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

  DisposableEffect(Unit) {
    val hasLocationPermission = LocationUtils.hasLocationPermission(locationPermissionState)
    Log.d("RunHikeScreen", "Has location permission: $hasLocationPermission")
    // If the user has granted at least one of the two permissions, center the map
    // on the user's location
    if (hasLocationPermission) {
      MapUtils.centerMapOnLocation(context, mapView, userLocationMarker)
    }
    // If the user yet needs to grant the permission, show a custom educational
    // alert
    else {
      showLocationPermissionDialog = true
    }
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
        wantToNavigateBack = true
      },
      simpleMessage = !locationPermissionState.shouldShowRationale,
      locationPermissionState = locationPermissionState,
      context = context)

  Box(modifier = Modifier.fillMaxSize().testTag(Screen.RUN_HIKE)) {
    // Back Button at the top of the screen
    BackButton(
        navigationActions = navigationActions,
        modifier =
            Modifier.padding(top = 40.dp, start = 16.dp, end = 16.dp)
                .testTag(RunHikeScreen.TEST_TAG_BACK_BUTTON),
        onClick = { wantToNavigateBack = true })

    // Button to center the map on the user's location
    MapMyLocationButton(
        onClick = {
          centerMapOnUserPosition = true
          if (userLocationMarker != null && userLocationMarker?.position != null)
              MapUtils.centerMapOnLocation(mapView, userLocationMarker!!.position)
        },
        modifier =
            Modifier.align(Alignment.BottomStart)
                .padding(bottom = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT + 8.dp)
                .testTag(RunHikeScreen.TEST_TAG_CENTER_MAP_BUTTON))

    // Zoom buttons at the bottom right of the screen
    ZoomMapButton(
        onZoomIn = { mapView.controller.zoomIn() },
        onZoomOut = { mapView.controller.zoomOut() },
        modifier =
            Modifier.align(Alignment.BottomEnd)
                .padding(bottom = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT + 8.dp)
                .testTag(RunHikeScreen.TEST_TAG_ZOOM_BUTTONS))

    // Display the bottom sheet with the hike details
    RunHikeBottomSheet(hike = hike, onStopTheRun = { wantToNavigateBack = true })
  }
}

/**
 * Parses a location result and updates the user's position on the map.
 *
 * @param locationResult The location result to parse
 * @param userLocationMarker The marker representing the previous user's location on the map
 * @param mapView The map view where the user's location is displayed
 * @param hike The hike where the user is running
 */
private fun parseLocationUpdate(
    locationResult: LocationResult,
    userLocationMarker: Marker?,
    mapView: MapView,
    hike: DetailedHike
): Marker? {
  return if (locationResult.lastLocation == null) {
    MapUtils.clearUserPosition(userLocationMarker, mapView, invalidate = true)
    null
  } else {
    val routeProjectionResponse =
        LocationUtils.projectLocationOnHike(
            locationResult.lastLocation!!.let { LatLong(it.latitude, it.longitude) }, hike)
    if (routeProjectionResponse != null) {
      if (routeProjectionResponse.distanceFromRoute > RunHikeScreen.MAX_DISTANCE_TO_CONSIDER_HIKE) {
        MapUtils.updateUserPosition(
            userLocationMarker,
            mapView,
            locationResult.lastLocation!!.let { location ->
              Location("").apply {
                latitude = location.latitude
                longitude = location.longitude
              }
            })
      } else {
        MapUtils.updateUserPosition(
            userLocationMarker,
            mapView,
            routeProjectionResponse.projectedLocation.let { location ->
              Location("").apply {
                latitude = location.lat
                longitude = location.lon
              }
            })
      }
    } else {
      null
    }
  }
}

@Composable
private fun runHikeMap(hike: DetailedHike): MapView {
  val context = LocalContext.current
  val routeZoomLevel = MapUtils.calculateBestZoomLevel(hike.bounds).toDouble()
  val hikeCenter = MapUtils.getGeographicalCenter(hike.bounds)

  // Avoid re-creating the MapView on every recomposition
  val mapView = remember {
    MapView(context).apply {
      controller.setZoom(routeZoomLevel)
      controller.setCenter(hikeCenter)
      // Limit the zoom to avoid the user zooming out or out too much
      minZoomLevel = routeZoomLevel
      // Avoid repeating the map when the user reaches the edge or zooms out
      // We keep the horizontal repetition enabled to allow the user to scroll the map
      // horizontally without limits (from Asia to America, for example)
      isHorizontalMapRepetitionEnabled = true
      isVerticalMapRepetitionEnabled = false
      // Disable built-in zoom controls since we have our own
      zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
      // Enable touch-controls such as pinch to zoom
      setMultiTouchControls(true)
    }
  }

  MapUtils.showHikeOnMap(
      mapView = mapView, waypoints = hike.waypoints, color = hike.color, onLineClick = {})

  // Map
  AndroidView(
      factory = { mapView },
      modifier =
          Modifier.fillMaxWidth()
              .padding(bottom = 300.dp) // Reserve space for the scaffold at the bottom
              .testTag(RunHikeScreen.TEST_TAG_MAP))

  return mapView
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
private fun RunHikeBottomSheet(
    hike: DetailedHike,
    onStopTheRun: () -> Unit,
) {
  val scaffoldState = rememberBottomSheetScaffoldState()

  BottomSheetScaffold(
      scaffoldState = scaffoldState,
      sheetContainerColor = MaterialTheme.colorScheme.surface,
      sheetPeekHeight = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT,
      modifier = Modifier.testTag(RunHikeScreen.TEST_TAG_BOTTOM_SHEET),
      sheetContent = {
        Column(
            modifier = Modifier.padding(16.dp).weight(1f),
        ) {
          Text(
              text = hike.name ?: stringResource(R.string.map_screen_hike_title_default),
              style = MaterialTheme.typography.titleLarge,
              textAlign = TextAlign.Left,
              modifier = Modifier.testTag(RunHikeScreen.TEST_TAG_HIKE_NAME))

          // Elevation graph and the progress details below the graph
          Column {
            val hikeColor = Color(hike.color)
            ElevationGraph(
                elevations = hike.elevation,
                styleProperties =
                    ElevationGraphStyleProperties(
                        strokeColor = hikeColor, fillColor = hikeColor.copy(0.1f)),
                modifier =
                    Modifier.fillMaxWidth()
                        .height(60.dp)
                        .padding(4.dp)
                        .testTag(RunHikeScreen.TEST_TAG_ELEVATION_GRAPH))

            // Progress details below the graph
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                  Text(
                      text = stringResource(R.string.run_hike_screen_zero_distance_progress_value),
                      style = MaterialTheme.typography.bodyLarge,
                      fontWeight = FontWeight.Bold,
                      textAlign = TextAlign.Left,
                  )
                  Text(
                      // Displays the progress percentage below the graph
                      // TODO hardcoded as 23% for now
                      text =
                          stringResource(R.string.run_hike_screen_progress_percentage_format, 23),
                      style = MaterialTheme.typography.bodyLarge,
                      color = hikeColor,
                      fontWeight = FontWeight.Bold,
                      textAlign = TextAlign.Right,
                      modifier = Modifier.testTag(RunHikeScreen.TEST_TAG_PROGRESS_TEXT),
                  )
                  Text(
                      text =
                          stringResource(
                              R.string.run_hike_screen_distance_progress_value_format,
                              hike.distance),
                      style = MaterialTheme.typography.bodyLarge,
                      fontWeight = FontWeight.Bold,
                      textAlign = TextAlign.Right,
                      modifier = Modifier.testTag(RunHikeScreen.TEST_TAG_TOTAL_DISTANCE_TEXT),
                  )
                }

            val hours = (hike.estimatedTime / 60).toInt()
            val minutes = (hike.estimatedTime % 60).roundToInt()

            DetailRow(
                label = stringResource(R.string.run_hike_screen_label_current_elevation),
                // TODO hardcoded to 50m for now
                value = stringResource(R.string.run_hike_screen_value_format_current_elevation, 50))
            DetailRow(
                label = stringResource(R.string.run_hike_screen_label_elevation_gain),
                value =
                    stringResource(
                        R.string.run_hike_screen_value_format_elevation_gain,
                        hike.elevationGain.roundToInt()))
            DetailRow(
                label = stringResource(R.string.run_hike_screen_label_estimated_time),
                value =
                    if (hours < 1)
                        stringResource(
                            R.string.run_hike_screen_value_format_estimated_time_minutes, minutes)
                    else
                        stringResource(
                            R.string.run_hike_screen_value_format_estimated_time_hours_and_minutes,
                            hours,
                            minutes))
            DetailRow(
                label = stringResource(R.string.run_hike_screen_label_difficulty),
                value = stringResource(hike.difficulty.nameResourceId),
                valueColor = colorResource(hike.difficulty.colorResourceId))

            BigButton(
                buttonType = ButtonType.PRIMARY,
                label = stringResource(R.string.run_hike_screen_stop_run_button_label),
                onClick = onStopTheRun,
                modifier =
                    Modifier.padding(top = 16.dp).testTag(RunHikeScreen.TEST_TAG_STOP_HIKE_BUTTON),
                fillColor = colorResource(R.color.red),
            )
          }
        }
      }) {}
}
