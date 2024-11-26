package ch.hikemate.app.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import ch.hikemate.app.R
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.model.route.ListOfHikeRoutesViewModel
import ch.hikemate.app.model.route.saved.SavedHikesViewModel
import ch.hikemate.app.ui.components.AsyncStateHandler
import ch.hikemate.app.ui.components.BackButton
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_MAP
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.utils.MapUtils
import kotlin.math.max
import kotlin.math.min
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun RunHikeScreen(
    listOfHikeRoutesViewModel: ListOfHikeRoutesViewModel,
    savedHikesViewModel: SavedHikesViewModel,
    profileViewModel: ProfileViewModel,
    navigationActions: NavigationActions
) {

  val context = LocalContext.current

  val route = listOfHikeRoutesViewModel.selectedHikeRoute.collectAsState().value!!

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

  val marker =
      Marker(mapView).apply {
        position = GeoPoint(46.57876571785863, 6.551381450987971)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        this.title = title
        icon?.let { this.icon = ContextCompat.getDrawable(context, R.drawable.my_location) }

        setOnMarkerClickListener { marker, mapView ->
          marker.showInfoWindow()
          true
        }
      }

  mapView.overlays.add(marker)
  mapView.invalidate()

  val hikeLineColor = route.getColor()
  MapUtils.showHikeOnMap(mapView = mapView, hike = route, color = hikeLineColor, onLineClick = {})

  val errorMessageIdState = profileViewModel.errorMessageId.collectAsState()
  val profileState = profileViewModel.profile.collectAsState()

  AsyncStateHandler(
      errorMessageIdState = errorMessageIdState,
      actionContentDescriptionStringId = R.string.go_back,
      actionOnErrorAction = { navigationActions.navigateTo(Route.MAP) },
      valueState = profileState,
  ) { profile ->
    Box(modifier = Modifier.fillMaxSize().testTag(Screen.HIKE_DETAILS)) {
      // Map
      AndroidView(
          factory = { mapView },
          modifier =
              Modifier.fillMaxWidth()
                  .padding(bottom = 300.dp) // Reserve space for the scaffold at the bottom
                  .testTag(TEST_TAG_MAP))
      // Back Button at the top of the screen
      BackButton(
          navigationActions = navigationActions,
          modifier = Modifier.padding(top = 40.dp, start = 16.dp, end = 16.dp),
          onClick = { listOfHikeRoutesViewModel.clearSelectedRoute() })
      // Zoom buttons at the bottom right of the screen
      ZoomMapButton(
          onZoomIn = { mapView.controller.zoomIn() },
          onZoomOut = { mapView.controller.zoomOut() },
          modifier =
              Modifier.align(Alignment.BottomEnd)
                  .padding(bottom = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT + 8.dp))

      RunHikeBottomSheet()
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunHikeBottomSheet() {
  val scaffoldState = rememberBottomSheetScaffoldState()

  BottomSheetScaffold(
      scaffoldState = scaffoldState,
      sheetContainerColor = MaterialTheme.colorScheme.surface,
      sheetPeekHeight = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT,
      sheetContent = { Text("Empty Bottom Sheet") }) {}
}
