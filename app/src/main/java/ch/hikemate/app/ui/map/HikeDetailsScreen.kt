package ch.hikemate.app.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.hikemate.app.R
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.model.facilities.FacilitiesViewModel
import ch.hikemate.app.model.facilities.Facility
import ch.hikemate.app.model.profile.HikingLevel
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.model.route.DetailedHike
import ch.hikemate.app.model.route.HikesViewModel
import ch.hikemate.app.ui.components.AsyncStateHandler
import ch.hikemate.app.ui.components.BackButton
import ch.hikemate.app.ui.components.BigButton
import ch.hikemate.app.ui.components.ButtonType
import ch.hikemate.app.ui.components.CenteredErrorAction
import ch.hikemate.app.ui.components.DetailRow
import ch.hikemate.app.ui.components.ElevationGraph
import ch.hikemate.app.ui.components.ElevationGraphStyleProperties
import ch.hikemate.app.ui.components.WithDetailedHike
import ch.hikemate.app.ui.map.HikeDetailScreen.MAP_MAX_ZOOM
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_ADD_DATE_BUTTON
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_BOOKMARK_ICON
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_DATE_PICKER
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_DATE_PICKER_CANCEL_BUTTON
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_DATE_PICKER_CONFIRM_BUTTON
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_ELEVATION_GRAPH
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_HIKE_NAME
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_MAP
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_PLANNED_DATE_TEXT_BOX
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_RUN_HIKE_BUTTON
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.utils.MapUtils
import ch.hikemate.app.utils.humanReadableFormat
import com.google.firebase.Timestamp
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

object HikeDetailScreen {
  const val MAP_MAX_ZOOM = 18.0
  const val MAP_MAX_LONGITUDE = 180.0
  const val MAP_MIN_LONGITUDE = -180.0
  const val MAP_BOUNDS_MARGIN: Int = 100

  const val DEBOUNCE_DURATION = 300L

  val BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT = 400.dp
  val MAP_BOTTOM_PADDING_ADJUSTMENT = 20.dp

  const val LOG_TAG = "HikeDetailScreen"

  const val TEST_TAG_MAP = "HikeDetailScreenMap"
  const val TEST_TAG_HIKE_NAME = "HikeDetailScreenHikeName"
  const val TEST_TAG_BOOKMARK_ICON = "HikeDetailScreenBookmarkIcon"
  const val TEST_TAG_ELEVATION_GRAPH = "HikeDetailScreenElevationGraph"
  const val TEST_TAG_ADD_DATE_BUTTON = "HikeDetailScreenAddDateButton"
  const val TEST_TAG_PLANNED_DATE_TEXT_BOX = "HikeDetailScreenPlannedDateTextBox"
  const val TEST_TAG_DATE_PICKER = "HikeDetailDatePicker"
  const val TEST_TAG_DATE_PICKER_CANCEL_BUTTON = "HikeDetailDatePickerCancelButton"
  const val TEST_TAG_DATE_PICKER_CONFIRM_BUTTON = "HikeDetailDatePickerConfirmButton"
  const val TEST_TAG_RUN_HIKE_BUTTON = "HikeDetailRunHikeButton"
  const val TEST_TAG_APPROPRIATENESS_MESSAGE = "HikeDetailAppropriatenessMessage"
}

