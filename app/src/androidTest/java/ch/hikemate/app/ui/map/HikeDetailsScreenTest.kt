package ch.hikemate.app.ui.map

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.HikeRoutesRepository
import ch.hikemate.app.model.route.LatLong
import ch.hikemate.app.model.route.ListOfHikeRoutesViewModel
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
  private lateinit var hikesRepository: HikeRoutesRepository
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
    mockNavigationActions = mock()
    hikesRepository = mock()

    listOfHikeRoutesViewModel =
        ListOfHikeRoutesViewModel(hikesRepository, UnconfinedTestDispatcher())

    // Set up the VM
    listOfHikeRoutesViewModel.selectRoute(route)
  }

  @Test
  fun hikeDetailScreen_displaysMap() {
    composeTestRule.setContent {
      HikeDetailScreen(
          listOfHikeRoutesViewModel = listOfHikeRoutesViewModel,
          navigationActions = mockNavigationActions)
    }

    composeTestRule.onNodeWithTag(TEST_TAG_MAP).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_displaysHikeNameAndBookmarkIcon() {
    composeTestRule.setContent { HikeDetails(route = route, isSaved = true, date = null) }

    composeTestRule.onNodeWithTag(TEST_TAG_HIKE_NAME).assertTextEquals(route.name!!)
    composeTestRule.onNodeWithTag(TEST_TAG_BOOKMARK_ICON).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_showsElevationGraph() {
    composeTestRule.setContent { HikeDetails(route = route, isSaved = true, date = null) }

    // Check if the elevation graph is displayed
    composeTestRule.onNodeWithTag(TEST_TAG_ELEVATION_GRAPH).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_displaysPlannedDateButton_whenDateNotSet() {
    composeTestRule.setContent { HikeDetails(route = route, isSaved = true, date = null) }

    // Check if the "Add a date" button is displayed
    composeTestRule.onNodeWithTag(TEST_TAG_ADD_DATE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_showsPlannedDate_whenDateIsSet() {
    val plannedDate = Timestamp(1622563200, 0) // Example timestamp

    composeTestRule.setContent { HikeDetails(route = route, isSaved = true, date = plannedDate) }

    // Check if the planned date text box is displayed
    composeTestRule.onNodeWithTag(TEST_TAG_PLANNED_DATE_TEXT_BOX).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_showsCorrectDetailsRowsWhenNotSaved() {
    composeTestRule.setContent { HikeDetails(route = route, isSaved = false, date = null) }

    // Check if the detail rows are displayed correctly
    composeTestRule.onAllNodesWithTag(TEST_TAG_DETAIL_ROW_TAG).assertCountEquals(5)
    composeTestRule.onAllNodesWithTag(TEST_TAG_DETAIL_ROW_VALUE).assertCountEquals(5)
  }

  @Test
  fun hikeDetails_showsCorrectDetailsRowsWhenSavedAndNoDateIsSet() {
    composeTestRule.setContent { HikeDetails(route = route, isSaved = true, date = null) }

    // Check if the detail rows are displayed correctly
    composeTestRule.onAllNodesWithTag(TEST_TAG_DETAIL_ROW_TAG).assertCountEquals(6)
    composeTestRule.onAllNodesWithTag(TEST_TAG_DETAIL_ROW_VALUE).assertCountEquals(5)
    composeTestRule.onNodeWithTag(TEST_TAG_ADD_DATE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun hikeDetails_showsCorrectDetailsRowsWhenSavedAndDateIsSet() {
    composeTestRule.setContent {
      HikeDetails(route = route, isSaved = true, date = Timestamp.now())
    }

    // Check if the detail rows are displayed correctly
    composeTestRule.onAllNodesWithTag(TEST_TAG_DETAIL_ROW_TAG).assertCountEquals(6)
    composeTestRule.onAllNodesWithTag(TEST_TAG_DETAIL_ROW_VALUE).assertCountEquals(5)
    composeTestRule.onNodeWithTag(TEST_TAG_PLANNED_DATE_TEXT_BOX).assertIsDisplayed()
  }

  @Test
  fun hikeDetailScreen_navigatesBackOnBackButtonClick() {
    composeTestRule.setContent {
      HikeDetailScreen(
          listOfHikeRoutesViewModel = listOfHikeRoutesViewModel,
          navigationActions = mockNavigationActions)
    }

    doNothing().`when`(mockNavigationActions).goBack()

    // Click the back button and verify the navigation action is triggered
    composeTestRule.onNodeWithTag(BACK_BUTTON_TEST_TAG).performClick()
    verify(mockNavigationActions).goBack()
  }

  @Test
  fun hikeDetails_opensDatePicker_whenAddDateButtonClicked() {
    composeTestRule.setContent { HikeDetails(route = route, isSaved = true, date = null) }

    // Check if the add date button is clickable and triggers an interaction
    composeTestRule.onNodeWithTag(TEST_TAG_ADD_DATE_BUTTON).assertHasClickAction().performClick()
  }
}
