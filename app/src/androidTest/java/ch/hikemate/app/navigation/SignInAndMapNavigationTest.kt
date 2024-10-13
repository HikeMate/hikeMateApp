package ch.hikemate.app.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.hikemate.app.HikeMateApp
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.navigation.TEST_TAG_DRAWER_ITEM_PREFIX
import ch.hikemate.app.ui.navigation.TEST_TAG_SIDEBAR_BUTTON
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignInAndMapNavigationTest {
  // Set up the Compose test rule
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testInitialScreenIsAuthScreen() {
    composeTestRule.setContent { HikeMateApp() }

    composeTestRule.onNodeWithTag(Screen.AUTH).assertIsDisplayed()
  }

  @Test
  fun testNavigationToPlannedHikesScreen() {
    composeTestRule.setContent { HikeMateApp() }

    composeTestRule.onNodeWithTag(Screen.AUTH).assertIsDisplayed()

    composeTestRule.onNodeWithTag("loginButton").performClick()
    composeTestRule.onNodeWithTag(Screen.PLANNED_HIKES).assertIsDisplayed()
  }

  @Test
  fun testNavigationToMapScreen() {
    composeTestRule.setContent { HikeMateApp() }
    composeTestRule.onNodeWithTag(Screen.AUTH).assertIsDisplayed()

    composeTestRule.onNodeWithTag("loginButton").performClick()
    composeTestRule.onNodeWithTag(Screen.PLANNED_HIKES).assertIsDisplayed()

    // Open the sidebar
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()

    // Simulate navigation to Map Screen
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + Route.MAP).performClick()

    // Assert that the Map screen is displayed
    composeTestRule
        .onNodeWithTag(Screen.MAP) // Example accessibility description for the Map screen
        .assertIsDisplayed()
  }
}
