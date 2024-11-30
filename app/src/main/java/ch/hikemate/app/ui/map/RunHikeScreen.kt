package ch.hikemate.app.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ch.hikemate.app.R
import ch.hikemate.app.model.elevation.ElevationServiceRepository
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.model.route.DetailedHikeRoute
import ch.hikemate.app.model.route.ListOfHikeRoutesViewModel
import ch.hikemate.app.ui.components.AsyncStateHandler
import ch.hikemate.app.ui.components.BackButton
import ch.hikemate.app.ui.map.RunHikeScreen.TEST_TAG_RUN_HIKE_SCREEN_BACK_BUTTON
import ch.hikemate.app.ui.map.RunHikeScreen.TEST_TAG_RUN_HIKE_SCREEN_BOTTOM_SHEET
import ch.hikemate.app.ui.map.RunHikeScreen.TEST_TAG_RUN_HIKE_SCREEN_MAP
import ch.hikemate.app.ui.map.RunHikeScreen.TEST_TAG_RUN_HIKE_SCREEN_ZOOM_BUTTONS
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.utils.MapUtils
import kotlin.math.max
import kotlin.math.min
import okhttp3.OkHttpClient
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

object RunHikeScreen {
  const val TEST_TAG_RUN_HIKE_SCREEN_MAP = "runHikeScreenMap"
  const val TEST_TAG_RUN_HIKE_SCREEN_BACK_BUTTON = "runHikeScreenBackButton"
  const val TEST_TAG_RUN_HIKE_SCREEN_ZOOM_BUTTONS = "runHikeScreenZoomInButton"
  const val TEST_TAG_RUN_HIKE_SCREEN_BOTTOM_SHEET = "runHikeScreenBottomSheet"
}

@Composable
fun RunHikeScreen(
    listOfHikeRoutesViewModel: ListOfHikeRoutesViewModel,
    profileViewModel: ProfileViewModel,
    navigationActions: NavigationActions,
) {

  val context = LocalContext.current

  val selectedRoute by listOfHikeRoutesViewModel.selectedHikeRoute.collectAsState()

  LaunchedEffect(selectedRoute) {
    if (selectedRoute == null) {
      navigationActions.goBack()
    }
  }
  val route = selectedRoute!!

  // This will need to be changed when the "run" feature of this screen is implemented
  val routeZoomLevel = MapUtils.calculateBestZoomLevel(route.bounds).toDouble()

  // Avoid re-creating the MapView on every recomposition
  val mapView = remember {
    MapView(context).apply {
      controller.setZoom(routeZoomLevel)
      controller.setCenter(MapUtils.getGeographicalCenter(route.bounds))
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

  // When the map is ready, it will have computed its bounding box
  mapView.addOnFirstLayoutListener { _, _, _, _, _ ->
    // Limit the vertical scrollable area to avoid the user scrolling too far from the hike
    mapView.setScrollableAreaLimitLatitude(
        min(MapScreen.MAP_MAX_LATITUDE, mapView.boundingBox.latNorth),
        max(MapScreen.MAP_MIN_LATITUDE, mapView.boundingBox.latSouth),
        HikeDetailScreen.MAP_BOUNDS_MARGIN)
    if (route.bounds.maxLon < HikeDetailScreen.MAP_MAX_LONGITUDE ||
        route.bounds.minLon > HikeDetailScreen.MAP_MIN_LONGITUDE) {
      mapView.setScrollableAreaLimitLongitude(
          max(HikeDetailScreen.MAP_MIN_LONGITUDE, mapView.boundingBox.lonWest),
          min(HikeDetailScreen.MAP_MAX_LONGITUDE, mapView.boundingBox.lonEast),
          HikeDetailScreen.MAP_BOUNDS_MARGIN)
    }
  }

  val hikeLineColor = route.getColor()
  MapUtils.showHikeOnMap(mapView = mapView, waypoints = route.ways, color = hikeLineColor, onLineClick = {})

  val errorMessageIdState = profileViewModel.errorMessageId.collectAsState()
  val profileState = profileViewModel.profile.collectAsState()

  AsyncStateHandler(
      errorMessageIdState = errorMessageIdState,
      actionContentDescriptionStringId = R.string.go_back,
      actionOnErrorAction = { navigationActions.goBack() },
      valueState = profileState,
  ) { _ ->
    Box(modifier = Modifier.fillMaxSize().testTag(Screen.HIKE_DETAILS)) {
      // Map
      AndroidView(
          factory = { mapView },
          modifier =
              Modifier.fillMaxWidth()
                  .padding(bottom = 300.dp) // Reserve space for the scaffold at the bottom
                  .testTag(TEST_TAG_RUN_HIKE_SCREEN_MAP))
      // Back Button at the top of the screen
      BackButton(
          navigationActions = navigationActions,
          modifier =
              Modifier.padding(top = 40.dp, start = 16.dp, end = 16.dp)
                  .testTag(TEST_TAG_RUN_HIKE_SCREEN_BACK_BUTTON),
          onClick = { navigationActions.goBack() })
      // Zoom buttons at the bottom right of the screen
      ZoomMapButton(
          onZoomIn = { mapView.controller.zoomIn() },
          onZoomOut = { mapView.controller.zoomOut() },
          modifier =
              Modifier.align(Alignment.BottomEnd)
                  .padding(bottom = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT + 8.dp)
                  .testTag(TEST_TAG_RUN_HIKE_SCREEN_ZOOM_BUTTONS))

      RunHikeBottomSheet(
          DetailedHikeRoute.create(route, ElevationServiceRepository(OkHttpClient())), {})
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunHikeBottomSheet(
    hikeRoute: DetailedHikeRoute,
    onStopTheRun: () -> Unit,
) {
  val scaffoldState = rememberBottomSheetScaffoldState()

  BottomSheetScaffold(
      scaffoldState = scaffoldState,
      sheetContainerColor = MaterialTheme.colorScheme.surface,
      sheetPeekHeight = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT,
      modifier = Modifier.testTag(TEST_TAG_RUN_HIKE_SCREEN_BOTTOM_SHEET),
      sheetContent = {}) {}
}
