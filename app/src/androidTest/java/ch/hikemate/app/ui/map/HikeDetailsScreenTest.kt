package ch.hikemate.app.ui.map

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.hikemate.app.model.authentication.AuthRepository
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.model.elevation.ElevationService
import ch.hikemate.app.model.profile.HikingLevel
import ch.hikemate.app.model.profile.Profile
import ch.hikemate.app.model.profile.ProfileRepository
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.DetailedHikeRoute
import ch.hikemate.app.model.route.HikeDifficulty
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.HikeRoutesRepository
import ch.hikemate.app.model.route.LatLong
import ch.hikemate.app.model.route.ListOfHikeRoutesViewModel
import ch.hikemate.app.model.route.saved.SavedHikesRepository
import ch.hikemate.app.model.route.saved.SavedHikesViewModel
import ch.hikemate.app.ui.components.BackButton.BACK_BUTTON_TEST_TAG
import ch.hikemate.app.ui.map.HikeDetailScreen.HIKE_DETAILS_TEST_TAG_ADD_DATE_BUTTON
import ch.hikemate.app.ui.map.HikeDetailScreen.HIKE_DETAILS_TEST_TAG_BOOKMARK_ICON
import ch.hikemate.app.ui.map.HikeDetailScreen.HIKE_DETAILS_TEST_TAG_DATE_PICKER
import ch.hikemate.app.ui.map.HikeDetailScreen.HIKE_DETAILS_TEST_TAG_DATE_PICKER_CANCEL_BUTTON
import ch.hikemate.app.ui.map.HikeDetailScreen.HIKE_DETAILS_TEST_TAG_DATE_PICKER_CONFIRM_BUTTON
import ch.hikemate.app.ui.map.HikeDetailScreen.HIKE_DETAILS_TEST_TAG_DETAIL_ROW_TAG
import ch.hikemate.app.ui.map.HikeDetailScreen.HIKE_DETAILS_TEST_TAG_DETAIL_ROW_VALUE
import ch.hikemate.app.ui.map.HikeDetailScreen.HIKE_DETAILS_TEST_TAG_ELEVATION_GRAPH
import ch.hikemate.app.ui.map.HikeDetailScreen.HIKE_DETAILS_TEST_TAG_HIKE_NAME
import ch.hikemate.app.ui.map.HikeDetailScreen.HIKE_DETAILS_TEST_TAG_MAP
import ch.hikemate.app.ui.map.HikeDetailScreen.HIKE_DETAILS_TEST_TAG_PLANNED_DATE_TEXT_BOX
import ch.hikemate.app.ui.map.HikeDetailScreen.HIKE_DETAILS_TEST_TAG_RUN_HIKE_BUTTON
import ch.hikemate.app.ui.navigation.NavigationActions
import com.google.firebase.Timestamp
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class HikeDetailScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockNavigationActions: NavigationActions
  private lateinit var authRepository: AuthRepository
  private lateinit var authViewModel: AuthViewModel
  private lateinit var profileRepository: ProfileRepository
  private lateinit var profileViewModel: ProfileViewModel
  private lateinit var savedHikesViewModel: SavedHikesViewModel
  private lateinit var mockSavedHikesRepository: SavedHikesRepository
  private lateinit var hikesRepository: HikeRoutesRepository
  private lateinit var elevationService: ElevationService
  private lateinit var listOfHikeRoutesViewModel: ListOfHikeRoutesViewModel
  private val route =
      HikeRoute(
          id = "1",
          bounds = Bounds(minLat = 45.9, minLon = 7.6, maxLat = 46.0, maxLon = 7.7),
          ways = listOf(LatLong(45.9, 7.6), LatLong(45.95, 7.65), LatLong(46.0, 7.7)),
          name = "Sample Hike",
          description =
              "A scenic trail with breathtaking views of the Matterhorn and surrounding glaciers.")

  private val detailedRoute =
      DetailedHikeRoute(
          route = route,
          totalDistance = 13.543077559212616,
          elevationGain = 68.0,
          estimatedTime = 169.3169307105514,
          difficulty = HikeDifficulty.DIFFICULT)

  private val profile =
      Profile(
          id = "1",
          name = "John Doe",
          email = "john-doe@gmail.com",
          hikingLevel = HikingLevel.INTERMEDIATE,
          joinedDate = Timestamp.now())

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    mockNavigationActions = mock(NavigationActions::class.java)
    profileRepository = mock(ProfileRepository::class.java)
    profileViewModel = ProfileViewModel(profileRepository)
    authRepository = mock(AuthRepository::class.java)
    authViewModel = AuthViewModel(authRepository, profileRepository)
    hikesRepository = mock(HikeRoutesRepository::class.java)
    elevationService = mock(ElevationService::class.java)
    mockSavedHikesRepository = mock(SavedHikesRepository::class.java)

    savedHikesViewModel = SavedHikesViewModel(mockSavedHikesRepository, UnconfinedTestDispatcher())
    listOfHikeRoutesViewModel =
        ListOfHikeRoutesViewModel(hikesRepository, elevationService, UnconfinedTestDispatcher())

    listOfHikeRoutesViewModel.selectRoute(route)

    `when`(profileRepository.getProfileById(eq(profile.id), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(Profile) -> Unit>(1)
      onSuccess(profile)
    }
    profileViewModel.getProfileById(profile.id)
  }

  @Test
  fun hikeDetailScreen_displaysMap() = runTest {
    listOfHikeRoutesViewModel.selectRoute(route)

    composeTestRule.setContent {
      HikeDetailScreen(
          listOfHikeRoutesViewModel = listOfHikeRoutesViewModel,
          savedHikesViewModel = savedHikesViewModel,
          profileViewModel = profileViewModel,
          authViewModel = authViewModel,
          navigationActions = mockNavigationActions)
    }

    composeTestRule.onNodeWithTag(HIKE_DETAILS_TEST_TAG_MAP).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_displaysHikeNameAndBookmarkIcon() = runTest {
    listOfHikeRoutesViewModel.selectRoute(route)

    composeTestRule.setContent {
      HikeDetails(
          detailedRoute = detailedRoute,
          savedHikesViewModel = savedHikesViewModel,
          emptyList(),
          HikingLevel.BEGINNER,
          {})
    }

    composeTestRule.onNodeWithTag(HIKE_DETAILS_TEST_TAG_HIKE_NAME).assertTextEquals(route.name!!)
    composeTestRule.onNodeWithTag(HIKE_DETAILS_TEST_TAG_BOOKMARK_ICON).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_showsElevationGraph() = runTest {
    listOfHikeRoutesViewModel.selectRoute(route)

    composeTestRule.setContent {
      HikeDetails(
          detailedRoute = detailedRoute,
          savedHikesViewModel = savedHikesViewModel,
          emptyList(),
          HikingLevel.BEGINNER,
          {})
    }

    composeTestRule.onNodeWithTag(HIKE_DETAILS_TEST_TAG_ELEVATION_GRAPH).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_showsPlannedDate_whenDateIsSet() = runTest {
    listOfHikeRoutesViewModel.selectRoute(route)

    // Mock the necessary functions
    whenever(mockSavedHikesRepository.loadSavedHikes()).thenReturn(emptyList())
    whenever(mockSavedHikesRepository.addSavedHike(any())).thenReturn(Unit)
    whenever(mockSavedHikesRepository.removeSavedHike(any())).thenReturn(Unit)

    // Update the hike detail state and toggle save state
    savedHikesViewModel.updateHikeDetailState(route)
    savedHikesViewModel.toggleSaveState()

    // Set the planned date
    val plannedDate = Timestamp(1622563200, 0) // Example timestamp
    savedHikesViewModel.updatePlannedDate(plannedDate)

    composeTestRule.setContent {
      HikeDetails(
          detailedRoute = detailedRoute,
          savedHikesViewModel = savedHikesViewModel,
          emptyList(),
          HikingLevel.BEGINNER,
          {})
    }

    composeTestRule.onNodeWithTag(HIKE_DETAILS_TEST_TAG_PLANNED_DATE_TEXT_BOX).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_showsCorrectDetailsRowsWhenNotSaved() = runTest {
    listOfHikeRoutesViewModel.selectRoute(route)

    composeTestRule.setContent {
      HikeDetails(
          detailedRoute = detailedRoute,
          savedHikesViewModel = savedHikesViewModel,
          emptyList(),
          HikingLevel.BEGINNER,
          {})
    }

    composeTestRule.onAllNodesWithTag(HIKE_DETAILS_TEST_TAG_DETAIL_ROW_TAG).assertCountEquals(5)
    composeTestRule.onAllNodesWithTag(HIKE_DETAILS_TEST_TAG_DETAIL_ROW_VALUE).assertCountEquals(5)
  }

  @Test
  fun hikeDetails_showsCorrectDetailsRowsWhenSavedAndNoDateIsSet() = runTest {
    listOfHikeRoutesViewModel.selectRoute(route)

    // Mock the necessary functions
    whenever(mockSavedHikesRepository.loadSavedHikes()).thenReturn(emptyList())
    whenever(mockSavedHikesRepository.addSavedHike(any())).thenReturn(Unit)
    whenever(mockSavedHikesRepository.removeSavedHike(any())).thenReturn(Unit)

    // Update the hike detail state and toggle save state
    savedHikesViewModel.updateHikeDetailState(route)
    savedHikesViewModel.toggleSaveState()

    composeTestRule.setContent {
      HikeDetails(
          detailedRoute = detailedRoute,
          savedHikesViewModel = savedHikesViewModel,
          emptyList(),
          HikingLevel.BEGINNER,
          {})
    }

    composeTestRule.onAllNodesWithTag(HIKE_DETAILS_TEST_TAG_DETAIL_ROW_TAG).assertCountEquals(6)
    composeTestRule.onAllNodesWithTag(HIKE_DETAILS_TEST_TAG_DETAIL_ROW_VALUE).assertCountEquals(5)
    composeTestRule.onNodeWithTag(HIKE_DETAILS_TEST_TAG_ADD_DATE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_showsCorrectDetailsRowsWhenSavedAndDateIsSet() = runTest {
    listOfHikeRoutesViewModel.selectRoute(route)

    // Mock the necessary functions
    whenever(mockSavedHikesRepository.loadSavedHikes()).thenReturn(emptyList())
    whenever(mockSavedHikesRepository.addSavedHike(any())).thenReturn(Unit)
    whenever(mockSavedHikesRepository.removeSavedHike(any())).thenReturn(Unit)

    // Update the hike detail state and toggle save state
    savedHikesViewModel.updateHikeDetailState(route)
    savedHikesViewModel.toggleSaveState()

    // Set the planned date
    val plannedDate = Timestamp(1622563200, 0) // Example timestamp
    savedHikesViewModel.updatePlannedDate(plannedDate)

    composeTestRule.setContent {
      HikeDetails(
          detailedRoute = detailedRoute,
          savedHikesViewModel = savedHikesViewModel,
          emptyList(),
          HikingLevel.BEGINNER,
          {})
    }

    composeTestRule.onAllNodesWithTag(HIKE_DETAILS_TEST_TAG_DETAIL_ROW_TAG).assertCountEquals(6)
    composeTestRule.onAllNodesWithTag(HIKE_DETAILS_TEST_TAG_DETAIL_ROW_VALUE).assertCountEquals(5)
    composeTestRule.onNodeWithTag(HIKE_DETAILS_TEST_TAG_PLANNED_DATE_TEXT_BOX).assertIsDisplayed()
  }

  @Test
  fun hikeDetailScreen_navigatesBackOnBackButtonClick() = runTest {
    listOfHikeRoutesViewModel.selectRoute(route)

    composeTestRule.setContent {
      HikeDetailScreen(
          listOfHikeRoutesViewModel = listOfHikeRoutesViewModel,
          savedHikesViewModel = savedHikesViewModel,
          profileViewModel = profileViewModel,
          authViewModel = authViewModel,
          navigationActions = mockNavigationActions)
    }

    doNothing().`when`(mockNavigationActions).goBack()

    composeTestRule.onNodeWithTag(BACK_BUTTON_TEST_TAG).performClick()

    composeTestRule.waitForIdle()

    assert(listOfHikeRoutesViewModel.selectedHikeRoute.value == null)
    verify(mockNavigationActions).goBack()
  }

  @Test
  fun hikeDetails_opensDatePicker_whenAddDateButtonClicked() = runTest {
    listOfHikeRoutesViewModel.selectRoute(route)

    // Mock the necessary functions
    whenever(mockSavedHikesRepository.loadSavedHikes()).thenReturn(emptyList())
    whenever(mockSavedHikesRepository.addSavedHike(any())).thenReturn(Unit)
    whenever(mockSavedHikesRepository.removeSavedHike(any())).thenReturn(Unit)

    // Update the hike detail state
    savedHikesViewModel.updateHikeDetailState(route)
    savedHikesViewModel.toggleSaveState()

    composeTestRule.setContent {
      HikeDetails(
          detailedRoute = detailedRoute,
          savedHikesViewModel = savedHikesViewModel,
          emptyList(),
          HikingLevel.BEGINNER,
          {})
    }

    composeTestRule
        .onNodeWithTag(HIKE_DETAILS_TEST_TAG_ADD_DATE_BUTTON)
        .assertHasClickAction()
        .performClick()
    composeTestRule.onNodeWithTag(HIKE_DETAILS_TEST_TAG_DATE_PICKER).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_datePickerDismisses_whenClickedOnCancel() = runTest {
    listOfHikeRoutesViewModel.selectRoute(route)

    // Mock the necessary functions
    whenever(mockSavedHikesRepository.loadSavedHikes()).thenReturn(emptyList())
    whenever(mockSavedHikesRepository.addSavedHike(any())).thenReturn(Unit)
    whenever(mockSavedHikesRepository.removeSavedHike(any())).thenReturn(Unit)

    // Update the hike detail state
    savedHikesViewModel.updateHikeDetailState(route)
    savedHikesViewModel.toggleSaveState()

    composeTestRule.setContent {
      HikeDetails(
          detailedRoute = detailedRoute,
          savedHikesViewModel = savedHikesViewModel,
          emptyList(),
          HikingLevel.BEGINNER,
          {})
    }

    composeTestRule
        .onNodeWithTag(HIKE_DETAILS_TEST_TAG_ADD_DATE_BUTTON)
        .assertHasClickAction()
        .performClick()
    composeTestRule.onNodeWithTag(HIKE_DETAILS_TEST_TAG_DATE_PICKER).assertIsDisplayed()

    composeTestRule.onNodeWithTag(HIKE_DETAILS_TEST_TAG_DATE_PICKER_CANCEL_BUTTON).performClick()

    composeTestRule.onNodeWithTag(HIKE_DETAILS_TEST_TAG_DATE_PICKER).assertIsNotDisplayed()
  }

  @Test
  fun hikeDetails_datePickerDismisses_whenClickedOnConfirm() = runTest {
    listOfHikeRoutesViewModel.selectRoute(route)

    // Mock the necessary functions
    whenever(mockSavedHikesRepository.loadSavedHikes()).thenReturn(emptyList())
    whenever(mockSavedHikesRepository.addSavedHike(any())).thenReturn(Unit)
    whenever(mockSavedHikesRepository.removeSavedHike(any())).thenReturn(Unit)

    // Update the hike detail state
    savedHikesViewModel.updateHikeDetailState(route)
    savedHikesViewModel.toggleSaveState()

    composeTestRule.setContent {
      HikeDetails(
          detailedRoute = detailedRoute,
          savedHikesViewModel = savedHikesViewModel,
          emptyList(),
          HikingLevel.BEGINNER,
          {})
    }

    composeTestRule
        .onNodeWithTag(HIKE_DETAILS_TEST_TAG_ADD_DATE_BUTTON)
        .assertHasClickAction()
        .performClick()
    composeTestRule.onNodeWithTag(HIKE_DETAILS_TEST_TAG_DATE_PICKER).assertIsDisplayed()

    composeTestRule.onNodeWithTag(HIKE_DETAILS_TEST_TAG_DATE_PICKER_CONFIRM_BUTTON).performClick()

    composeTestRule.onNodeWithTag(HIKE_DETAILS_TEST_TAG_DATE_PICKER).assertIsNotDisplayed()
  }

  @Test
  fun hikeDetails_showsCorrectDetailedHikesValues() {
    composeTestRule.setContent {
      HikeDetails(
          detailedRoute = detailedRoute,
          savedHikesViewModel = savedHikesViewModel,
          emptyList(),
          HikingLevel.BEGINNER,
          {})
    }

    val distanceString = String.format(Locale.ENGLISH, "%.2f", detailedRoute.totalDistance)
    val elevationGainString = detailedRoute.elevationGain.roundToInt().toString()
    val hourString =
        String.format(Locale.getDefault(), "%02d", (detailedRoute.estimatedTime / 60).toInt())
    val minuteString =
        String.format(Locale.getDefault(), "%02d", (detailedRoute.estimatedTime % 60).roundToInt())

    composeTestRule
        .onAllNodesWithTag(HIKE_DETAILS_TEST_TAG_DETAIL_ROW_VALUE)
        .assertAny(hasText("${distanceString}km"))
    composeTestRule
        .onAllNodesWithTag(HIKE_DETAILS_TEST_TAG_DETAIL_ROW_VALUE)
        .assertAny(hasText("${elevationGainString}m"))
    composeTestRule
        .onAllNodesWithTag(HIKE_DETAILS_TEST_TAG_DETAIL_ROW_VALUE)
        .assertAny(hasText("${hourString}h${minuteString}"))
    composeTestRule
        .onAllNodesWithTag(HIKE_DETAILS_TEST_TAG_DETAIL_ROW_VALUE)
        .assertAny(
            hasText(
                ApplicationProvider.getApplicationContext<Context>()
                    .getString(detailedRoute.difficulty.nameResourceId)))
  }

  @Test
  fun hikeDetails_showsCorrectDetailedHikesValues_whenTimeIsLessThan60Min() {
    val detailedRoute = detailedRoute.copy(estimatedTime = 45.0) // test hike that takes just 45 min

    composeTestRule.setContent {
      HikeDetails(
          detailedRoute = detailedRoute,
          savedHikesViewModel = savedHikesViewModel,
          emptyList(),
          HikingLevel.BEGINNER,
          {})
    }

    val minuteString =
        String.format(Locale.getDefault(), "%02d", (detailedRoute.estimatedTime % 60).roundToInt())

    composeTestRule
        .onAllNodesWithTag(HIKE_DETAILS_TEST_TAG_DETAIL_ROW_VALUE)
        .assertAny(hasText("${minuteString}min"))
  }

  @Test
  fun hikeDetails_showsRunThisHikeButton_andTriggersOnRunThisHike() {
    val onRunThisHike = mock<() -> Unit>()
    composeTestRule.setContent {
      HikeDetails(
          detailedRoute = detailedRoute,
          savedHikesViewModel = savedHikesViewModel,
          emptyList(),
          HikingLevel.BEGINNER,
          { onRunThisHike() })
    }

    composeTestRule
        .onNodeWithTag(HIKE_DETAILS_TEST_TAG_RUN_HIKE_BUTTON)
        .assertIsDisplayed()
        .performClick()

    verify(onRunThisHike).invoke()
  }
}
