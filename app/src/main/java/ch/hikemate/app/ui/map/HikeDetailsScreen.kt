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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import ch.hikemate.app.model.facilities.FacilitiesViewModel
import ch.hikemate.app.model.facilities.Facility
import ch.hikemate.app.model.profile.HikingLevel
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.DetailedHike
import ch.hikemate.app.model.route.HikesViewModel
import ch.hikemate.app.model.route.LatLong
import ch.hikemate.app.ui.components.AsyncStateHandler
import ch.hikemate.app.ui.components.BackButton
import ch.hikemate.app.ui.components.BigButton
import ch.hikemate.app.ui.components.ButtonType
import ch.hikemate.app.ui.components.CenteredLoadingAnimation
import ch.hikemate.app.ui.components.DetailRow
import ch.hikemate.app.ui.components.ElevationGraph
import ch.hikemate.app.ui.components.ElevationGraphStyleProperties
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
import ch.hikemate.app.utils.LocationUtils
import ch.hikemate.app.utils.MapUtils
import ch.hikemate.app.utils.humanReadableFormat
import com.google.firebase.Timestamp
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

object HikeDetailScreen {
    const val MAP_MAX_ZOOM = 18.0
    const val MAP_MAX_LONGITUDE = 180.0
    const val MAP_MIN_LONGITUDE = -180.0
    const val MAP_BOUNDS_MARGIN: Int = 100
    const val MARGIN_BOUNDS = 0.001

    const val MIN_ZOOM_FOR_FACILITIES = 13.0
    const val DEBOUNCE_DURATION = 150L
    const val MAX_DISTANCE_FROM_ROUTE_TO_FACILITY = 3000
    val MAX_AMOUNT_OF_FACILITIES_ZOOM_LEVEL = mapOf(
        13.0 to 10,
        14.0 to 20,
        15.0 to 25,
        16.0 to 30,
        17.0 to 35,
        18.0 to 40
    )


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

    LaunchedEffect(selectedHike) {
        if (selectedHike == null) {
            Log.e(HikeDetailScreen.LOG_TAG, "No selected hike, going back")
            navigationActions.goBack()
        }
    }

    when {
        // If the hike is null, the LaunchedEffect will navigate back, don't handle it here
        selectedHike == null -> CenteredLoadingAnimation()

        // All the details of the hike have been computed, display them
        hikesViewModel.areDetailsComputedFor(selectedHike!!) -> {
            val detailedHike = selectedHike!!.withDetailsOrThrow()
            val errorMessageIdState = profileViewModel.errorMessageId.collectAsState()
            val profileState = profileViewModel.profile.collectAsState()

            // This is the list of all the facilities inside the hike's bounds
            val facilities = remember { mutableStateOf<List<Facility>?>( null)}
            LaunchedEffect(Unit) {
                // This is calculated so that elements that are near but not contained into the bounds of a
                // Hike
                // are still displayed this is actually a very common occurrence.
                val boundsWithMargin =
                    Bounds(
                        detailedHike.bounds.minLat - HikeDetailScreen.MARGIN_BOUNDS,
                        detailedHike.bounds.minLon - HikeDetailScreen.MARGIN_BOUNDS,
                        detailedHike.bounds.maxLat + HikeDetailScreen.MARGIN_BOUNDS,
                        detailedHike.bounds.maxLon + HikeDetailScreen.MARGIN_BOUNDS
                    )
                // Get all the facilities within the bounds.
                facilitiesViewModel.getFacilities(boundsWithMargin, { facilities.value = it }, {})
            }

            AsyncStateHandler(
                errorMessageIdState = errorMessageIdState,
                actionContentDescriptionStringId = R.string.go_back,
                actionOnErrorAction = { navigationActions.navigateTo(Route.MAP) },
                valueState = profileState,
            ) { profile ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(Screen.HIKE_DETAILS)
                ) {
                    // Display the hike's actual information
                    HikeDetailsContent(
                        detailedHike,
                        navigationActions,
                        hikesViewModel,
                        profile.hikingLevel,
                        facilities
                    )
                }
            }
        }

