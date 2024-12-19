package ch.hikemate.app.ui.map

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.hikemate.app.R
import ch.hikemate.app.model.authentication.AuthRepository
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.model.elevation.ElevationRepository
import ch.hikemate.app.model.facilities.FacilitiesRepository
import ch.hikemate.app.model.facilities.FacilitiesViewModel
import ch.hikemate.app.model.facilities.Facility
import ch.hikemate.app.model.facilities.FacilityType
import ch.hikemate.app.model.profile.HikingLevel
import ch.hikemate.app.model.profile.Profile
import ch.hikemate.app.model.profile.ProfileRepository
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.DetailedHike
import ch.hikemate.app.model.route.Hike
import ch.hikemate.app.model.route.HikeDifficulty
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.HikeRoutesRepository
import ch.hikemate.app.model.route.HikesViewModel
import ch.hikemate.app.model.route.LatLong
import ch.hikemate.app.model.route.saved.SavedHike
import ch.hikemate.app.model.route.saved.SavedHikesRepository
import ch.hikemate.app.model.route.toBoundingBox
import ch.hikemate.app.ui.components.BackButton.BACK_BUTTON_TEST_TAG
import ch.hikemate.app.ui.components.CenteredErrorAction
import ch.hikemate.app.ui.components.DetailRow
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.utils.MapUtils
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class HikeDetailScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockNavigationActions: NavigationActions
  private lateinit var authRepository: AuthRepository
  private lateinit var authViewModel: AuthViewModel
  private lateinit var profileRepository: ProfileRepository
  private lateinit var profileViewModel: ProfileViewModel
  private lateinit var mockSavedHikesRepository: SavedHikesRepository
  private lateinit var hikesRepository: HikeRoutesRepository

  private lateinit var hikesViewModel: HikesViewModel

  private lateinit var facilitiesViewModel: FacilitiesViewModel
  private lateinit var facilitiesRepository: FacilitiesRepository
  private lateinit var elevationRepository: ElevationRepository

  private lateinit var context: Context

  private val hikeId = "1"
  private val detailedHike =
      DetailedHike(
          id = hikeId,
          color = Hike(hikeId, false, null, null).getColor(),
          isSaved = true,
          plannedDate = null,
          name = "Sample Hike",
          description =
              "A scenic trail with breathtaking views of the Matterhorn and surrounding glaciers.",
          bounds = Bounds(minLat = 45.9, minLon = 7.6, maxLat = 46.0, maxLon = 7.7),
          waypoints = listOf(LatLong(45.9, 7.6), LatLong(45.95, 7.65), LatLong(46.0, 7.7)),
          elevation = listOf(0.0, 10.0, 20.0, 30.0),
          distance = 13.543077559212616,
          elevationGain = 68.0,
          estimatedTime = 169.3169307105514,
          difficulty = HikeDifficulty.DIFFICULT,
      )
  // Waypoints closer to each other
  private val detailedHike2 =
      DetailedHike(
          id = hikeId,
          color = Hike(hikeId, false, null, null).getColor(),
          isSaved = false,
          plannedDate = null,
          name = "Sample Hike",
          description =
              "A scenic trail with breathtaking views of the Matterhorn and surrounding glaciers.",
          bounds = Bounds(minLat = 45.9, minLon = 7.6, maxLat = 45.91, maxLon = 7.61),
          waypoints =
              listOf(LatLong(45.9, 7.6), LatLong(45.9001, 7.6001), LatLong(45.9002, 7.6002)),
          elevation = listOf(0.0, 10.0, 20.0, 30.0),
          distance = 13.543077559212616,
          elevationGain = 68.0,
          estimatedTime = 169.3169307105514,
          difficulty = HikeDifficulty.DIFFICULT,
      )

  // Bigger bounds
  private val detailedHike3 =
      DetailedHike(
          id = hikeId,
          color = Hike(hikeId, false, null, null).getColor(),
          isSaved = false,
          plannedDate = null,
          name = "Sample Hike",
          description =
              "A scenic trail with breathtaking views of the Matterhorn and surrounding glaciers.",
          bounds = Bounds(minLat = 44.9, minLon = 6.6, maxLat = 46.91, maxLon = 8.61),
          waypoints = listOf(LatLong(45.9, 7.6), LatLong(45.908, 7.605), LatLong(45.91, 7.61)),
          elevation = listOf(0.0, 10.0, 20.0, 30.0),
          distance = 13.543077559212616,
          elevationGain = 68.0,
          estimatedTime = 169.3169307105514,
          difficulty = HikeDifficulty.DIFFICULT,
      )

  private val dispatcher = UnconfinedTestDispatcher()

  private fun setUpCompleteScreen() {
    composeTestRule.setContent {
      context = LocalContext.current
      HikeDetailScreen(
          hikesViewModel = hikesViewModel,
          profileViewModel = profileViewModel,
          authViewModel = authViewModel,
          navigationActions = mockNavigationActions,
          facilitiesViewModel = facilitiesViewModel)
    }
  }

  @OptIn(ExperimentalTestApi::class)
  private fun setUpBottomSheetScaffold(
      hike: DetailedHike = detailedHike,
      onRunThisHike: () -> Unit = {}
  ) {
    composeTestRule.setContent {
      HikeDetailsBottomScaffold(
          detailedHike = hike,
          hikesViewModel = hikesViewModel,
          userHikingLevel = HikingLevel.BEGINNER,
          onRunThisHike = onRunThisHike)
    }

    // Open the bottom sheet
    composeTestRule.waitUntilExactlyOneExists(
        hasTestTag(HikeDetailScreen.TEST_TAG_BOTTOM_SHEET), timeoutMillis = 10000)

    composeTestRule.onNodeWithTag(HikeDetailScreen.TEST_TAG_BOTTOM_SHEET).performTouchInput {
      down(1, position = Offset(centerX, centerY))
      moveTo(1, position = Offset(centerX, 100f))
      up(1)
    }
  }

  private suspend fun setUpSelectedHike(
      hike: DetailedHike,
      waypointsRetrievalSucceeds: Boolean = true,
      elevationRetrievalSucceeds: Boolean = true,
      alreadyLoadData: Boolean = true
  ) {
    val asSavedHike = SavedHike(hike.id, hike.name ?: "", hike.plannedDate)

    val hikeAsOsm =
        HikeRoute(
            id = hike.id,
            bounds = hike.bounds,
            ways = hike.waypoints,
            name = hike.name,
            description = hike.description,
        )

    // Make sure the saved status of the hike is set correctly
    `when`(mockSavedHikesRepository.loadSavedHikes())
        .thenReturn(if (hike.isSaved) listOf(asSavedHike) else emptyList())

    if (waypointsRetrievalSucceeds) {
      if (hike.isSaved) {
        // Make sure that the hike's OSM data can be loaded
        `when`(hikesRepository.getRoutesByIds(any(), any(), any())).thenAnswer {
          val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
          onSuccess(listOf(hikeAsOsm))
        }
      } else {
        // Make sure that the hike can be loaded from bounds
        `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
          val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
          onSuccess(listOf(hikeAsOsm))
        }
      }
    } else {
      if (hike.isSaved) {
        // Make sure the hike's OSM data can't be loaded
        `when`(hikesRepository.getRoutesByIds(any(), any(), any())).thenAnswer {
          val onFailure = it.getArgument<(Exception) -> Unit>(2)
          onFailure(Exception("Failed to load hike bounds"))
        }
      } else {
        // Make sure the hike cannot be loaded from bounds
        `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
          val onFailure = it.getArgument<(Exception) -> Unit>(2)
          onFailure(Exception("Failed to load hikes from bounds"))
        }
      }
    }

    if (waypointsRetrievalSucceeds && elevationRetrievalSucceeds) {
      // Make sure the appropriate elevation profile is obtained when requested
      `when`(elevationRepository.getElevation(any(), any(), any())).thenAnswer {
        val onSuccess = it.getArgument<(List<Double>) -> Unit>(1)
        onSuccess(hike.elevation)
      }
    } else {
      // Make sure the elevation profile can't be obtained when requested
      `when`(elevationRepository.getElevation(any(), any(), any())).thenAnswer {
        val onFailure = it.getArgument<(Exception) -> Unit>(2)
        onFailure(Exception("Failed to load elevation data"))
      }
    }

    // Reset the view model
    hikesViewModel =
        HikesViewModel(
            mockSavedHikesRepository,
            hikesRepository,
            elevationRepository,
            UnconfinedTestDispatcher())

    if (hike.isSaved) {
      // Load the hike from the saved hikes repository
      hikesViewModel.loadSavedHikes()
    } else {
      // Load the hike from OSM, as if the user had searched it on the map
      hikesViewModel.loadHikesInBounds(hike.bounds.toBoundingBox())
    }

    if (alreadyLoadData) {
      // Load the hike's waypoints, but only if the hike was loaded from saved hikes
      if (hike.isSaved) {
        hikesViewModel.retrieveLoadedHikesOsmData()
      }

      // Retrieve the hike's elevation data from the repository
      hikesViewModel.retrieveElevationDataFor(hikeId)

      // Compute the hike's details
      hikesViewModel.computeDetailsFor(hikeId)
    }

    // Mark the hike as selected, to make sure it is the one displayed on the details screen
    hikesViewModel.selectHike(hikeId)
  }

  // To be able to click on an individual date in the date picker dialog we need the date in
  // the format "Today, Friday, December 13, 2024"
  private fun prepareTextTagForDatePickerDialog(): String {
    val formatter = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.ENGLISH)
    return "Today, " + formatter.format(Date())
  }

  private val profile =
      Profile(
          id = "1",
          name = "John Doe",
          email = "john-doe@gmail.com",
          hikingLevel = HikingLevel.AMATEUR,
          joinedDate = Timestamp.now())

  @Before
  fun setUp() {
    mockNavigationActions = mock(NavigationActions::class.java)
    profileRepository = mock(ProfileRepository::class.java)
    profileViewModel = ProfileViewModel(profileRepository)
    authRepository = mock(AuthRepository::class.java)
    authViewModel = AuthViewModel(authRepository, profileRepository)
    hikesRepository = mock(HikeRoutesRepository::class.java)
    elevationRepository = mock(ElevationRepository::class.java)
    mockSavedHikesRepository = mock(SavedHikesRepository::class.java)
    facilitiesRepository = mock(FacilitiesRepository::class.java)

    facilitiesViewModel = FacilitiesViewModel(facilitiesRepository)
    `when`(profileRepository.getProfileById(eq(profile.id), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(Profile) -> Unit>(1)
      onSuccess(profile)
    }
    profileViewModel.getProfileById(profile.id)
    //    Dispatchers.setMain(dispatcher)
    //    facilitiesViewModel = FacilitiesViewModel(facilitiesRepository, dispatcher)
  }

  @Test
  fun hikeDetailsScreen_displaysError_whenWaypointsRetrievalFails() = runTest {
    setUpSelectedHike(detailedHike, waypointsRetrievalSucceeds = false)
    setUpCompleteScreen()

    // So far, the waypoints retrieval should have happened once
    verify(hikesRepository, times(1)).getRoutesByIds(any(), any(), any())

    // An error message should be displayed to the user, along with a go back action
    composeTestRule.onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_MESSAGE)
    composeTestRule
        .onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_BUTTON)
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()

    // Clicking the button should trigger unselecting the hike
    assertNull(hikesViewModel.selectedHike.value)
  }

  @Test
  fun hikeDetailsScreen_displaysError_whenElevationRetrievalFails() = runTest {
    setUpSelectedHike(detailedHike, elevationRetrievalSucceeds = false)
    setUpCompleteScreen()

    // So far, the elevation retrieval should have happened once
    verify(elevationRepository, times(1)).getElevation(any(), any(), any())

    // An error message should be displayed to the user, along with a retry action
    composeTestRule.onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_MESSAGE)
    composeTestRule
        .onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_BUTTON)
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()

    // Clicking the button should trigger a retry of the elevation retrieval
    verify(elevationRepository, times(2)).getElevation(any(), any(), any())
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun hikeDetailsScreen_loadsMissingData() = runTest {
    setUpSelectedHike(detailedHike, alreadyLoadData = false)
    setUpCompleteScreen()

    composeTestRule.waitUntilExactlyOneExists(
        hasTestTag(HikeDetailScreen.TEST_TAG_MAP), timeoutMillis = 10000)

    verify(hikesRepository).getRoutesByIds(any(), any(), any())
    verify(elevationRepository).getElevation(any(), any(), any())
  }

  @Test
  fun hikeDetailScreen_displaysMap() = runTest {
    setUpSelectedHike(detailedHike)
    setUpCompleteScreen()

    composeTestRule.onNodeWithTag(HikeDetailScreen.TEST_TAG_MAP).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_displaysHikeNameAndBookmarkIcon() = runTest {
    setUpSelectedHike(detailedHike)
    setUpBottomSheetScaffold()

    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_HIKE_NAME)
        .assertTextEquals(detailedHike.name!!)
    composeTestRule.onNodeWithTag(HikeDetailScreen.TEST_TAG_BOOKMARK_ICON).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_showsElevationGraph() = runTest {
    setUpSelectedHike(detailedHike)
    setUpBottomSheetScaffold()

    composeTestRule.onNodeWithTag(HikeDetailScreen.TEST_TAG_ELEVATION_GRAPH).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_showsPlannedDate_whenDateIsSet() = runTest {
    val plannedDate = Timestamp(1622563200, 0)
    val hike = detailedHike.copy(isSaved = true, plannedDate = plannedDate)
    setUpSelectedHike(hike)

    // Display only the bottom part without the map
    setUpBottomSheetScaffold(hike)

    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_PLANNED_DATE_TEXT_BOX)
        .assertIsDisplayed()
  }

  @Test
  fun hikeDetails_showsCorrectDetailsRowsWhenNotSaved() = runTest {
    val hike = detailedHike.copy(isSaved = false, plannedDate = null)
    setUpSelectedHike(hike)

    // Display only the bottom part without the map
    setUpBottomSheetScaffold(hike)

    composeTestRule.onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_TAG).assertCountEquals(5)
    composeTestRule.onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_VALUE).assertCountEquals(5)
  }

  @Test
  fun hikeDetails_showsCorrectDetailsRowsWhenSavedAndNoDateIsSet() = runTest {
    val hike = detailedHike.copy(isSaved = true, plannedDate = null)
    setUpSelectedHike(hike)

    // Display only the bottom part without the map
    setUpBottomSheetScaffold(hike)

    composeTestRule.onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_TAG).assertCountEquals(6)
    composeTestRule.onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_VALUE).assertCountEquals(5)
    composeTestRule.onNodeWithTag(HikeDetailScreen.TEST_TAG_ADD_DATE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_showsCorrectDetailsRowsWhenSavedAndDateIsSet() = runTest {
    val plannedDate = Timestamp(1622563200, 0)
    val hike = detailedHike.copy(isSaved = true, plannedDate = plannedDate)
    setUpSelectedHike(hike)

    // Display only the bottom part without the map
    setUpBottomSheetScaffold(hike)

    composeTestRule.onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_TAG).assertCountEquals(6)
    composeTestRule.onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_VALUE).assertCountEquals(5)
    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_PLANNED_DATE_TEXT_BOX)
        .assertIsDisplayed()
  }

  @Test
  fun hikeDetailScreen_navigatesBackOnBackButtonClick() = runTest {
    setUpSelectedHike(detailedHike)

    // Display the whole details screen for the hike, including the map
    setUpCompleteScreen()

    doNothing().`when`(mockNavigationActions).goBack()

    composeTestRule
        .onNodeWithTag(BACK_BUTTON_TEST_TAG)
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()

    composeTestRule.waitForIdle()

    assertNull(hikesViewModel.selectedHike.value)
    verify(mockNavigationActions).goBack()
  }

  @Test
  fun hikeDetails_opensDatePicker_whenAddDateButtonClicked() = runTest {
    val hike = detailedHike.copy(isSaved = true, plannedDate = null)
    setUpSelectedHike(hike)

    // Display only the bottom part without the map
    setUpBottomSheetScaffold(hike)

    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_ADD_DATE_BUTTON)
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()
    composeTestRule.onNodeWithTag(HikeDetailScreen.TEST_TAG_DATE_PICKER).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_datePickerDismisses_whenClickedOnCancel() = runTest {
    val hike = detailedHike.copy(isSaved = true, plannedDate = null)
    setUpSelectedHike(hike)

    // Display only the bottom part without the map
    setUpBottomSheetScaffold(hike)

    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_ADD_DATE_BUTTON)
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()
    composeTestRule.onNodeWithTag(HikeDetailScreen.TEST_TAG_DATE_PICKER).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_DATE_PICKER_CANCEL_BUTTON)
        .performClick()

    composeTestRule.onNodeWithTag(HikeDetailScreen.TEST_TAG_DATE_PICKER).assertIsNotDisplayed()
  }

  @Test
  fun hikeDetails_datePickerDismisses_whenClickedOnConfirm() = runTest {
    val hike = detailedHike.copy(isSaved = true, plannedDate = null)
    setUpSelectedHike(hike)

    // Display only the bottom part without the map
    setUpBottomSheetScaffold(hike)

    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_ADD_DATE_BUTTON)
        .assertHasClickAction()
        .performClick()
    composeTestRule.onNodeWithTag(HikeDetailScreen.TEST_TAG_DATE_PICKER).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_DATE_PICKER_CONFIRM_BUTTON)
        .performClick()

    composeTestRule.onNodeWithTag(HikeDetailScreen.TEST_TAG_DATE_PICKER).assertIsNotDisplayed()
  }

  @Test
  fun hikeDetails_showsDateTextBox_whenHikeIsPlanned() = runTest {
    var currentPlannedDate by mutableStateOf<Timestamp?>(null)
    val datePickerDateTextTag = prepareTextTagForDatePickerDialog()

    composeTestRule.setContent {
      DateDetailRow(
          isSaved = true,
          plannedDate = currentPlannedDate,
          updatePlannedDate = { currentPlannedDate = it })
    }

    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_ADD_DATE_BUTTON)
        .assertHasClickAction()
        .performClick()
    composeTestRule.onNodeWithText(datePickerDateTextTag).performClick()
    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_DATE_PICKER_CONFIRM_BUTTON)
        .performClick()

    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_PLANNED_DATE_TEXT_BOX)
        .assertIsDisplayed()
  }

  @Test
  fun hikeDetails_datePickerUnplansHike_whenPlannedDateIsReselected() = runTest {
    var currentPlannedDate by mutableStateOf<Timestamp?>(null)
    val datePickerDateTextTag = prepareTextTagForDatePickerDialog()

    composeTestRule.setContent {
      context = LocalContext.current
      DateDetailRow(
          isSaved = true,
          plannedDate = currentPlannedDate,
          updatePlannedDate = { currentPlannedDate = it })
    }

    // Selects the current date
    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_ADD_DATE_BUTTON)
        .assertHasClickAction()
        .performClick()
    composeTestRule.onNodeWithText(datePickerDateTextTag).performClick()
    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_DATE_PICKER_CONFIRM_BUTTON)
        .performClick()

    // Selects the current date again, effectively un-planning the hike
    composeTestRule.onNodeWithTag(HikeDetailScreen.TEST_TAG_PLANNED_DATE_TEXT_BOX).performClick()
    composeTestRule.onNodeWithText(datePickerDateTextTag).performClick()
    // Assert than it now says "Unplan this hike"
    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_DATE_PICKER_CONFIRM_BUTTON)
        .assertTextEquals(
            context.getString(R.string.hike_detail_screen_date_picker_unplan_hike_button))
        .performClick()

    // The hike should no longer be planned
    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_PLANNED_DATE_TEXT_BOX)
        .assertIsNotDisplayed()
  }

  @Test
  fun hikeDetails_showsCorrectDetailedHikesValues() = runTest {
    setUpSelectedHike(detailedHike)

    // Display only the bottom part without the map
    setUpBottomSheetScaffold(detailedHike)

    val distanceString = String.format(Locale.getDefault(), "%.2f", detailedHike.distance)
    val elevationGainString = detailedHike.elevationGain.roundToInt().toString()
    val hourString =
        String.format(Locale.getDefault(), "%02d", (detailedHike.estimatedTime / 60).toInt())
    val minuteString =
        String.format(Locale.getDefault(), "%02d", (detailedHike.estimatedTime % 60).roundToInt())

    composeTestRule
        .onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_VALUE)
        .assertAny(hasText("${distanceString}km"))
    composeTestRule
        .onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_VALUE)
        .assertAny(hasText("${elevationGainString}m"))
    composeTestRule
        .onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_VALUE)
        .assertAny(hasText("${hourString}h${minuteString}"))
    composeTestRule
        .onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_VALUE)
        .assertAny(
            hasText(
                ApplicationProvider.getApplicationContext<Context>()
                    .getString(detailedHike.difficulty.nameResourceId)))
  }

  @Test
  fun hikeDetails_showsCorrectDetailedHikesValues_whenTimeIsLessThan60Min() = runTest {
    val hike = detailedHike.copy(estimatedTime = 45.0) // test hike that takes just 45 min
    setUpSelectedHike(hike)

    // Display only the bottom part without the map
    setUpBottomSheetScaffold(hike)

    val minuteString =
        String.format(Locale.getDefault(), "%02d", (hike.estimatedTime % 60).roundToInt())

    composeTestRule
        .onAllNodesWithTag(DetailRow.TEST_TAG_DETAIL_ROW_VALUE)
        .assertAny(hasText("${minuteString}min"))
  }

  @Test
  fun hikeDetails_showsRunThisHikeButton_andTriggersOnRunThisHike() = runTest {
    val onRunThisHike = mock<() -> Unit>()
    // It is important that the hike be unsaved, otherwise there will be an additional "date" field
    // in the details bottom scaffold, which will render the button just below the screen limit,
    // making it undisplayed.
    val hike = detailedHike.copy(isSaved = false, plannedDate = null)
    setUpSelectedHike(hike)

    setUpBottomSheetScaffold(hike = hike, onRunThisHike = onRunThisHike)

    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_RUN_HIKE_BUTTON)
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()

    verify(onRunThisHike).invoke()
  }

  @Test
  fun hikeDetails_fetchesFacilities() = runTest {
    val listFacility =
        listOf(
            Facility(
                type = FacilityType.TOILETS, // We'll test the toilets drawable
                coordinates = LatLong(45.9, 7.6)))
    `when`(facilitiesRepository.getFacilities(any(), any(), any())).then {
      val onSuccess = it.getArgument<(List<Facility>) -> Unit>(1)
      onSuccess(listFacility)
    }
    setUpSelectedHike(detailedHike2)
    composeTestRule.setContent {
      HikeDetailScreen(
          hikesViewModel = hikesViewModel,
          profileViewModel = profileViewModel,
          authViewModel = authViewModel,
          navigationActions = mockNavigationActions,
          facilitiesViewModel = facilitiesViewModel)
    }

    verify(facilitiesRepository).getFacilities(any(), any(), any())
  }

  // @Test
  fun hikeDetails_displaysCorrectDrawableForFacilityType() =
      runTest(dispatcher) {
        setUpSelectedHike(detailedHike2)

        val bounds = detailedHike2.bounds.toBoundingBox()
        val center = LatLong(bounds.centerLatitude, bounds.centerLongitude)
        val testFacility = Facility(type = FacilityType.TOILETS, coordinates = center)
        val listFacility = listOf(testFacility)

        `when`(facilitiesRepository.getFacilities(any(), any(), any())).then {
          val onSuccess = it.getArgument<(List<Facility>) -> Unit>(1)
          onSuccess(listFacility)
        }

        lateinit var mapView: MapView
        lateinit var context: Context

        composeTestRule.setContent {
          context = LocalContext.current
          mapView = hikeDetailsMap(detailedHike2, facilitiesViewModel)
        }

        facilitiesViewModel.fetchFacilitiesForHike(detailedHike3)
        // Simulate ViewModel and map updates

        // The reason for await is I found out the test only works with it I don't know why.
        composeTestRule.awaitIdle()

        // Assert marker presence and validity
        val facilityMarkers =
            mapView.overlays.filterIsInstance<Marker>().filter {
              it.relatedObject == MapUtils.FACILITIES_RELATED_OBJECT_NAME
            }

        assertTrue("Marker should be added to the map", facilityMarkers.isNotEmpty())

        val marker = facilityMarkers.first()
        val expectedDrawable = ContextCompat.getDrawable(context, R.drawable.toilets)

        assertEquals(1, facilityMarkers.size)
        assertTrue(
            "Marker should have correct drawable icon",
            areSameDrawable(expectedDrawable, marker.icon))
        assertEquals(testFacility.coordinates.lat, marker.position.latitude, 0.0001)
        assertEquals(testFacility.coordinates.lon, marker.position.longitude, 0.0001)
      }

  // @Test
  fun hikeDetails_hidesFacilities_whenZoomLevelIsInsufficient() =
      runTest(dispatcher) {
        setUpSelectedHike(detailedHike3)

        lateinit var mapView: MapView

        val bounds = detailedHike3.bounds.toBoundingBox()
        val center = LatLong(bounds.centerLatitude, bounds.centerLongitude)
        val testFacility = Facility(type = FacilityType.TOILETS, coordinates = center)
        val listFacility = listOf(testFacility)
        `when`(facilitiesRepository.getFacilities(any(), any(), any())).then {
          val onSuccess = it.getArgument<(List<Facility>) -> Unit>(1)
          onSuccess(listFacility)
        }

        composeTestRule.setContent { mapView = hikeDetailsMap(detailedHike3, facilitiesViewModel) }

        facilitiesViewModel.fetchFacilitiesForHike(detailedHike3)
        // The reason for await is I found out the test only works with it I don't know why.
        composeTestRule.awaitIdle()

        // Verify facilities are hidden at insufficient zoom levels
        val finalMarkers =
            mapView.overlays.filterIsInstance<Marker>().filter {
              it.relatedObject == MapUtils.FACILITIES_RELATED_OBJECT_NAME
            }

        assertTrue(
            "Facilities should be hidden at zoom level ${mapView.zoomLevelDouble}, " +
                "below minimum",
            finalMarkers.isEmpty())
      }

  // @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // Helper function to compare drawables
  fun areSameDrawable(drawable1: Drawable?, drawable2: Drawable?): Boolean {
    if (drawable1 == null || drawable2 == null) return false

    // Convert both drawables to bitmap for comparison
    val bitmap1 = drawable1.toBitmap()
    val bitmap2 = drawable2.toBitmap()

    return bitmap1.sameAs(bitmap2)
  }

  @Test
  fun testSignOutAndNavigateToAuthFromMapScreen() = runTest {
    `when`(profileRepository.getProfileById(any(), any(), any())).thenAnswer {
      val onError = it.getArgument<(Exception) -> Unit>(2)
      onError(Exception("No profile found"))
    }
    `when`(authRepository.signOut(any())).thenAnswer {
      val onSuccess = it.getArgument<() -> Unit>(0)
      onSuccess()
    }

    profileViewModel.getProfileById(profile.id)

    setUpSelectedHike(detailedHike)
    setUpCompleteScreen()
    composeTestRule
        .onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_MESSAGE)
        .assertIsDisplayed()
        .assertTextEquals(context.getString(R.string.an_error_occurred_while_fetching_the_profile))
    composeTestRule.onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_BUTTON).performClick()

    verify(authRepository).signOut(any())
    verify(mockNavigationActions).navigateTo(Route.AUTH)
    assertNull(authViewModel.currentUser.value)
  }

  @Test
  fun hikeDetails_savesMapStateOnGoBack() = runTest {
    setUpSelectedHike(detailedHike)
    setUpCompleteScreen()

    composeTestRule.onNodeWithTag(BACK_BUTTON_TEST_TAG).performClick()
    composeTestRule.waitForIdle()

    val zoomLevel = MapUtils.calculateBestZoomLevel(detailedHike.bounds).toDouble()
    val center = MapUtils.getGeographicalCenter(detailedHike.bounds)
    assertEquals(zoomLevel, hikesViewModel.getMapState().zoom, 0.1)
    assertEquals(center.latitude, hikesViewModel.getMapState().center.latitude, 0.1)
    assertEquals(center.longitude, hikesViewModel.getMapState().center.longitude, 0.1)
  }
}
