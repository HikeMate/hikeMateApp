package ch.hikemate.app.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.model.elevation.ElevationServiceRepository
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.model.route.DetailedHikeRoute
import ch.hikemate.app.model.route.ListOfHikeRoutesViewModel
import ch.hikemate.app.ui.components.AsyncStateHandler
import ch.hikemate.app.ui.components.BackButton
import ch.hikemate.app.ui.components.BigButton
import ch.hikemate.app.ui.components.ButtonType
import ch.hikemate.app.ui.components.DetailRow
import ch.hikemate.app.ui.components.ElevationGraph
import ch.hikemate.app.ui.components.ElevationGraphStyleProperties
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.utils.MapUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import okhttp3.OkHttpClient
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

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
}

@Composable
fun RunHikeScreen(
    listOfHikeRoutesViewModel: ListOfHikeRoutesViewModel,
    profileViewModel: ProfileViewModel,
    navigationActions: NavigationActions,
    authViewModel: AuthViewModel
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

  val elevationData = remember { mutableStateListOf<Double>() }

  LaunchedEffect(Unit) {
    listOfHikeRoutesViewModel.getRoutesElevation(route, { elevationData.addAll(it) })
  }

  val hikeLineColor = route.getColor()
  MapUtils.showHikeOnMap(
      mapView = mapView, waypoints = route.ways, color = hikeLineColor, onLineClick = {})

  // avoids the app crashing when spamming the back button
  var wantToNavigateBack by remember { mutableStateOf(false) }
  LaunchedEffect(wantToNavigateBack) { if (wantToNavigateBack) navigationActions.goBack() }

  val errorMessageIdState = profileViewModel.errorMessageId.collectAsState()
  val profileState = profileViewModel.profile.collectAsState()

  AsyncStateHandler(
      errorMessageIdState = errorMessageIdState,
      actionContentDescriptionStringId = R.string.go_back,
      actionOnErrorAction = { authViewModel.signOut { navigationActions.navigateTo(Route.AUTH) } },
      valueState = profileState,
  ) { _ ->
    Box(modifier = Modifier.fillMaxSize().testTag(Screen.HIKE_DETAILS)) {
      // Map
      AndroidView(
          factory = { mapView },
          modifier =
              Modifier.fillMaxWidth()
                  .padding(bottom = 300.dp) // Reserve space for the scaffold at the bottom
                  .testTag(RunHikeScreen.TEST_TAG_MAP))
      // Back Button at the top of the screen
      BackButton(
          navigationActions = navigationActions,
          modifier =
              Modifier.padding(top = 40.dp, start = 16.dp, end = 16.dp)
                  .testTag(RunHikeScreen.TEST_TAG_BACK_BUTTON),
          onClick = { wantToNavigateBack = true })
      // Zoom buttons at the bottom right of the screen
      ZoomMapButton(
          onZoomIn = { mapView.controller.zoomIn() },
          onZoomOut = { mapView.controller.zoomOut() },
          modifier =
              Modifier.align(Alignment.BottomEnd)
                  .padding(bottom = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT + 8.dp)
                  .testTag(RunHikeScreen.TEST_TAG_ZOOM_BUTTONS))

      RunHikeBottomSheet(
          hikeRoute = DetailedHikeRoute.create(route, ElevationServiceRepository(OkHttpClient())),
          elevationData = elevationData,
          onStopTheRun = { wantToNavigateBack = true },
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunHikeBottomSheet(
    hikeRoute: DetailedHikeRoute,
    elevationData: List<Double>,
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
              text = hikeRoute.route.name ?: stringResource(R.string.map_screen_hike_title_default),
              style = MaterialTheme.typography.titleLarge,
              textAlign = TextAlign.Left,
              modifier = Modifier.testTag(RunHikeScreen.TEST_TAG_HIKE_NAME))

          // Elevation graph and the progress details below the graph
          Column {
            val hikeColor = Color(hikeRoute.route.getColor())
            ElevationGraph(
                elevations = elevationData,
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
                              hikeRoute.totalDistance),
                      style = MaterialTheme.typography.bodyLarge,
                      fontWeight = FontWeight.Bold,
                      textAlign = TextAlign.Right,
                      modifier = Modifier.testTag(RunHikeScreen.TEST_TAG_TOTAL_DISTANCE_TEXT),
                  )
                }

            val hours = (hikeRoute.estimatedTime / 60).toInt()
            val minutes = (hikeRoute.estimatedTime % 60).roundToInt()

            DetailRow(
                label = stringResource(R.string.run_hike_screen_label_current_elevation),
                // TODO hardcoded to 50m for now
                value = stringResource(R.string.run_hike_screen_value_format_current_elevation, 50))
            DetailRow(
                label = stringResource(R.string.run_hike_screen_label_elevation_gain),
                value =
                    stringResource(
                        R.string.run_hike_screen_value_format_elevation_gain,
                        hikeRoute.elevationGain.roundToInt()))
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
                value = stringResource(hikeRoute.difficulty.nameResourceId),
                valueColor = colorResource(hikeRoute.difficulty.colorResourceId))

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