@Composable
fun HikeDetailScreen(
    hikesViewModel: HikesViewModel,
    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
    authViewModel: AuthViewModel,
    navigationActions: NavigationActions,
    facilitiesViewModel: FacilitiesViewModel
) {
  // Load the user's profile to get their hiking level
  LaunchedEffect(Unit) {
    if (authViewModel.currentUser.value == null) {
      Log.e("HikeDetailScreen", "User is not signed in")
      return@LaunchedEffect
    }
    profileViewModel.getProfileById(authViewModel.currentUser.value!!.uid)
    // Load or reload the saved hikes list to see if the currently selected one is saved
    hikesViewModel.refreshSavedHikesCache()
  }
  val selectedHike by hikesViewModel.selectedHike.collectAsState()

  // Gets initialized here so that the LaunchedEffect has access to it. The value is only actually
  // initialised and used in HikeDetailsContent
  val mapViewState = remember { mutableStateOf<MapView?>(null) }

  // If the selected hike is null, save the map's state and go back to the map screen
  LaunchedEffect(selectedHike) {
    if (selectedHike == null) {
      Log.e(HikeDetailScreen.LOG_TAG, "No selected hike, going back")
      if (mapViewState.value != null) {
        hikesViewModel.setMapState(
            center =
                GeoPoint(
                    mapViewState.value!!.mapCenter.latitude,
                    mapViewState.value!!.mapCenter.longitude),
            zoom = mapViewState.value!!.zoomLevelDouble)
      }
      navigationActions.goBack()
    }
  }

  if (selectedHike == null) {
    return
  }

  BackHandler { hikesViewModel.unselectHike() }

  val hike = selectedHike!!

  WithDetailedHike(
      hike = hike,
      hikesViewModel = hikesViewModel,
      withDetailedHike = { detailedHike ->
        val errorMessageIdState = profileViewModel.errorMessageId.collectAsState()
        val profileState = profileViewModel.profile.collectAsState()

        LaunchedEffect(Unit) { facilitiesViewModel.fetchFacilitiesForHike(detailedHike) }

        AsyncStateHandler(
            errorMessageIdState = errorMessageIdState,
            actionContentDescriptionStringId = R.string.go_back,
            // Whenever there's an error the user needs to re-authenticate
            // thus forcing him to sign out and navigate to the Auth screen
            actionOnErrorAction = {
              authViewModel.signOut { navigationActions.navigateTo(Route.AUTH) }
            },
            valueState = profileState,
        ) { profile ->
          Box(modifier = Modifier.fillMaxSize().testTag(Screen.HIKE_DETAILS)) {

            // Display the hike's actual information
            HikeDetailsContent(
                detailedHike,
                mapViewState,
                navigationActions,
                hikesViewModel,
                profile.hikingLevel,
                facilitiesViewModel)
          }
        }
      },
      whenError = {
        val loadingErrorMessageId by hikesViewModel.loadingErrorMessageId.collectAsState()

        CenteredErrorAction(
            errorMessageId = loadingErrorMessageId ?: R.string.loading_hike_error,
            actionIcon = Icons.AutoMirrored.Filled.ArrowBack,
            actionContentDescriptionStringId = R.string.go_back,
            onAction = { hikesViewModel.unselectHike() })
      },
      navigationActions = navigationActions,
      onBackAction = { hikesViewModel.unselectHike() })
}

@Composable
fun HikeDetailsContent(
    hike: DetailedHike,
    mapViewState: MutableState<MapView?>,
    navigationActions: NavigationActions,
    hikesViewModel: HikesViewModel,
    userHikingLevel: HikingLevel,
    facilitiesViewModel: FacilitiesViewModel
) {

  Box(modifier = Modifier.fillMaxSize().testTag(Screen.HIKE_DETAILS)) {
    // Display the map and the zoom buttons

    mapViewState.value = hikeDetailsMap(hike, facilitiesViewModel)

    // Display the back button on top of the map
    BackButton(
        navigationActions = navigationActions,
        modifier =
            Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .safeDrawingPadding(),
        onClick = { hikesViewModel.unselectHike() })

    // Zoom buttons at the bottom right of the screen
    ZoomMapButton(
        onZoomIn = { mapViewState.value?.controller?.zoomIn() },
        onZoomOut = { mapViewState.value?.controller?.zoomOut() },
        modifier =
            Modifier.align(Alignment.BottomEnd)
                .padding(bottom = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT + 8.dp))

    // Prevent the app from crashing when the "run hike" button is spammed
    var wantToRunHike by remember { mutableStateOf(false) }
    LaunchedEffect(wantToRunHike) {
      if (wantToRunHike) {
        navigationActions.navigateTo(Screen.RUN_HIKE)
        wantToRunHike = false
      }
    }
    // Display the details of the hike at the bottom of the screen
    HikesDetailsBottomScaffold(
        hike, hikesViewModel, userHikingLevel, onRunThisHike = { wantToRunHike = true })
  }
}

