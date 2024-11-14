package ch.hikemate.app.ui.saved

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.hikemate.app.model.route.saved.SavedHike
import ch.hikemate.app.model.route.saved.SavedHikesRepository
import ch.hikemate.app.model.route.saved.SavedHikesViewModel
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.TEST_TAG_BOTTOM_BAR
import com.google.firebase.Timestamp
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class SavedHikesScreenTest : TestCase() {
  private lateinit var savedHikesRepository: SavedHikesRepository
  private lateinit var savedHikesViewModel: SavedHikesViewModel
  private lateinit var navigationActions: NavigationActions

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    navigationActions = mock(NavigationActions::class.java)
    savedHikesRepository = mock(SavedHikesRepository::class.java)
    savedHikesViewModel = SavedHikesViewModel(savedHikesRepository)
  }

  @Test
  fun bottomBarMenuIsDisplayed() {
    composeTestRule.setContent { SavedHikesScreen(savedHikesViewModel, navigationActions) }

    composeTestRule.onNodeWithTag(TEST_TAG_BOTTOM_BAR).assertIsDisplayed()
  }

  @Test
  fun bottomMenuIsDisplayedAndHasItems() {
    composeTestRule.setContent { SavedHikesScreen(savedHikesViewModel, navigationActions) }

    composeTestRule.onNodeWithTag(TEST_TAG_SAVED_HIKES_TABS_MENU).assertIsDisplayed()
    for (element in SavedHikesScreen.values()) {
      composeTestRule
          .onNodeWithTag(TEST_TAG_SAVED_HIKES_TABS_MENU_ITEM_PREFIX + element.name)
          .assertIsDisplayed()
    }
  }

  @Test
  fun sectionContainerIsDisplayed() {
    composeTestRule.setContent { SavedHikesScreen(savedHikesViewModel, navigationActions) }

    composeTestRule.onNodeWithTag(TEST_TAG_SAVED_HIKES_SECTION_CONTAINER).assertIsDisplayed()
  }

  @Test
  fun plannedScreenWithNoHikesDisplaysCorrectly() {
    composeTestRule.setContent { SavedHikesScreen(savedHikesViewModel, navigationActions) }

    // Select the planned hikes tab
    composeTestRule
        .onNodeWithTag(TEST_TAG_SAVED_HIKES_TABS_MENU_ITEM_PREFIX + SavedHikesScreen.Planned.name)
        .performClick()

    composeTestRule.onNodeWithTag(TEST_TAG_SAVED_HIKES_SAVED_TITLE).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_SAVED_HIKES_SAVED_EMPTY_MESSAGE).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_SAVED_HIKES_PLANNED_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_SAVED_HIKES_PLANNED_EMPTY_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun plannedScreenWithHikesDisplaysCorrectly() =
      runTest(timeout = 5.seconds) {
        val savedHikes =
            listOf(
                SavedHike(id = "1", name = "Hike 1", date = Timestamp.now()),
                SavedHike(id = "2", name = "Hike 2", date = Timestamp.now()),
                SavedHike(id = "3", name = "Hike 3", date = null))
        `when`(savedHikesRepository.loadSavedHikes()).thenReturn(savedHikes)

        composeTestRule.setContent { SavedHikesScreen(savedHikesViewModel, navigationActions) }

        // Select the planned hikes tab
        composeTestRule
            .onNodeWithTag(
                TEST_TAG_SAVED_HIKES_TABS_MENU_ITEM_PREFIX + SavedHikesScreen.Planned.name)
            .performClick()

        composeTestRule.onNodeWithTag(TEST_TAG_SAVED_HIKES_SAVED_TITLE).assertIsNotDisplayed()
        composeTestRule
            .onNodeWithTag(TEST_TAG_SAVED_HIKES_SAVED_EMPTY_MESSAGE)
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_SAVED_HIKES_PLANNED_TITLE).assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(TEST_TAG_SAVED_HIKES_PLANNED_EMPTY_MESSAGE)
            .assertIsNotDisplayed()
        composeTestRule.onAllNodesWithTag(TEST_TAG_SAVED_HIKES_HIKE_CARD).assertCountEquals(2)
      }

  @Test
  fun savedScreenWithNoHikesDisplaysCorrectly() {
    composeTestRule.setContent { SavedHikesScreen(savedHikesViewModel, navigationActions) }

    composeTestRule
        .onNodeWithTag(TEST_TAG_SAVED_HIKES_TABS_MENU_ITEM_PREFIX + SavedHikesScreen.Saved.name)
        .performClick()

    composeTestRule.onNodeWithTag(TEST_TAG_SAVED_HIKES_PLANNED_TITLE).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_SAVED_HIKES_PLANNED_EMPTY_MESSAGE).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_SAVED_HIKES_SAVED_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_SAVED_HIKES_SAVED_EMPTY_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun savedScreenWithHikesDisplaysCorrectly() =
      runTest(timeout = 5.seconds) {
        val savedHikes =
            listOf(
                SavedHike(id = "1", name = "Hike 1", date = null),
                SavedHike(id = "2", name = "Hike 2", date = null),
                SavedHike(id = "3", name = "Hike 3", date = Timestamp.now()))
        `when`(savedHikesRepository.loadSavedHikes()).thenReturn(savedHikes)

        composeTestRule.setContent { SavedHikesScreen(savedHikesViewModel, navigationActions) }

        composeTestRule
            .onNodeWithTag(TEST_TAG_SAVED_HIKES_TABS_MENU_ITEM_PREFIX + SavedHikesScreen.Saved.name)
            .performClick()

        composeTestRule.onNodeWithTag(TEST_TAG_SAVED_HIKES_PLANNED_TITLE).assertIsNotDisplayed()
        composeTestRule
            .onNodeWithTag(TEST_TAG_SAVED_HIKES_PLANNED_EMPTY_MESSAGE)
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_SAVED_HIKES_SAVED_TITLE).assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(TEST_TAG_SAVED_HIKES_SAVED_EMPTY_MESSAGE)
            .assertIsNotDisplayed()
        composeTestRule.onAllNodesWithTag(TEST_TAG_SAVED_HIKES_HIKE_CARD).assertCountEquals(2)
      }
}
