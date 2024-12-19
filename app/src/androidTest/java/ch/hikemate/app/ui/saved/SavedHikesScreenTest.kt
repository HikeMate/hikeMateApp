package ch.hikemate.app.ui.saved

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.hikemate.app.model.elevation.ElevationRepository
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
import ch.hikemate.app.ui.components.CenteredErrorAction
import ch.hikemate.app.ui.components.HikeCard
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.TEST_TAG_BOTTOM_BAR
import com.google.firebase.Timestamp
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class SavedHikesScreenTest : TestCase() {
  private lateinit var savedHikesRepository: SavedHikesRepository
  private lateinit var osmHikesRepository: HikeRoutesRepository
  private lateinit var elevationRepository: ElevationRepository
  private lateinit var hikesViewModel: HikesViewModel
  private lateinit var navigationActions: NavigationActions

  @get:Rule val composeTestRule = createComposeRule()

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    navigationActions = mock(NavigationActions::class.java)
    savedHikesRepository = mock(SavedHikesRepository::class.java)
    osmHikesRepository = mock(HikeRoutesRepository::class.java)
    elevationRepository = mock(ElevationRepository::class.java)
    hikesViewModel =
        HikesViewModel(
            savedHikesRepository,
            osmHikesRepository,
            elevationRepository,
            UnconfinedTestDispatcher())
  }

  private val hikeId = "1"
  private val detailedHike =
      DetailedHike(
          id = hikeId,
          color = Hike(hikeId, false, null, null).getColor(),
          isSaved = false,
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

  private suspend fun setupSavedHikes(hikes: List<DetailedHike>) {
    `when`(savedHikesRepository.loadSavedHikes())
        .thenReturn(hikes.map { SavedHike(it.id, it.name ?: "", it.plannedDate) })
    `when`(osmHikesRepository.getRoutesByIds(any(), any(), any())).thenAnswer {
      val ids = it.getArgument<List<String>>(0)
      val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
      onSuccess(
          hikes
              .filter { detailed -> ids.contains(detailed.id) }
              .map { detailed ->
                HikeRoute(
                    detailed.id,
                    detailed.bounds,
                    detailed.waypoints,
                    detailed.name,
                    detailed.description)
              })
    }
    `when`(elevationRepository.getElevation(any(), any(), any())).thenAnswer {
      val waypoints = it.getArgument<List<LatLong>>(0)
      val onSuccess = it.getArgument<(List<Double>) -> Unit>(1)
      onSuccess(waypoints.map { 0.0 })
    }
  }

  @Test
  fun displaysErrorIfLoadingSavedHikesFails() = runTest {
    // Ensure the loading of saved hikes fails
    `when`(savedHikesRepository.loadSavedHikes())
        .thenThrow(RuntimeException("Failed to load saved hikes"))

    // Set up the screen
    composeTestRule.setContent { SavedHikesScreen(hikesViewModel, navigationActions) }

    // Check that the repository was called once
    verify(savedHikesRepository, times(1)).loadSavedHikes()

    // Verify that the error message is displayed
    composeTestRule
        .onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_MESSAGE)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_BUTTON)
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()

    // Check that the repository was called again, because the button is a retry action
    verify(savedHikesRepository, times(2)).loadSavedHikes()
  }

  @Test
  fun displaysErrorIfLoadingOsmDataFails() = runTest {
    // Ensure loading the saved hikes from the saved hikes repo will succeed
    val hikes =
        listOf(
            detailedHike.copy(
                id = "1", name = "Hike 1", plannedDate = Timestamp.now(), isSaved = true),
            detailedHike.copy(
                id = "2", name = "Hike 2", plannedDate = Timestamp.now(), isSaved = true),
            detailedHike.copy(id = "3", name = "Hike 3", plannedDate = null, isSaved = true))
    setupSavedHikes(hikes)

    // Ensure loading the OSM data fails
    `when`(osmHikesRepository.getRoutesByIds(any(), any(), any()))
        .thenThrow(RuntimeException("Failed to load OSM data"))

    // Set up the screen
    composeTestRule.setContent { SavedHikesScreen(hikesViewModel, navigationActions) }

    // Check that the repository was called once
    verify(savedHikesRepository, times(1)).loadSavedHikes()

    // Verify that the error message is displayed
    composeTestRule
        .onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_MESSAGE)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_BUTTON)
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()

    // Check that the repository was called again, because the button is a retry action
    verify(savedHikesRepository, times(2)).loadSavedHikes()
  }

  @Test
  fun bottomBarMenuIsDisplayed() = runTest {
    setupSavedHikes(emptyList())
    composeTestRule.setContent { SavedHikesScreen(hikesViewModel, navigationActions) }

    composeTestRule.onNodeWithTag(TEST_TAG_BOTTOM_BAR).assertIsDisplayed()
  }

  @Test
  fun bottomMenuIsDisplayedAndHasItems() = runTest {
    setupSavedHikes(emptyList())
    composeTestRule.setContent { SavedHikesScreen(hikesViewModel, navigationActions) }

    composeTestRule
        .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_TABS_MENU)
        .assertIsDisplayed()
    for (element in SavedHikesSection.values()) {
      composeTestRule
          .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_TABS_MENU_ITEM_PREFIX + element.name)
          .assertIsDisplayed()
    }
  }

  @Test
  fun sectionContainerIsDisplayed() = runTest {
    setupSavedHikes(emptyList())
    composeTestRule.setContent { SavedHikesScreen(hikesViewModel, navigationActions) }

    composeTestRule
        .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_SECTION_CONTAINER)
        .assertIsDisplayed()
  }

  @Test
  fun plannedScreenWithNoHikesDisplaysCorrectly() = runTest {
    setupSavedHikes(emptyList())
    composeTestRule.setContent { SavedHikesScreen(hikesViewModel, navigationActions) }

    // Select the planned hikes tab
    composeTestRule
        .onNodeWithTag(
            SavedHikesScreen.TEST_TAG_SAVED_HIKES_TABS_MENU_ITEM_PREFIX +
                SavedHikesSection.Planned.name)
        .performClick()

    composeTestRule
        .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_SAVED_TITLE)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_SAVED_EMPTY_MESSAGE)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_PLANNED_TITLE)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_PLANNED_EMPTY_MESSAGE)
        .assertIsDisplayed()
  }

  @Test
  fun plannedScreenWithHikesDisplaysCorrectly() =
      runTest(timeout = 5.seconds) {
        val hikes =
            listOf(
                detailedHike.copy(
                    id = "1", name = "Hike 1", plannedDate = Timestamp.now(), isSaved = true),
                detailedHike.copy(
                    id = "2", name = "Hike 2", plannedDate = Timestamp.now(), isSaved = true),
                detailedHike.copy(id = "3", name = "Hike 3", plannedDate = null, isSaved = true))
        setupSavedHikes(hikes)

        composeTestRule.setContent { SavedHikesScreen(hikesViewModel, navigationActions) }

        // Select the planned hikes tab
        composeTestRule
            .onNodeWithTag(
                SavedHikesScreen.TEST_TAG_SAVED_HIKES_TABS_MENU_ITEM_PREFIX +
                    SavedHikesSection.Planned.name)
            .performClick()

        composeTestRule
            .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_SAVED_TITLE)
            .assertIsNotDisplayed()
        composeTestRule
            .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_SAVED_EMPTY_MESSAGE)
            .assertIsNotDisplayed()
        composeTestRule
            .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_PLANNED_TITLE)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_PLANNED_EMPTY_MESSAGE)
            .assertIsNotDisplayed()
        composeTestRule
            .onAllNodesWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_HIKE_CARD)
            .assertCountEquals(2)
      }

  @Test
  fun savedScreenWithNoHikesDisplaysCorrectly() = runTest {
    setupSavedHikes(emptyList())
    composeTestRule.setContent { SavedHikesScreen(hikesViewModel, navigationActions) }

    composeTestRule
        .onNodeWithTag(
            SavedHikesScreen.TEST_TAG_SAVED_HIKES_TABS_MENU_ITEM_PREFIX +
                SavedHikesSection.Saved.name)
        .performClick()

    composeTestRule
        .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_PLANNED_TITLE)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_PLANNED_EMPTY_MESSAGE)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_SAVED_TITLE)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_SAVED_EMPTY_MESSAGE)
        .assertIsDisplayed()
  }

  @Test
  fun savedScreenWithHikesDisplaysCorrectly() =
      runTest(timeout = 5.seconds) {
        val hikes =
            listOf(
                detailedHike.copy(id = "1", name = "Hike 1", plannedDate = null, isSaved = true),
                detailedHike.copy(id = "2", name = "Hike 2", plannedDate = null, isSaved = true),
                detailedHike.copy(
                    id = "3", name = "Hike 3", plannedDate = Timestamp.now(), isSaved = true))
        setupSavedHikes(hikes)

        composeTestRule.setContent { SavedHikesScreen(hikesViewModel, navigationActions) }

        composeTestRule
            .onNodeWithTag(
                SavedHikesScreen.TEST_TAG_SAVED_HIKES_TABS_MENU_ITEM_PREFIX +
                    SavedHikesSection.Saved.name)
            .performClick()

        composeTestRule
            .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_PLANNED_TITLE)
            .assertIsNotDisplayed()
        composeTestRule
            .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_PLANNED_EMPTY_MESSAGE)
            .assertIsNotDisplayed()
        composeTestRule
            .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_SAVED_TITLE)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_SAVED_EMPTY_MESSAGE)
            .assertIsNotDisplayed()
        composeTestRule
            .onAllNodesWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_HIKE_CARD)
            .assertCountEquals(3)
      }

  @Test
  fun savedHikeThatIsPlannedIsStillInSavedHikesScreen() =
      runTest(timeout = 5.seconds) {
        val hikes =
            listOf(
                detailedHike.copy(id = "1", name = "Hike 1", plannedDate = null, isSaved = true),
                detailedHike.copy(id = "2", name = "Hike 2", plannedDate = null, isSaved = true))
        setupSavedHikes(hikes)

        composeTestRule.setContent { SavedHikesScreen(hikesViewModel, navigationActions) }

        // Select the saved hikes tab
        composeTestRule
            .onNodeWithTag(
                SavedHikesScreen.TEST_TAG_SAVED_HIKES_TABS_MENU_ITEM_PREFIX +
                    SavedHikesSection.Saved.name)
            .performClick()

        val hikes2 =
            listOf(
                detailedHike.copy(
                    id = "1", name = "Hike 1", plannedDate = Timestamp.now(), isSaved = true),
                detailedHike.copy(id = "2", name = "Hike 2", plannedDate = null, isSaved = true))
        setupSavedHikes(hikes2)

        hikesViewModel.refreshSavedHikesCache()

        // Verify that the hike is still displayed in the saved hikes screen
        composeTestRule
            .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_SAVED_TITLE)
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_HIKE_CARD)
            .assertCountEquals(2)
      }

  @Test
  fun plannedHikesSectionDisplaysDates() =
      runTest(timeout = 5.seconds) {
        val hikes =
            listOf(
                detailedHike.copy(
                    id = "1", name = "Hike 1", plannedDate = Timestamp.now(), isSaved = true),
                detailedHike.copy(
                    id = "2", name = "Hike 2", plannedDate = Timestamp.now(), isSaved = true))
        setupSavedHikes(hikes)

        composeTestRule.setContent { SavedHikesScreen(hikesViewModel, navigationActions) }

        // Select the planned hikes tab
        composeTestRule
            .onNodeWithTag(
                SavedHikesScreen.TEST_TAG_SAVED_HIKES_TABS_MENU_ITEM_PREFIX +
                    SavedHikesSection.Planned.name)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onAllNodesWithTag(HikeCard.TEST_TAG_IS_SUITABLE_TEXT, useUnmergedTree = true)
            .assertCountEquals(2)
      }
}