@Composable
fun hikeDetailsMap(hike: DetailedHike, facilitiesViewModel: FacilitiesViewModel): MapView {
  val context = LocalContext.current
  val hikeZoomLevel = MapUtils.calculateBestZoomLevel(hike.bounds).toDouble()
  val hikeCenter = MapUtils.getGeographicalCenter(hike.bounds)
  val facilities by facilitiesViewModel.facilities.collectAsState()

  // Add a state to force re-triggering effects when reentering screen
  var shouldLoadFacilities by remember { mutableStateOf(true) }

  // Avoid re-creating the MapView on every recomposition
  val mapView = remember {
    MapView(context).apply {
      // Set map's initial state
      controller.setZoom(hikeZoomLevel)
      controller.setCenter(hikeCenter)
      // Limit the zoom to avoid the user zooming out or out too much
      minZoomLevel = hikeZoomLevel
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

  // Create state values that we can actually observe in the LaunchedEffect
  // We keep our StateFlows for debouncing
  val boundingBoxState = remember { MutableStateFlow(mapView.boundingBox) }
  val zoomLevelState = remember { MutableStateFlow(mapView.zoomLevelDouble) }

  // This effect handles both initial facility display and subsequent updates
  // It triggers when facilities are loaded or when the map view changes
  shouldLoadFacilities =
      launchedEffectLoadingOfFacilities(
          facilities, shouldLoadFacilities, mapView, facilitiesViewModel, hike, context)

  // This solves the bug of the screen freezing by properly cleaning up resources
  DisposableEffect(Unit) {
    onDispose {
      mapView.onPause()
      mapView.onDetach()
      mapView.overlayManager.clear()
      mapView.tileProvider.clearTileCache()
      // Set flag to reload facilities when returning to screen
      shouldLoadFacilities = true
    }
  }

  // This LaunchedEffect handles map updates with debouncing to prevent too frequent refreshes
  LaunchedEffectFacilitiesDisplay(
      mapView, boundingBoxState, zoomLevelState, facilitiesViewModel, hike, context)

  // When the map is ready, it will have computed its bounding box
  LaunchedEffectMapviewListener(mapView, hike)

  // Show the selected hike on the map
  // OnLineClick does nothing, the line should not be clickable
  Log.d(HikeDetailScreen.LOG_TAG, "Drawing hike on map: ${hike.bounds}")
  MapUtils.showHikeOnMap(
      mapView = mapView, waypoints = hike.waypoints, color = hike.color, onLineClick = {})

  // Display the map as a composable
  AndroidView(
      factory = { mapView },
      modifier =
          Modifier.fillMaxWidth()
              // Reserve space for the scaffold at the bottom, -20.dp to avoid the map being to
              // small under the bottomSheet
              .padding(
                  bottom =
                      HikeDetailScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT -
                          HikeDetailScreen.MAP_BOTTOM_PADDING_ADJUSTMENT)
              .testTag(TEST_TAG_MAP))
  return mapView
}

@OptIn(FlowPreview::class)
@Composable
private fun LaunchedEffectFacilitiesDisplay(
    mapView: MapView,
    boundingBoxState: MutableStateFlow<BoundingBox>,
    zoomLevelState: MutableStateFlow<Double>,
    facilitiesViewModel: FacilitiesViewModel,
    hike: DetailedHike,
    context: Context
) {
  LaunchedEffect(Unit) {
    MapUtils.setMapViewListenerForStates(mapView, boundingBoxState, zoomLevelState)

    // Create our combined flow which is limited by a debounce
    val combinedFlow =
        combine(
            boundingBoxState.debounce(HikeDetailScreen.DEBOUNCE_DURATION),
            zoomLevelState.debounce(HikeDetailScreen.DEBOUNCE_DURATION)) { boundingBox, zoomLevel ->
              boundingBox to zoomLevel
            }

    try {
      combinedFlow.collect { (boundingBox, zoomLevel) ->
        facilitiesViewModel.filterFacilitiesForDisplay(
            bounds = boundingBox,
            zoomLevel = zoomLevel,
            hikeRoute = hike,
            onSuccess = { newFacilities ->
              MapUtils.clearFacilities(mapView)
              if (newFacilities.isNotEmpty()) {
                MapUtils.displayFacilities(newFacilities, mapView, context)
              }
            },
            onNoFacilitiesForState = { MapUtils.clearFacilities(mapView) })
      }
    } catch (e: Exception) {
      Log.e(HikeDetailScreen.LOG_TAG, "Error in facility updates flow", e)
    }
  }
}

@Composable
private fun LaunchedEffectMapviewListener(mapView: MapView, hike: DetailedHike) {
  mapView.addOnFirstLayoutListener { _, _, _, _, _ ->
    // Limit the vertical scrollable area to avoid the user scrolling too far from the hike
    mapView.setScrollableAreaLimitLatitude(
        min(MapScreen.MAP_MAX_LATITUDE, mapView.boundingBox.latNorth),
        max(MapScreen.MAP_MIN_LATITUDE, mapView.boundingBox.latSouth),
        HikeDetailScreen.MAP_BOUNDS_MARGIN)
    if (hike.bounds.maxLon < HikeDetailScreen.MAP_MAX_LONGITUDE ||
        hike.bounds.minLon > HikeDetailScreen.MAP_MIN_LONGITUDE) {
      mapView.setScrollableAreaLimitLongitude(
          max(HikeDetailScreen.MAP_MIN_LONGITUDE, mapView.boundingBox.lonWest),
          min(HikeDetailScreen.MAP_MAX_LONGITUDE, mapView.boundingBox.lonEast),
          HikeDetailScreen.MAP_BOUNDS_MARGIN)
    }
  }
}

@Composable
private fun launchedEffectLoadingOfFacilities(
    facilities: List<Facility>?,
    shouldLoadFacilities: Boolean,
    mapView: MapView,
    facilitiesViewModel: FacilitiesViewModel,
    hike: DetailedHike,
    context: Context
): Boolean {
  var shouldLoadFacilities1 = shouldLoadFacilities
  LaunchedEffect(facilities, shouldLoadFacilities1) {
    if (facilities != null && mapView.repository != null) {
      facilitiesViewModel.filterFacilitiesForDisplay(
          bounds = mapView.boundingBox,
          zoomLevel = mapView.zoomLevelDouble,
          hikeRoute = hike,
          onSuccess = { newFacilities ->
            MapUtils.clearFacilities(mapView)
            if (newFacilities.isNotEmpty()) {
              MapUtils.displayFacilities(newFacilities, mapView, context)
            }
          },
          onNoFacilitiesForState = { MapUtils.clearFacilities(mapView) })
      // Reset the flag after loading
      shouldLoadFacilities1 = false
    }
  }
  return shouldLoadFacilities1
}

/**
 * A composable that displays details about a hike in a bottom sheet.
 *
 * @param detailedHike The route about which information is displayed
 * @param hikesViewModel The view model to handle selected hike data and saving/unsaving operations
 * @param userHikingLevel The user's hiking experience level
 * @param onRunThisHike Callback triggered when "Run This Hike" button is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HikesDetailsBottomScaffold(
    detailedHike: DetailedHike,
    hikesViewModel: HikesViewModel,
    userHikingLevel: HikingLevel,
    onRunThisHike: () -> Unit
) {
  val scaffoldState = rememberBottomSheetScaffoldState()

  val hikeColor = Color(detailedHike.color)
  val isSuitable = detailedHike.difficulty.ordinal <= userHikingLevel.ordinal
  val bookmarkIconId =
      if (detailedHike.isSaved) R.drawable.bookmark_filled_blue else R.drawable.bookmark_no_fill

  BottomSheetScaffold(
      scaffoldState = scaffoldState,
      sheetContainerColor = MaterialTheme.colorScheme.surface,
      sheetPeekHeight = HikeDetailScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT,
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
                      detailedHike.name ?: stringResource(R.string.map_screen_hike_title_default),
                  style = MaterialTheme.typography.headlineLarge,
                  textAlign = TextAlign.Left,
                  modifier = Modifier.testTag(TEST_TAG_HIKE_NAME))
              AppropriatenessMessage(isSuitable)
            }

            Image(
                painter = painterResource(bookmarkIconId),
                contentDescription =
                    if (detailedHike.isSaved)
                        stringResource(R.string.hike_detail_screen_bookmark_hint_on_isSaved_true)
                    else stringResource(R.string.hike_detail_screen_bookmark_hint_on_isSaved_false),
                modifier =
                    Modifier.size(60.dp, 80.dp).testTag(TEST_TAG_BOOKMARK_ICON).clickable {
                      if (detailedHike.isSaved) {
                        hikesViewModel.unsaveHike(detailedHike.id)
                      } else {
                        hikesViewModel.saveHike(detailedHike.id)
                      }
                    },
                contentScale = ContentScale.FillBounds,
            )
          }

          ElevationGraph(
              elevations = detailedHike.elevation,
              modifier =
                  Modifier.fillMaxWidth()
                      .height(60.dp)
                      .padding(4.dp)
                      .testTag(TEST_TAG_ELEVATION_GRAPH),
              styleProperties =
                  ElevationGraphStyleProperties(
                      strokeColor = hikeColor, fillColor = hikeColor.copy(0.5f)))

          val distanceString =
              String.format(
                  Locale.getDefault(),
                  stringResource(R.string.hike_detail_screen_distance_format),
                  detailedHike.distance)
          val elevationGainString =
              String.format(
                  Locale.getDefault(),
                  stringResource(R.string.hike_detail_screen_elevation_gain_format),
                  detailedHike.elevationGain.roundToInt())
          val hour = (detailedHike.estimatedTime / 60).toInt()
          val minute = (detailedHike.estimatedTime % 60).roundToInt()

          val remainingTimeString =
              if (hour > 0) {
                String.format(
                    Locale.getDefault(),
                    stringResource(R.string.hike_detail_screen_estimated_time_format_hour),
                    hour,
                    minute)
              } else {
                String.format(
                    Locale.getDefault(),
                    stringResource(R.string.hike_detail_screen_estimated_time_format_minute),
                    minute)
              }

          DetailRow(
              label = stringResource(R.string.hike_detail_screen_label_distance),
              value = distanceString)
          DetailRow(
              label = stringResource(R.string.hike_detail_screen_label_elevation_gain),
              value = elevationGainString)
          DetailRow(
              label = stringResource(R.string.hike_detail_screen_label_estimated_time),
              value = remainingTimeString)
          DetailRow(
              label = stringResource(R.string.hike_detail_screen_label_difficulty),
              value = stringResource(detailedHike.difficulty.nameResourceId),
              valueColor =
                  Color(
                      ContextCompat.getColor(
                          LocalContext.current, detailedHike.difficulty.colorResourceId)),
          )
          DateDetailRow(
              isSaved = detailedHike.isSaved,
              plannedDate = detailedHike.plannedDate,
              updatePlannedDate = { timestamp: Timestamp? ->
                hikesViewModel.setPlannedDate(detailedHike.id, timestamp)
              })

          // "Run This Hike" button
          BigButton(
              buttonType = ButtonType.PRIMARY,
              label = stringResource(R.string.hike_detail_screen_run_this_hike_button_label),
              onClick = { onRunThisHike() },
              modifier =
                  Modifier.padding(top = 16.dp).fillMaxWidth().testTag(TEST_TAG_RUN_HIKE_BUTTON))
        }
      },
  ) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun DateDetailRow(
    isSaved: Boolean,
    plannedDate: Timestamp?,
    updatePlannedDate: (Timestamp?) -> Unit
) {
  val showingDatePicker = remember { mutableStateOf(false) }
  val datePickerState =
      rememberDatePickerState(
          initialSelectedDateMillis = plannedDate?.toDate()?.time ?: System.currentTimeMillis(),
      )

  var previouslySelectedDate by remember { mutableStateOf<Long?>(plannedDate?.toDate()?.time) }

  fun showDatePicker() {
    showingDatePicker.value = true
  }

  fun dismissDatePicker() {
    showingDatePicker.value = false
  }

  if (showingDatePicker.value) {
    DatePickerDialog(
        modifier = Modifier.testTag(TEST_TAG_DATE_PICKER),
        onDismissRequest = { dismissDatePicker() },
        dismissButton = {
          Button(
              modifier = Modifier.testTag(TEST_TAG_DATE_PICKER_CANCEL_BUTTON),
              onClick = { dismissDatePicker() }) {
                Text(text = stringResource(R.string.hike_detail_screen_date_picker_cancel_button))
              }
        },
        confirmButton = {
          val selectedDate = datePickerState.selectedDateMillis
          val actionIsToPlan = selectedDate != previouslySelectedDate || selectedDate == null

          Button(
              modifier = Modifier.testTag(TEST_TAG_DATE_PICKER_CONFIRM_BUTTON),
              onClick = {
                previouslySelectedDate =
                    confirmDateDetailButton(
                        datePickerState, previouslySelectedDate, updatePlannedDate)
                dismissDatePicker()
              },
              colors =
                  if (actionIsToPlan)
                      ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.primary)
                  else
                      ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          ) {
            Text(
                text =
                    // If the date selected is the same date that is already saved in the hike give
                    // the
                    // user the option to un-plan the hike
                    if (actionIsToPlan)
                        stringResource(R.string.hike_detail_screen_date_picker_confirm_button)
                    else stringResource(R.string.hike_detail_screen_date_picker_unplan_hike_button))
          }
        },
    ) {
      DatePicker(state = datePickerState)
    }
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
            modifier = Modifier.testTag(DetailRow.TEST_TAG_DETAIL_ROW_TAG))

        Button(
            modifier = Modifier.width(90.dp).height(25.dp).testTag(TEST_TAG_ADD_DATE_BUTTON),
            contentPadding = PaddingValues(0.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4285F4), // Blue color to match the image
                    contentColor = Color.White),
            onClick = { showDatePicker() },
        ) {
          Text(
              text = stringResource(R.string.hike_detail_screen_add_a_date_button_text),
              style = MaterialTheme.typography.bodySmall,
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
            modifier = Modifier.testTag(DetailRow.TEST_TAG_DETAIL_ROW_TAG))
        Box(
            modifier =
                Modifier.border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)) {
              Text(
                  text = plannedDate.humanReadableFormat(),
                  // saved Date
                  style = MaterialTheme.typography.bodySmall,
                  modifier =
                      Modifier.clickable { showDatePicker() }
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

/**
 * This function is called when the user confirms the date selection in the date picker. It updates
 * the hike's planned date. If the same date is selected twice, the hike is un-planned.
 *
 * @param datePickerState The state of the date picker
 * @param previouslySelectedDate The previously selected date
 * @param updatePlannedDate The function to update the hike's planned date
 * @return The selected date in milliseconds if it is different from the previously selected date,
 */
@OptIn(ExperimentalMaterial3Api::class)
private fun confirmDateDetailButton(
    datePickerState: DatePickerState,
    previouslySelectedDate: Long?,
    updatePlannedDate: (Timestamp?) -> Unit
): Long? {
  val selectedDateMillis = datePickerState.selectedDateMillis

  // If the same date is selected twice, unselect it, else save the date
  return if (selectedDateMillis == previouslySelectedDate) {
    updatePlannedDate(null)
    null
  } else {
    if (selectedDateMillis != null) {
      updatePlannedDate(Timestamp(Date(selectedDateMillis)))
      selectedDateMillis
    } else {
      null
    }
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
        // The icon is only decorative, the following message is enough for
        // accessibility
        painter = suitableLabelIcon,
        contentDescription = null,
        tint = suitableLabelColor,
        modifier = Modifier.size(16.dp))
    Spacer(modifier = Modifier.width(4.dp))
    Text(
        modifier = Modifier.testTag(HikeDetailScreen.TEST_TAG_APPROPRIATENESS_MESSAGE),
        text = suitableLabelText,
        style = MaterialTheme.typography.bodySmall,
        color = suitableLabelColor)
  }
}
