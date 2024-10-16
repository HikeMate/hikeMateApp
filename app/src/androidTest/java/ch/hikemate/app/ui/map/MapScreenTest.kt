package ch.hikemate.app.ui.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.TEST_TAG_SIDEBAR_BUTTON
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

class MapScreenTest : TestCase() {
  @get:Rule val composeTestRule = createComposeRule()
  lateinit var navigationActions: NavigationActions

  @Before
  fun setUp() {
    navigationActions = mock(NavigationActions::class.java)
    composeTestRule.setContent { MapScreen(navigationActions = navigationActions) }
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
  fun zoomingOnMapCallsViewModel() {
    // TODO : Implement this test once the view model uses an actual repository that is mockable
  }

  @Test
  fun scrollingOnMapCallsViewModel() {
    // TODO : Implement this test once the view model uses an actual repository that is mockable
  }

  @Test
  fun emptyHikesListDisplaysEmptyMessage() {
    // TODO : Implement this test once the view model uses an actual repository that is mockable
  }

  @Test
  fun clickingMenuButtonOpensTheMenu() {
    // TODO : Implement this test once the navigation has been integrated to the map screen
  }

  @Test
  fun clickingOnHikeOpensHikeDetails() {
    // TODO : Implement this test once the hike details screen has been implemented
  }
}
