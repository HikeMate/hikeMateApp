package ch.hikemate.app.ui.map

import android.app.DatePickerDialog
import android.util.Log
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
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.ListOfHikeRoutesViewModel
import ch.hikemate.app.ui.components.AppropriatenessMessage
import ch.hikemate.app.ui.components.BackButton
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Screen
import com.google.firebase.Timestamp
import java.util.Calendar
import kotlin.math.cos
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
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

@OptIn(ExperimentalMaterial3Api::class)
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

  val mapView = remember {
    MapView(context).apply {
      // Set map's initial state
      Log.d(
          "HikeDetailScreen", "Setting map's initial state ${calculateBestZoomLevel(route.bounds)}")
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

  showHikeOnMap(mapView, route, getRandomColor(), navigationActions)

  Box(modifier = Modifier.fillMaxSize().testTag(Screen.HIKE_DETAILS)) {
    AndroidView(
        factory = { mapView },
        modifier =
            Modifier.fillMaxWidth()
                .padding(bottom = 300.dp) // Reserve space for the scaffold at the bottom
                .testTag(TEST_TAG_MAP))
    BackButton(
        navigationActions = navigationActions, backgroundColor = MaterialTheme.colorScheme.surface)
    ZoomMapButton(
        onZoomIn = { mapView.controller.zoomIn() },
        onZoomOut = { mapView.controller.zoomOut() },
        modifier =
            Modifier.align(Alignment.BottomEnd)
                .padding(bottom = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT + 8.dp))
    HikeDetails(route, true, null)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HikeDetails(route: HikeRoute, isSaved: Boolean, date: Timestamp?) {
  val scaffoldState = rememberBottomSheetScaffoldState()
  val context = LocalContext.current

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
                  text = route.name ?: "Unnammed Hike",
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
  // The color of the card's message is chosen based on whether the hike is suitable or not
  val suitableLabelColor = if (isSuitable) Color(0xFF4CAF50) else Color(0xFFFFC107)

  // The text and icon of the card's message are chosen based on whether the hike is suitable or not
  val suitableLabelText =
      if (isSuitable) LocalContext.current.getString(R.string.map_screen_suitable_hike_label)
      else LocalContext.current.getString(R.string.map_screen_challenging_hike_label)

  // The icon of the card's message is chosen based on whether the hike is suitable or not
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

fun getGeographicalCenter(bounds: Bounds): GeoPoint {
  Log.d(
      "Bounds",
      "minLat: ${bounds.minLat}, maxLat: ${bounds.maxLat}, minLon: ${bounds.minLon}, maxLon: ${bounds.maxLon}")
  val minLong = bounds.minLon
  val maxLong = bounds.maxLon
  val minLat = bounds.minLat
  val maxLat = bounds.maxLat

  val centerLat = (minLat + maxLat) / 2
  var centerLong = (minLong + maxLong) / 2

  // Adjust if the longitude crosses the Date Line (i.e., difference > 180 degrees)
  if (maxLong - minLong > 180) {
    centerLong = (minLong + maxLong + 360) / 2
  } else if (minLong - maxLong > 180) {
    centerLong = (minLong + maxLong - 360) / 2
  }

  // Normalize longitude to be in the range -180 to 180
  if (centerLong > 180) {
    centerLong -= 360
  } else if (centerLong < -180) {
    centerLong += 360
  }

  Log.d("Bounds", "centerLat: $centerLat, centerLong: $centerLong")

  return GeoPoint(centerLat, centerLong)
}

fun calculateBestZoomLevel(bounds: Bounds): Int {
  val minLong = bounds.minLon
  val maxLong = bounds.maxLon
  val minLat = bounds.minLat
  val maxLat = bounds.maxLat

  val centerLat = (minLat + maxLat) / 2

  val latDiff = maxLat - minLat
  val longDiff = maxLong - minLong

  // Adjust longitude difference based on the latitude compression factor
  val adjustedLongDiff = longDiff * cos(Math.toRadians(centerLat))

  // Calculate the maximum degree difference between lat and adjusted long
  val maxDegreeDiff = kotlin.math.max(latDiff, adjustedLongDiff)

  val zoomLevels =
      listOf(
          360.0,
          180.0,
          90.0,
          45.0,
          22.5,
          11.25,
          5.625,
          2.813,
          1.406,
          0.703,
          0.352,
          0.176,
          0.088,
          0.044,
          0.022,
          0.011,
          0.005,
          0.003,
          0.001,
          0.0005,
          0.00025)

  val bestZoomLevel =
      zoomLevels.indexOfFirst { it <= maxDegreeDiff }.takeIf { it != -1 } ?: (zoomLevels.size - 1)

  return bestZoomLevel
}
