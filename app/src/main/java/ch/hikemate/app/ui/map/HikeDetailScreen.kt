package ch.hikemate.app.ui.map

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.hikemate.app.R
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.model.profile.HikingLevel
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.model.route.DetailedHikeRoute
import ch.hikemate.app.model.route.ListOfHikeRoutesViewModel
import ch.hikemate.app.model.route.saved.SavedHikesViewModel
import ch.hikemate.app.ui.components.AsyncStateHandler
import ch.hikemate.app.ui.components.BackButton
import ch.hikemate.app.ui.components.ElevationGraph
import ch.hikemate.app.ui.components.ElevationGraphStyleProperties
import ch.hikemate.app.ui.map.HikeDetailScreen.MAP_MAX_ZOOM
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_ADD_DATE_BUTTON
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_BOOKMARK_ICON
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_DETAIL_ROW_TAG
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_DETAIL_ROW_VALUE
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_ELEVATION_GRAPH
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_HIKE_NAME
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_MAP
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_PLANNED_DATE_TEXT_BOX
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.utils.MapUtils
import ch.hikemate.app.utils.from
import ch.hikemate.app.utils.toFormattedString
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

object HikeDetailScreen {
  const val MAP_MAX_ZOOM = 18.0
  const val MAP_MAX_LONGITUDE = 180.0
  const val MAP_MIN_LONGITUDE = -180.0
  const val MAP_BOUNDS_MARGIN: Int = 100

  const val TEST_TAG_MAP = "map"
  const val TEST_TAG_HIKE_NAME = "hikeName"
  const val TEST_TAG_BOOKMARK_ICON = "bookmarkIcon"
  const val TEST_TAG_ELEVATION_GRAPH = "elevationGraph"
  const val TEST_TAG_DETAIL_ROW_TAG = "detailRowTag"
  const val TEST_TAG_DETAIL_ROW_VALUE = "detailRowValue"
  const val TEST_TAG_ADD_DATE_BUTTON = "addDateButton"
  const val TEST_TAG_PLANNED_DATE_TEXT_BOX = "plannedDateTextBox"
}

