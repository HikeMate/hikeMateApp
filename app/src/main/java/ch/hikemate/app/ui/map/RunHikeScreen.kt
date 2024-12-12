package ch.hikemate.app.ui.map

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.hikemate.app.R
import ch.hikemate.app.model.facilities.FacilitiesViewModel
import ch.hikemate.app.model.route.DetailedHike
import ch.hikemate.app.model.route.HikesViewModel
import ch.hikemate.app.ui.components.BackButton
import ch.hikemate.app.ui.components.BigButton
import ch.hikemate.app.ui.components.ButtonType
import ch.hikemate.app.ui.components.CenteredErrorAction
import ch.hikemate.app.ui.components.DetailRow
import ch.hikemate.app.ui.components.ElevationGraph
import ch.hikemate.app.ui.components.ElevationGraphStyleProperties
import ch.hikemate.app.ui.components.WithDetailedHike
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Screen
import kotlin.math.roundToInt

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
    hikesViewModel: HikesViewModel,
    navigationActions: NavigationActions,
    facilitiesViewModel: FacilitiesViewModel
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
      withDetailedHike = { RunHikeContent(it, navigationActions, facilitiesViewModel) },
      whenError = {
        val loadingErrorMessageId by hikesViewModel.loadingErrorMessageId.collectAsState()

        CenteredErrorAction(
            errorMessageId = loadingErrorMessageId ?: R.string.loading_hike_error,
            actionIcon = Icons.AutoMirrored.Filled.ArrowBack,
            actionContentDescriptionStringId = R.string.go_back,
            onAction = { navigationActions.goBack() })
      })
}

@Composable
private fun RunHikeContent(
    hike: DetailedHike,
    navigationActions: NavigationActions,
    facilitiesViewModel: FacilitiesViewModel
) {

  LaunchedEffect(Unit) { facilitiesViewModel.fetchFacilitiesForHike(hike) }

  // Avoids the app crashing when spamming the back button
  var wantToNavigateBack by remember { mutableStateOf(false) }
  LaunchedEffect(wantToNavigateBack) {
    if (wantToNavigateBack) {
      navigationActions.goBack()
      wantToNavigateBack = false
    }
  }

  Box(modifier = Modifier.fillMaxSize().testTag(Screen.RUN_HIKE)) {
    // Display the map
    // We use the exact same map as the HikeDetailsMap
    val mapView = hikeDetailsMap(hike, facilitiesViewModel)

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

    // Display the bottom sheet with the hike details
    RunHikeBottomSheet(hike = hike, onStopTheRun = { wantToNavigateBack = true })
  }
}

@OptIn(ExperimentalMaterial3Api::class)
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
