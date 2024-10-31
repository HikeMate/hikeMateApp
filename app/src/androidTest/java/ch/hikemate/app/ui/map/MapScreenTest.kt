package ch.hikemate.app.ui.map

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import ch.hikemate.app.R
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.HikeRoutesRepository
import ch.hikemate.app.model.route.ListOfHikeRoutesViewModel
import ch.hikemate.app.ui.components.HikeCard
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

  private fun setUpMap(
      mapMinZoomLevel: Double = MapScreen.MAP_MIN_ZOOM,
      mapInitialZoomLevel: Double = MapScreen.MAP_INITIAL_ZOOM
  ) {
    composeTestRule.setContent {
      MapScreen(
          hikingRoutesViewModel = listOfHikeRoutesViewModel,
          navigationActions = navigationActions,
          mapMinZoomLevel = mapMinZoomLevel,
          mapInitialZoomLevel = mapInitialZoomLevel)
    }
  }

  @Before
  fun setUp() {
    navigationActions = mock(NavigationActions::class.java)
    hikesRepository = mock(HikeRoutesRepository::class.java)
    listOfHikeRoutesViewModel = ListOfHikeRoutesViewModel(hikesRepository)
  }

  @Test
  fun mapIsDisplayed() {
    setUpMap()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_MAP).assertIsDisplayed()
  }

  @Test
  fun hikesListIsDisplayed() {
    setUpMap()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_HIKES_LIST).assertIsDisplayed()
  }

  @Test
  fun menuButtonIsDisplayed() {
    setUpMap()
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).assertIsDisplayed()
  }

  @Test
  fun searchButtonIsDisplayed() {
    setUpMap()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsDisplayed()
  }

  @Test
  fun searchButtonCallsRepositoryWhenClicked() {
    setUpMap()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()
    Thread.sleep(500)
    verify(hikesRepository, times(1)).getRoutes(any(), any(), any())
  }

  @Test
  fun searchButtonDisappearsWhenClicked() {
    setUpMap()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsNotDisplayed()
  }

  @Test
  fun searchButtonReappearsWhenSearchSucceeded() {
    setUpMap()
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
      onSuccess(listOf())
    }
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsDisplayed()
  }

  @Test
  fun searchButtonReappearsWhenSearchFailed() {
    setUpMap()
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onFailure = it.getArgument<(Exception) -> Unit>(2)
      onFailure(Exception("Test exception"))
    }
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsDisplayed()
  }

  @Test
  fun clickingSearchDisplaysSearchingMessageAndClearsList() {
    setUpMap()
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
    setUpMap()
    val hikeRoute = HikeRoute("Route 1", Bounds(0.0, 0.0, 0.0, 0.0), emptyList())
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
      onSuccess(listOf(hikeRoute))
    }
    // Have an initial list of routes with a single route to display on the map
    listOfHikeRoutesViewModel.setArea(BoundingBox(0.0, 0.0, 0.0, 0.0))

    // Wait for the recomposition to terminate before clicking on the route item
    // The test does not seem to work without this line
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(MapScreen.TEST_TAG_HIKE_ITEM)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click on the route item in the list
    composeTestRule
        .onNodeWithTag(MapScreen.TEST_TAG_HIKE_ITEM, useUnmergedTree = true)
        .performClick()

    // Check that the selected route is the one that was clicked
    assert(listOfHikeRoutesViewModel.selectedHikeRoute.value == hikeRoute)
  }

  @Test
  fun emptyHikesListDisplaysEmptyMessage() {
    setUpMap()
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
      onSuccess(listOf())
    }
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_EMPTY_HIKES_LIST_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun hikesListDisplaysHikeNameIfAvailable() {
    // Given
    val hikeName = "My test hike whatever"
    val hike = HikeRoute("1", Bounds(-1.0, -1.0, 1.0, 1.0), listOf(), hikeName, null)
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
      onSuccess(listOf(hike))
    }

    // When
    setUpMap()
    listOfHikeRoutesViewModel.setArea(BoundingBox(0.0, 0.0, 0.0, 0.0))
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(MapScreen.TEST_TAG_HIKE_ITEM)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Then
    composeTestRule
        .onAllNodesWithTag(HikeCard.TEST_TAG_HIKE_CARD_TITLE, useUnmergedTree = true)
        .assertCountEquals(1)
    composeTestRule
        .onNodeWithTag(HikeCard.TEST_TAG_HIKE_CARD_TITLE, useUnmergedTree = true)
        .assertTextEquals(hikeName)
  }

  @Test
  fun hikesListDisplaysDefaultValueIfNameNotAvailable() {
    // Given
    val hike = HikeRoute("1", Bounds(-1.0, -1.0, 1.0, 1.0), listOf(), null, null)
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
      onSuccess(listOf(hike))
    }
    val context = InstrumentationRegistry.getInstrumentation().targetContext

    // When
    setUpMap()
    listOfHikeRoutesViewModel.setArea(BoundingBox(0.0, 0.0, 0.0, 0.0))
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(MapScreen.TEST_TAG_HIKE_ITEM)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Then
    composeTestRule
        .onAllNodesWithTag(HikeCard.TEST_TAG_HIKE_CARD_TITLE, useUnmergedTree = true)
        .assertCountEquals(1)
    composeTestRule
        .onNodeWithTag(HikeCard.TEST_TAG_HIKE_CARD_TITLE, useUnmergedTree = true)
        .assertTextEquals(context.getString(R.string.map_screen_hike_title_default))
  }

  /**
   * Regression test for the app crashing when searching for hikes while zoomed out too far.
   *
   * See https://github.com/HikeMate/hikeMateApp/issues/78 for more details.
   */
  @Test
  fun zoomedOutMapDoesNotCrashWhenSearchingForHikes() {
    // Initialize the map as zoomed out very far
    setUpMap(mapMinZoomLevel = 1.0, mapInitialZoomLevel = 1.0)

    // Click on the search button
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()

    // Check that the repository is called with valid bounds
    verify(hikesRepository, times(1)).getRoutes(any(), any(), any())
  }
}
