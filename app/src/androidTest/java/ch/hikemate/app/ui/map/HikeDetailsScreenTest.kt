package ch.hikemate.app.ui.map

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.hikemate.app.model.elevation.ElevationService
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.HikeRoutesRepository
import ch.hikemate.app.model.route.LatLong
import ch.hikemate.app.model.route.ListOfHikeRoutesViewModel
import ch.hikemate.app.model.route.saved.SavedHikesRepository
import ch.hikemate.app.model.route.saved.SavedHikesViewModel
import ch.hikemate.app.ui.components.BackButton.BACK_BUTTON_TEST_TAG
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_ADD_DATE_BUTTON
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_BOOKMARK_ICON
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_DETAIL_ROW_TAG
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_DETAIL_ROW_VALUE
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_ELEVATION_GRAPH
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_HIKE_NAME
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_MAP
import ch.hikemate.app.ui.map.HikeDetailScreen.TEST_TAG_PLANNED_DATE_TEXT_BOX
import ch.hikemate.app.ui.navigation.NavigationActions
import com.google.firebase.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class HikeDetailScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockNavigationActions: NavigationActions
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

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    mockNavigationActions = mock(NavigationActions::class.java)
    hikesRepository = mock(HikeRoutesRepository::class.java)
    elevationService = mock(ElevationService::class.java)
    mockSavedHikesRepository = mock(SavedHikesRepository::class.java)

    listOfHikeRoutesViewModel =
        ListOfHikeRoutesViewModel(hikesRepository, elevationService, UnconfinedTestDispatcher())
    savedHikesViewModel = SavedHikesViewModel(mockSavedHikesRepository)

    listOfHikeRoutesViewModel.selectRoute(route)
  }

  @Test
  fun hikeDetailScreen_displaysMap() {
    savedHikesViewModel.updateHikeDetailState(route)
    composeTestRule.setContent {
      HikeDetailScreen(
          listOfHikeRoutesViewModel = listOfHikeRoutesViewModel,
          savedHikesViewModel = savedHikesViewModel,
          navigationActions = mockNavigationActions)
    }

    composeTestRule.onNodeWithTag(TEST_TAG_MAP).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_displaysHikeNameAndBookmarkIcon() {
    savedHikesViewModel.updateHikeDetailState(route)
    composeTestRule.setContent { HikeDetails(route = route, savedHikesViewModel) }

    composeTestRule.onNodeWithTag(TEST_TAG_HIKE_NAME).assertTextEquals(route.name!!)
    composeTestRule.onNodeWithTag(TEST_TAG_BOOKMARK_ICON).assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun hikeDetails_showsElevationGraph() {
    savedHikesViewModel.updateHikeDetailState(route)
    composeTestRule.setContent { HikeDetails(route = route, savedHikesViewModel) }

    composeTestRule.onNodeWithTag(TEST_TAG_ELEVATION_GRAPH).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_displaysPlannedDateButton_whenDateNotSet() {
    savedHikesViewModel.updateHikeDetailState(route)
    composeTestRule.setContent { HikeDetails(route = route, savedHikesViewModel) }

    composeTestRule.onNodeWithTag(TEST_TAG_ADD_DATE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_showsPlannedDate_whenDateIsSet() {
    savedHikesViewModel.updateHikeDetailState(route)
    savedHikesViewModel.updatePlannedDate(Timestamp(1622563200, 0)) // Example timestamp

    composeTestRule.setContent { HikeDetails(route = route, savedHikesViewModel) }

    composeTestRule.onNodeWithTag(TEST_TAG_PLANNED_DATE_TEXT_BOX).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_showsCorrectDetailsRowsWhenNotSaved() {
    savedHikesViewModel.updateHikeDetailState(route)
    composeTestRule.setContent { HikeDetails(route = route, savedHikesViewModel) }

    composeTestRule.onAllNodesWithTag(TEST_TAG_DETAIL_ROW_TAG).assertCountEquals(5)
    composeTestRule.onAllNodesWithTag(TEST_TAG_DETAIL_ROW_VALUE).assertCountEquals(5)
  }

  @Test
  fun hikeDetails_showsCorrectDetailsRowsWhenSavedAndNoDateIsSet() {
    savedHikesViewModel.updateHikeDetailState(route)
    savedHikesViewModel.toggleSaveState()
    composeTestRule.setContent { HikeDetails(route = route, savedHikesViewModel) }

    composeTestRule.onAllNodesWithTag(TEST_TAG_DETAIL_ROW_TAG).assertCountEquals(6)
    composeTestRule.onAllNodesWithTag(TEST_TAG_DETAIL_ROW_VALUE).assertCountEquals(5)
    composeTestRule.onNodeWithTag(TEST_TAG_ADD_DATE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_showsCorrectDetailsRowsWhenSavedAndDateIsSet() {
    savedHikesViewModel.updateHikeDetailState(route)
    savedHikesViewModel.toggleSaveState()
    savedHikesViewModel.updatePlannedDate(Timestamp.now())

    composeTestRule.setContent { HikeDetails(route = route, savedHikesViewModel) }

    composeTestRule.onAllNodesWithTag(TEST_TAG_DETAIL_ROW_TAG).assertCountEquals(6)
    composeTestRule.onAllNodesWithTag(TEST_TAG_DETAIL_ROW_VALUE).assertCountEquals(5)
    composeTestRule.onNodeWithTag(TEST_TAG_PLANNED_DATE_TEXT_BOX).assertIsDisplayed()
  }

  @Test
  fun hikeDetailScreen_navigatesBackOnBackButtonClick() {
    savedHikesViewModel.updateHikeDetailState(route)
    composeTestRule.setContent {
      HikeDetailScreen(
          listOfHikeRoutesViewModel = listOfHikeRoutesViewModel,
          savedHikesViewModel = savedHikesViewModel,
          navigationActions = mockNavigationActions)
    }

    doNothing().`when`(mockNavigationActions).goBack()
    composeTestRule.onNodeWithTag(BACK_BUTTON_TEST_TAG).performClick()
    verify(mockNavigationActions).goBack()
  }

  @Test
  fun hikeDetails_opensDatePicker_whenAddDateButtonClicked() {
    savedHikesViewModel.updateHikeDetailState(route)

    if (!savedHikesViewModel.hikeDetailState.value!!.isSaved) savedHikesViewModel.toggleSaveState()

    composeTestRule.setContent { HikeDetails(route = route, savedHikesViewModel) }

    composeTestRule.onNodeWithTag(TEST_TAG_ADD_DATE_BUTTON).assertHasClickAction().performClick()
  }
}
