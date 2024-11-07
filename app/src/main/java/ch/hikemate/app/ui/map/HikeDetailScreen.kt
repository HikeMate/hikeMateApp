package ch.hikemate.app.ui.map

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ch.hikemate.app.R
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.ListOfHikeRoutesViewModel
import ch.hikemate.app.ui.components.AppropriatenessMessage
import ch.hikemate.app.ui.components.BackButton
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.utils.calculateBestZoomLevel
import ch.hikemate.app.utils.getGeographicalCenter
import com.google.firebase.Timestamp
import java.util.Calendar
import org.osmdroid.config.Configuration
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

const val MAP_MIN_ZOOM = 3.0
const val MAP_MAX_ZOOM = 18.0

const val TEST_TAG_MAP = "map"
const val TEST_TAG_HIKE_NAME = "hikeName"
const val TEST_TAG_BOOKMARK_ICON = "bookmarkIcon"
const val TEST_TAG_ELEVATION_GRAPH = "elevationGraph"
const val TEST_TAG_DETAIL_ROW_TAG = "detailRowTag"
const val TEST_TAG_DETAIL_ROW_VALUE = "detailRowValue"
const val TEST_TAG_ADD_DATE_BUTTON = "addDateButton"
const val TEST_TAG_PLANNED_DATE_TEXT_BOX = "plannedDateTextBox"

@Composable
fun HikeDetailScreen(
    listOfHikeRoutesViewModel: ListOfHikeRoutesViewModel,
    navigationActions: NavigationActions
) {

  val route: HikeRoute =
      listOfHikeRoutesViewModel.selectedHikeRoute.collectAsState().value
          ?: throw IllegalStateException(
              "Selected HikeRoute is null. This screen should only be shown when a route is selected.")

  val context = LocalContext.current

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
      controller.setZoom(calculateBestZoomLevel(route.bounds))
      controller.setCenter(getGeographicalCenter(route.bounds))
      // Limit the zoom to avoid the user zooming out or out too much
      minZoomLevel = MAP_MIN_ZOOM
      maxZoomLevel = MAP_MAX_ZOOM
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

  // Show the selected hike on the map
  showHikeOnMap(mapView, route, getRandomColor(), navigationActions)

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
        navigationActions = navigationActions, backgroundColor = MaterialTheme.colorScheme.surface)
    // Zoom buttons at the bottom right of the screen
    ZoomMapButton(
        onZoomIn = { mapView.controller.zoomIn() },
        onZoomOut = { mapView.controller.zoomOut() },
        modifier =
            Modifier.align(Alignment.BottomEnd)
                .padding(bottom = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT + 8.dp))

    // Hike Details bottom sheet
    HikeDetails(route, true, null)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HikeDetails(route: HikeRoute, isSaved: Boolean, date: Timestamp?) {
  val scaffoldState = rememberBottomSheetScaffoldState()
  val context = LocalContext.current

  // Needed for the pop-up that allows the user to show the date
  fun showDatePickerDialog(onDateSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
              val formattedDate =
                  "%02d/%02d/%d".format(selectedDay, selectedMonth + 1, selectedYear)
              onDateSelected(formattedDate)
            },
            year,
            month,
            day)
        .show()
  }

  BottomSheetScaffold(
      scaffoldState = scaffoldState,
      sheetContainerColor = MaterialTheme.colorScheme.surface,
      sheetContent = {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Row {
            Column(
                modifier = Modifier.padding(16.dp).weight(1f),
            ) {
              Text(
                  text = route.name ?: "Unnamed Hike",
                  style = MaterialTheme.typography.titleLarge,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.testTag(TEST_TAG_HIKE_NAME))
              AppropriatenessMessageWrapper(isSuitable = true)
            }

            // Bookmark icon that is filled if the hike is Saved
            val bookmark =
                if (isSaved) R.drawable.bookmark_filled_blue else R.drawable.bookmark_no_fill
            Image(
                painter = painterResource(bookmark),
                contentDescription = null,
                modifier = Modifier.size(60.dp, 80.dp).testTag(TEST_TAG_BOOKMARK_ICON),
                contentScale = ContentScale.FillBounds,
            )
          }

          // Elevation box
          Box(
              modifier =
                  Modifier.fillMaxWidth()
                      .height(30.dp)
                      .background(Color.Gray)
                      .testTag(TEST_TAG_ELEVATION_GRAPH))

          DetailRow(label = "Distance", value = "5km")
          DetailRow(label = "Elevation Gain", value = "834m")
          DetailRow(label = "Estimated time", value = "3:30")
          DetailRow(label = "Difficulty", value = "Easy", valueColor = Color.Green)

          // Row to display and change the date
          if (isSaved) {
            if (date == null) {
              DetailRow(label = "Status", value = "Saved", valueColor = Color(0xFF3B9DE8))
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                    text = "Planned for",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier =
                        Modifier.clickable {
                              showDatePickerDialog {
                                // TODO ON SUCCESS
                              }
                            }
                            .testTag(TEST_TAG_DETAIL_ROW_TAG))
                Button(
                    modifier =
                        Modifier.width(90.dp).height(25.dp).testTag(TEST_TAG_ADD_DATE_BUTTON),
                    contentPadding = PaddingValues(0.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4285F4), // Blue color to match the image
                            contentColor = Color.White),
                    onClick = {
                      showDatePickerDialog {
                        // TODO ON SUCCESS
                      }
                    },
                ) {
                  Text(
                      text = "Add a date",
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Bold,
                  )
                }
              }
            } else { // A Date is set
              DetailRow(label = "Status", value = "Planned", valueColor = Color(0xFF3B9DE8))
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                    text = "Planned for",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier =
                        Modifier.clickable { showDatePickerDialog { TODO() } }
                            .testTag(TEST_TAG_DETAIL_ROW_TAG))
                Box(
                    modifier =
                        Modifier.border(
                                BorderStroke(1.dp, Color.Black), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)) {
                      Text(
                          text = "dd/mm/yyyy",
                          style = MaterialTheme.typography.bodySmall,
                          modifier =
                              Modifier.clickable { showDatePickerDialog {} }
                                  .testTag(TEST_TAG_PLANNED_DATE_TEXT_BOX))
                    }
              }
            }
          } else {
            DetailRow(label = "Status", value = "Not Saved", valueColor = Color.Black)
          }
        }
      },
      sheetPeekHeight = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT) {}
}

@Composable
fun AppropriatenessMessageWrapper(isSuitable: Boolean) {
  // The text, icon and color of the card's message are chosen based on whether the hike is suitable
  // or not
  val suitableLabelColor = if (isSuitable) Color(0xFF4CAF50) else Color(0xFFFFC107)

  val suitableLabelText =
      if (isSuitable) LocalContext.current.getString(R.string.map_screen_suitable_hike_label)
      else LocalContext.current.getString(R.string.map_screen_challenging_hike_label)

  val suitableLabelIcon =
      painterResource(if (isSuitable) R.drawable.check_circle else R.drawable.warning)

  AppropriatenessMessage(
      messageIcon = suitableLabelIcon,
      messageColor = suitableLabelColor,
      messageContent = suitableLabelText)
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = Color.Black) {
  Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag(TEST_TAG_DETAIL_ROW_TAG))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = valueColor,
            modifier = Modifier.testTag(TEST_TAG_DETAIL_ROW_VALUE))
      }
}