@Composable
fun HikeDetailScreen(
    listOfHikeRoutesViewModel: ListOfHikeRoutesViewModel,
    savedHikesViewModel: SavedHikesViewModel,
    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
    authViewModel: AuthViewModel,
    navigationActions: NavigationActions
) {

  val context = LocalContext.current

  LaunchedEffect(listOfHikeRoutesViewModel.selectedHikeRoute.collectAsState().value) {
    if (listOfHikeRoutesViewModel.selectedHikeRoute.value == null) {
      navigationActions.goBack()
    }
  }

  if (listOfHikeRoutesViewModel.selectedHikeRoute.collectAsState().value == null) {
    Log.e("HikeDetailScreen", "No selected hike route")
    return
  }

  val route = listOfHikeRoutesViewModel.selectedHikeRoute.collectAsState().value!!
  val elevationData = remember { mutableListOf<Double>() }
  val detailedRoute = DetailedHikeRoute.create(route)
  val routeZoomLevel = MapUtils.calculateBestZoomLevel(route.bounds).toDouble()

  // Avoid re-creating the MapView on every recomposition
  val mapView = remember {
    MapView(context).apply {
      // Set map's initial state
      controller.setZoom(routeZoomLevel)
      controller.setCenter(MapUtils.getGeographicalCenter(route.bounds))
      // Limit the zoom to avoid the user zooming out or out too much
      minZoomLevel = routeZoomLevel
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

  // Show the selected hike on the map
  // OnLineClick does nothing, the line should not be clickable
  val hikeLineColor = route.getColor()
  Log.d("HikeDetailScreen", "Drawing hike on map: ${route.bounds}")
  MapUtils.showHikeOnMap(mapView = mapView, hike = route, color = hikeLineColor, onLineClick = {})

  LaunchedEffect(Unit) {
    if (authViewModel.currentUser.value == null) {
      Log.e("HikeDetailScreen", "User is not signed in")
      return@LaunchedEffect
    }
    profileViewModel.getProfileById(authViewModel.currentUser.value!!.uid)
  }

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
      Button(
          onClick = { navigationActions.navigateTo(Screen.RUN_HIKE) },
          modifier = Modifier.padding(top = 16.dp)) {
            Text(text = "Test")
          }

      // Hike Details bottom sheet
      HikeDetails(detailedRoute, savedHikesViewModel, elevationData, profile.hikingLevel)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HikeDetails(
    detailedRoute: DetailedHikeRoute,
    savedHikesViewModel: SavedHikesViewModel,
    elevationData: List<Double>,
    userHikingLevel: HikingLevel,
) {
  val hikeDetailState = savedHikesViewModel.hikeDetailState.collectAsState(null).value

  // Handle save/unsave actions
  val isSaved = hikeDetailState?.isSaved ?: false
  val toggleSaveState = { savedHikesViewModel.toggleSaveState() }

  // Handle planned date updates
  val plannedDate = hikeDetailState?.plannedDate
  val updatePlannedDate = { timestamp: Timestamp? ->
    savedHikesViewModel.updatePlannedDate(timestamp)
  }

  val hikeColor = Color(detailedRoute.route.getColor())

  val scaffoldState = rememberBottomSheetScaffoldState()

  val isSuitable = detailedRoute.difficulty.ordinal <= userHikingLevel.ordinal

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
                  text =
                      detailedRoute.route.name
                          ?: stringResource(R.string.map_screen_hike_title_default),
                  style = MaterialTheme.typography.titleLarge,
                  textAlign = TextAlign.Left,
                  modifier = Modifier.testTag(TEST_TAG_HIKE_NAME))
              AppropriatenessMessage(isSuitable)
            }

            Image(
                painter = painterResource(hikeDetailState?.bookmark ?: R.drawable.bookmark_no_fill),
                contentDescription =
                    if (hikeDetailState?.isSaved == true)
                        stringResource(R.string.hike_detail_screen_bookmark_hint_on_isSaved_true)
                    else stringResource(R.string.hike_detail_screen_bookmark_hint_on_isSaved_false),
                modifier =
                    Modifier.size(60.dp, 80.dp).testTag(TEST_TAG_BOOKMARK_ICON).clickable {
                      toggleSaveState()
                    },
                contentScale = ContentScale.FillBounds,
            )
          }

          ElevationGraph(
              elevations = elevationData,
              modifier =
                  Modifier.fillMaxWidth()
                      .height(60.dp)
                      .padding(4.dp)
                      .testTag(TEST_TAG_ELEVATION_GRAPH),
              styleProperties =
                  ElevationGraphStyleProperties(
                      strokeColor = hikeColor, fillColor = hikeColor.copy(0.1f)))

          val distanceString =
              String.format(Locale.getDefault(), "%.2f", detailedRoute.totalDistance)
          val elevationGainString = detailedRoute.elevationGain.roundToInt().toString()
          val hourString =
              String.format(Locale.getDefault(), "%02d", (detailedRoute.estimatedTime / 60).toInt())
          val minuteString =
              String.format(
                  Locale.getDefault(), "%02d", (detailedRoute.estimatedTime % 60).roundToInt())

          DetailRow(
              label = stringResource(R.string.hike_detail_screen_label_distance),
              value = "${distanceString}km")
          DetailRow(
              label = stringResource(R.string.hike_detail_screen_label_elevation_gain),
              value = "${elevationGainString}m")
          DetailRow(
              label = stringResource(R.string.hike_detail_screen_label_estimated_time),
              value =
                  if (detailedRoute.estimatedTime / 60 < 1) "${minuteString}min"
                  else "${hourString}h${minuteString}")
          DetailRow(
              label = stringResource(R.string.hike_detail_screen_label_difficulty),
              value = stringResource(detailedRoute.difficulty.nameResourceId),
              valueColor =
                  Color(
                      ContextCompat.getColor(
                          LocalContext.current, detailedRoute.difficulty.colorResourceId)),
          )
          DateDetailRow(isSaved, plannedDate, updatePlannedDate)
        }
      },
      sheetPeekHeight = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT) {}
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun DateDetailRow(
    isSaved: Boolean,
    plannedDate: Timestamp?,
    updatePlannedDate: (Timestamp?) -> Unit
) {
  // Row to display and change the date

  val context = LocalContext.current

  // Needed for the pop-up that allows the user to show the date
  fun showDatePickerDialog() {
    val calendar = Calendar.getInstance()
    val year = calendar[Calendar.YEAR]
    val month = calendar[Calendar.MONTH]
    val day = calendar[Calendar.DAY_OF_MONTH]

    DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
              val timestamp = Timestamp.from(selectedYear, selectedMonth + 1, selectedDay)
              updatePlannedDate(timestamp)
            },
            year,
            month,
            day)
        .show()
  }
  if (isSaved) {
    if (plannedDate == null) {
      DetailRow(
          label = stringResource(R.string.hike_detail_screen_label_status),
          value = stringResource(R.string.hike_detail_screen_value_saved),
          valueColor = Color(0xFF3B9DE8))
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
            text = stringResource(R.string.hike_detail_screen_label_planned_for),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag(TEST_TAG_DETAIL_ROW_TAG))

        Button(
            modifier = Modifier.width(90.dp).height(25.dp).testTag(TEST_TAG_ADD_DATE_BUTTON),
            contentPadding = PaddingValues(0.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4285F4), // Blue color to match the image
                    contentColor = Color.White),
            onClick = { showDatePickerDialog() },
        ) {
          Text(
              text = stringResource(R.string.hike_detail_screen_add_a_date_button_text),
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
          )
        }
      }
    } else { // A Date is set
      DetailRow(
          label = stringResource(R.string.hike_detail_screen_label_status),
          value = stringResource(R.string.hike_detail_screen_value_planned),
          valueColor = Color(0xFF3B9DE8))
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
            text = stringResource(R.string.hike_detail_screen_label_planned_for),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag(TEST_TAG_DETAIL_ROW_TAG))
        Box(
            modifier =
                Modifier.border(BorderStroke(1.dp, Color.Black), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)) {
              Text(
                  text = plannedDate.toFormattedString(),
                  // saved Date
                  style = MaterialTheme.typography.bodySmall,
                  modifier =
                      Modifier.clickable { showDatePickerDialog() }
                          .testTag(TEST_TAG_PLANNED_DATE_TEXT_BOX))
            }
      }
    }
  } else {
    DetailRow(
        label = stringResource(R.string.hike_detail_screen_label_status),
        value = stringResource(R.string.hike_detail_screen_value_not_saved),
        valueColor = MaterialTheme.colorScheme.onSurface)
  }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
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

@Composable
fun AppropriatenessMessage(isSuitable: Boolean) {
  // The text, icon and color of the card's message are chosen based on whether the hike is suitable
  // or not
  val suitableLabelColor = if (isSuitable) Color(0xFF4CAF50) else Color(0xFFFFC107)

  val suitableLabelText =
      if (isSuitable) LocalContext.current.getString(R.string.map_screen_suitable_hike_label)
      else LocalContext.current.getString(R.string.map_screen_challenging_hike_label)

  val suitableLabelIcon =
      painterResource(if (isSuitable) R.drawable.check_circle else R.drawable.warning)

  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
        painter = suitableLabelIcon,
        // The icon is only decorative, the following message is enough for accessibility
        contentDescription = null,
        tint = suitableLabelColor,
        modifier = Modifier.size(16.dp))
    Spacer(modifier = Modifier.width(4.dp))
    Text(
        text = suitableLabelText,
        style = MaterialTheme.typography.bodySmall,
        color = suitableLabelColor)
  }
}