        // Details have not been computed yet, but the elevation has been retrieved
        selectedHike!!.elevation.obtained() -> {
            hikesViewModel.computeDetailsFor(selectedHike!!.id)
            CenteredLoadingAnimation(stringResource(R.string.hike_details_screen_computing_details))
        }

        // Waypoints are available, but elevation hasn't been retrieved yet. Retrieve it
        hikesViewModel.canElevationDataBeRetrievedFor(selectedHike!!) -> {
            hikesViewModel.retrieveElevationDataFor(selectedHike!!.id)
            CenteredLoadingAnimation(stringResource(R.string.hike_details_screen_obtaining_elevation))
        }

        // Waypoints are not available, retrieve them
        else -> {
            hikesViewModel.retrieveLoadedHikesOsmData()
            CenteredLoadingAnimation(stringResource(R.string.hike_details_screen_obtaining_osm_data))
        }
    }
}

@Composable
fun HikeDetailsContent(
    hike: DetailedHike,
    navigationActions: NavigationActions,
    hikesViewModel: HikesViewModel,
    userHikingLevel: HikingLevel,
    facilities: MutableState<List<Facility>?>
) {
    BackHandler { hikesViewModel.unselectHike() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(Screen.HIKE_DETAILS)
    ) {
        // Display the map and the zoom buttons
        val mapView = hikeDetailsMap(hike, facilities)

        // Display the back button on top of the map
        BackButton(
            navigationActions = navigationActions,
            modifier = Modifier
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                .safeDrawingPadding(),
            onClick = { hikesViewModel.unselectHike() })

        // Zoom buttons at the bottom right of the screen
        ZoomMapButton(
            onZoomIn = { mapView.controller.zoomIn() },
            onZoomOut = { mapView.controller.zoomOut() },
            modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT + 8.dp)
        )

        // Prevent the app from crashing when the "run hike" button is spammed
        var wantToRunHike by remember { mutableStateOf(false) }
        LaunchedEffect(wantToRunHike) {
            if (wantToRunHike) navigationActions.navigateTo(Screen.RUN_HIKE)
        }

        // Display the details of the hike at the bottom of the screen
        HikesDetailsBottomScaffold(
            hike, hikesViewModel, userHikingLevel, onRunThisHike = { wantToRunHike = true })
    }
}

