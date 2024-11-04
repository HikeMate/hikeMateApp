package ch.hikemate.app.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.hikemate.app.HikeMateApp
import ch.hikemate.app.ui.auth.TEST_TAG_LOGIN_BUTTON
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.navigation.TEST_TAG_DRAWER_ITEM_PREFIX
import ch.hikemate.app.ui.navigation.TEST_TAG_SIDEBAR_BUTTON
import ch.hikemate.app.ui.saved.TEST_TAG_SAVED_HIKES_SECTION_CONTAINER
import ch.hikemate.app.ui.theme.HikeMateTheme
import org.junit.Rule

// @RunWith(AndroidJUnit4::class)
class HikeMateAppNavigationTest {
  // Set up the Compose test rule
  @get:Rule val composeTestRule = createComposeRule()

  // @Test
  fun testInitialScreenIsAuthScreen() {
    composeTestRule.setContent { HikeMateTheme { HikeMateApp() } }

    composeTestRule.onNodeWithTag(Screen.AUTH).assertIsDisplayed()
  }

  // @Test
  fun testNavigationToMapScreen() {
    composeTestRule.setContent { HikeMateApp() }

    composeTestRule.onNodeWithTag(Screen.AUTH).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_LOGIN_BUTTON).performClick()
    composeTestRule.onNodeWithTag(Screen.MAP).assertIsDisplayed()
  }

  // @Test
  fun testNavigationToPlannedHikesScreen() {
    composeTestRule.setContent { HikeMateApp() }
    composeTestRule.onNodeWithTag(Screen.AUTH).assertIsDisplayed()

    composeTestRule.onNodeWithTag(TEST_TAG_LOGIN_BUTTON).performClick()
    composeTestRule.onNodeWithTag(Screen.MAP).assertIsDisplayed()

    // Open the sidebar
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()

    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + Route.SAVED_HIKES).performClick()

    composeTestRule.onNodeWithTag(TEST_TAG_SAVED_HIKES_SECTION_CONTAINER).assertIsDisplayed()
  }

  // @Test
  fun testNavigationToProfileScreen() {
    composeTestRule.setContent { HikeMateApp() }
    composeTestRule.onNodeWithTag(Screen.AUTH).assertIsDisplayed()

    composeTestRule.onNodeWithTag(TEST_TAG_LOGIN_BUTTON).performClick()
    composeTestRule.onNodeWithTag(Screen.MAP).assertIsDisplayed()
    // Open the sidebar
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + Route.PROFILE).performClick()
    composeTestRule.onNodeWithTag(Screen.PROFILE).assertIsDisplayed()
  }
}
