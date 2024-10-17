package ch.hikemate.app.ui.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.HikeRoutesRepository
import ch.hikemate.app.model.route.ListOfHikeRoutesViewModel
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.TEST_TAG_SIDEBAR_BUTTON
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.osmdroid.util.BoundingBox

class MapScreenTest : TestCase() {
  private lateinit var hikesRepository: HikeRoutesRepository
  private lateinit var listOfHikeRoutesViewModel: ListOfHikeRoutesViewModel
  private lateinit var navigationActions: NavigationActions

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    navigationActions = mock(NavigationActions::class.java)
    hikesRepository = mock(HikeRoutesRepository::class.java)
    listOfHikeRoutesViewModel = ListOfHikeRoutesViewModel(hikesRepository)
    composeTestRule.setContent {
      MapScreen(
          hikingRoutesViewModel = listOfHikeRoutesViewModel, navigationActions = navigationActions)
    }
  }

  @Test
  fun mapIsDisplayed() {
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_MAP).assertIsDisplayed()
  }

  @Test
  fun hikesListIsDisplayed() {
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_HIKES_LIST).assertIsDisplayed()
  }

  @Test
  fun menuButtonIsDisplayed() {
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).assertIsDisplayed()
  }

  @Test
  fun searchButtonIsDisplayed() {
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsDisplayed()
  }

  @Test
  fun searchButtonCallsRepositoryWhenClicked() {
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()
    verify(hikesRepository, times(1)).getRoutes(any(), any(), any())
  }

  @Test
  fun searchButtonDisappearsWhenClicked() {
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsNotDisplayed()
  }

  @Test
  fun searchButtonReappearsWhenSearchSucceeded() {
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
      onSuccess(listOf())
    }
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsDisplayed()
  }

  @Test
  fun searchButtonReappearsWhenSearchFailed() {
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onFailure = it.getArgument<(Exception) -> Unit>(2)
      onFailure(Exception("Test exception"))
    }
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsDisplayed()
  }

  @Test
  fun clickingSearchDisplaysSearchingMessageAndClearsList() {
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCHING_MESSAGE).assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(MapScreen.TEST_TAG_SEARCH_LOADING_ANIMATION)
        .assertIsNotDisplayed()

    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()

    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCHING_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_LOADING_ANIMATION).assertIsDisplayed()
  }

  @Test
  fun clickingOnHikeItemSelectsHikeRoute() {
    val hikeRoute = HikeRoute("Route 1", Bounds(0.0, 0.0, 0.0, 0.0), emptyList())
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
      onSuccess(listOf(hikeRoute))
    }
    // Have an initial list of routes with a single route to display on the map
    listOfHikeRoutesViewModel.setArea(BoundingBox(0.0, 0.0, 0.0, 0.0))

    // Wait for the recomposition to terminate before clicking on the route item
    // The test does not seem to work without this line
    composeTestRule.waitForIdle()

    // Click on the route item in the list
    composeTestRule
        .onNodeWithTag(MapScreen.TEST_TAG_HIKE_ITEM, useUnmergedTree = true)
        .performClick()

    // Check that the selected route is the one that was clicked
    assert(listOfHikeRoutesViewModel.selectedHikeRoute.value == hikeRoute)
  }

  @Test
  fun emptyHikesListDisplaysEmptyMessage() {
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
      onSuccess(listOf())
    }
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_EMPTY_HIKES_LIST_MESSAGE).assertIsDisplayed()
  }
}