@Composable
fun hikeDetailsMap(hike: DetailedHike, facilities: MutableState<List<Facility>?>): MapView {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val facilitiesDisplayed = remember { mutableStateOf(false) }
    val hikeZoomLevel = MapUtils.calculateBestZoomLevel(hike.bounds).toDouble()
    val hikeCenter = MapUtils.getGeographicalCenter(hike.bounds)

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

    // The state of the current bounds
    val boundingBoxState = remember { MutableStateFlow(mapView.boundingBox) }
    // The state of the current zoom level
    val zoomLevelState = remember { MutableStateFlow(mapView.zoomLevelDouble) }
    // Display the facilities according to changes in the boundingBoxState and zoomLevelState


    LaunchedEffect(facilities.value,mapView) {
        if(facilities.value!=null)
            combine(
                // Debounce acts as a delay triggering less redrawings and thus being more performant.
                boundingBoxState.debounce(HikeDetailScreen.DEBOUNCE_DURATION),
                zoomLevelState.debounce(HikeDetailScreen.DEBOUNCE_DURATION)
            ) { boundingBox, zoomLevel ->
                handleFacilities(
                    mapView,
                    zoomLevel,
                    boundingBox,
                    facilities.value!!,
                    hike,
                    facilitiesDisplayed,
                    context
                )
            }.launchIn(scope)
    }

    // This listener basically updates the state of the variables of the bounds and zoom level
    mapView.addMapListener(
        object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                event?.let { boundingBoxState.value = mapView.boundingBox }
                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                event?.let { zoomLevelState.value = mapView.zoomLevelDouble }
                return true
            }
        })

    // When the map is ready, it will have computed its bounding box
    mapView.addOnFirstLayoutListener { _, _, _, _, _ ->
        // Limit the vertical scrollable area to avoid the user scrolling too far from the hike
        mapView.setScrollableAreaLimitLatitude(
            min(MapScreen.MAP_MAX_LATITUDE, mapView.boundingBox.latNorth),
            max(MapScreen.MAP_MIN_LATITUDE, mapView.boundingBox.latSouth),
            HikeDetailScreen.MAP_BOUNDS_MARGIN
        )
        if (hike.bounds.maxLon < HikeDetailScreen.MAP_MAX_LONGITUDE ||
            hike.bounds.minLon > HikeDetailScreen.MAP_MIN_LONGITUDE
        ) {
            mapView.setScrollableAreaLimitLongitude(
                max(HikeDetailScreen.MAP_MIN_LONGITUDE, mapView.boundingBox.lonWest),
                min(HikeDetailScreen.MAP_MAX_LONGITUDE, mapView.boundingBox.lonEast),
                HikeDetailScreen.MAP_BOUNDS_MARGIN
            )
        }
        if(facilities.value!=null){
            handleFacilities(
                mapView,
                zoomLevelState.value,
                boundingBoxState.value,
                facilities.value!!,
                hike,
                facilitiesDisplayed,
                context
            )
        }
    }

    // Show the selected hike on the map
    // OnLineClick does nothing, the line should not be clickable
    Log.d("HikeDetailScreen", "Drawing hike on map: ${hike.bounds}")
    MapUtils.showHikeOnMap(
        mapView = mapView, waypoints = hike.waypoints, color = hike.color, onLineClick = {})

    // Display the map as a composable
    AndroidView(
        factory = { mapView },
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(bottom = 300.dp) // Reserve space for the scaffold at the bottom
            .testTag(TEST_TAG_MAP)
    )

    return mapView
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
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .weight(1f),
                    ) {
                        Text(
                            text =
                            detailedHike.name
                                ?: stringResource(R.string.map_screen_hike_title_default),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Left,
                            modifier = Modifier.testTag(TEST_TAG_HIKE_NAME)
                        )
                        AppropriatenessMessage(isSuitable)
                    }

                    Image(
                        painter = painterResource(bookmarkIconId),
                        contentDescription =
                        if (detailedHike.isSaved)
                            stringResource(R.string.hike_detail_screen_bookmark_hint_on_isSaved_true)
                        else stringResource(R.string.hike_detail_screen_bookmark_hint_on_isSaved_false),
                        modifier =
                        Modifier
                            .size(60.dp, 80.dp)
                            .testTag(TEST_TAG_BOOKMARK_ICON)
                            .clickable {
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
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(4.dp)
                        .testTag(TEST_TAG_ELEVATION_GRAPH),
                    styleProperties =
                    ElevationGraphStyleProperties(
                        strokeColor = hikeColor, fillColor = hikeColor.copy(0.1f)
                    )
                )

                val distanceString =
                    String.format(Locale.getDefault(), "%.2f", detailedHike.distance)
                val elevationGainString = detailedHike.elevationGain.roundToInt().toString()
                val hourString =
                    String.format(
                        Locale.getDefault(),
                        "%02d",
                        (detailedHike.estimatedTime / 60).toInt()
                    )
                val minuteString =
                    String.format(
                        Locale.getDefault(), "%02d", (detailedHike.estimatedTime % 60).roundToInt()
                    )

                DetailRow(
                    label = stringResource(R.string.hike_detail_screen_label_distance),
                    value = "${distanceString}km"
                )
                DetailRow(
                    label = stringResource(R.string.hike_detail_screen_label_elevation_gain),
                    value = "${elevationGainString}m"
                )
                DetailRow(
                    label = stringResource(R.string.hike_detail_screen_label_estimated_time),
                    value =
                    if (detailedHike.estimatedTime / 60 < 1) "${minuteString}min"
                    else "${hourString}h${minuteString}"
                )
                DetailRow(
                    label = stringResource(R.string.hike_detail_screen_label_difficulty),
                    value = stringResource(detailedHike.difficulty.nameResourceId),
                    valueColor =
                    Color(
                        ContextCompat.getColor(
                            LocalContext.current, detailedHike.difficulty.colorResourceId
                        )
                    ),
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
                    Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .testTag(TEST_TAG_RUN_HIKE_BUTTON)
                )
            }
        },
        sheetPeekHeight = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT
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
    val datePickerState = rememberDatePickerState()

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
                Button(
                    modifier = Modifier.testTag(TEST_TAG_DATE_PICKER_CONFIRM_BUTTON),
                    onClick = {
                        if (datePickerState.selectedDateMillis != null) {
                            updatePlannedDate(Timestamp(Date(datePickerState.selectedDateMillis!!)))
                        }

                        dismissDatePicker()
                    }) {
                    Text(text = stringResource(R.string.hike_detail_screen_date_picker_confirm_button))
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
                valueColor = Color(0xFF3B9DE8)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.hike_detail_screen_label_planned_for),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag(DetailRow.TEST_TAG_DETAIL_ROW_TAG)
                )

                Button(
                    modifier = Modifier
                        .width(90.dp)
                        .height(25.dp)
                        .testTag(TEST_TAG_ADD_DATE_BUTTON),
                    contentPadding = PaddingValues(0.dp),
                    colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4285F4), // Blue color to match the image
                        contentColor = Color.White
                    ),
                    onClick = { showDatePicker() },
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
                valueColor = Color(0xFF3B9DE8)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.hike_detail_screen_label_planned_for),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag(DetailRow.TEST_TAG_DETAIL_ROW_TAG)
                )
                Box(
                    modifier =
                    Modifier
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = plannedDate.humanReadableFormat(),
                        // saved Date
                        style = MaterialTheme.typography.bodySmall,
                        modifier =
                        Modifier
                            .clickable { showDatePicker() }
                            .testTag(TEST_TAG_PLANNED_DATE_TEXT_BOX))
                }
            }
        }
    } else {
        DetailRow(
            label = stringResource(R.string.hike_detail_screen_label_status),
            value = stringResource(R.string.hike_detail_screen_value_not_saved),
            valueColor = MaterialTheme.colorScheme.onSurface
        )
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
            painter = suitableLabelIcon, // The icon is only decorative, the following message is enough for accessibility
            contentDescription = null,
            tint = suitableLabelColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = suitableLabelText,
            style = MaterialTheme.typography.bodySmall,
            color = suitableLabelColor
        )
    }

}

private fun handleFacilities(
    mapView: MapView,
    zoomLevel: Double,
    boundingBox: BoundingBox,
    facilities: List<Facility>,
    hike: DetailedHike,
    facilitiesDisplayed: MutableState<Boolean>,
    context: Context,
) {
    // If the user is too far away the facilities won't be displayed
    if (zoomLevel > HikeDetailScreen.MIN_ZOOM_FOR_FACILITIES) {
        val distanceFromCenterToHike =
            LocationUtils.projectLocationOnHike(
                LatLong(
                    mapView.boundingBox.centerLatitude,
                    mapView.boundingBox.centerLongitude
                ),
                hike
            )!!
                .distanceFromRoute
        // This particular case is useful for hikes with very big bounds around big cities
        // this avoids drawing facilities that are really far away but are contained into
        // the bounds
        // since these are useless
        if (distanceFromCenterToHike <=
            HikeDetailScreen.MAX_DISTANCE_FROM_ROUTE_TO_FACILITY
        ) {
            // We make a list with facilities that are contained into the current bounds and
            // don't
            // surpass the max amount of facilities to not draw too many facilities.
            val filteredFacilities = mutableListOf<Facility>()
            for (facility in facilities) {
                if (filteredFacilities.size >= (HikeDetailScreen.MAX_AMOUNT_OF_FACILITIES_ZOOM_LEVEL.get(
                        zoomLevel
                    ) ?: 0)
                ) break
                if (boundingBox.contains(
                        GeoPoint(facility.coordinates.lat, facility.coordinates.lon)
                    )
                ) {
                    filteredFacilities.add(facility)
                }
            }
            // Function call to draw the icons.
            MapUtils.displayFacilities(filteredFacilities, mapView, context)
            facilitiesDisplayed.value = true
        }
    } else if (facilitiesDisplayed.value) {
        // Removes any facilities that are drawn inside the map.
        MapUtils.clearFacilities(mapView)
        facilitiesDisplayed.value = false
    }
}
