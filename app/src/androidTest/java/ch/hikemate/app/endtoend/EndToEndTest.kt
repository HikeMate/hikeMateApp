package ch.hikemate.app.endtoend

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.hikemate.app.MainActivity
import ch.hikemate.app.ui.auth.TEST_TAG_LOGIN_BUTTON
import ch.hikemate.app.ui.map.MapScreen.TEST_TAG_MAP
import ch.hikemate.app.ui.navigation.LIST_TOP_LEVEL_DESTINATIONS
import ch.hikemate.app.ui.navigation.Screen.PLANNED_HIKES
import ch.hikemate.app.ui.navigation.Screen.PROFILE
import ch.hikemate.app.ui.navigation.TEST_TAG_DRAWER_CLOSE_BUTTON
import ch.hikemate.app.ui.navigation.TEST_TAG_DRAWER_CONTENT
import ch.hikemate.app.ui.navigation.TEST_TAG_DRAWER_ITEM_PREFIX
import ch.hikemate.app.ui.navigation.TEST_TAG_SIDEBAR_BUTTON
import ch.hikemate.app.ui.navigation.TopLevelDestinations
import junit.framework.TestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EndToEndTest : TestCase() {
  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun test() {
    // Check that we are on the login screen
    composeTestRule.onNodeWithTag(TEST_TAG_LOGIN_BUTTON).assertIsDisplayed()

    // Click on the login button
    composeTestRule.onNodeWithTag(TEST_TAG_LOGIN_BUTTON).performClick()

    // Check that we are not on the login screen anymore
    composeTestRule.onNodeWithTag(TEST_TAG_LOGIN_BUTTON).assertIsNotDisplayed()

    // Check that we are on the map
    composeTestRule.onNodeWithTag(TEST_TAG_MAP).assertIsDisplayed()

    // Check that we can open the menu
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()

    // Check that the menu is open
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CONTENT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CLOSE_BUTTON).assertIsDisplayed()

    // Check that the menu items are there in respect to LIST_TOP_LEVEL_DESTINATIONS
    for (tld in LIST_TOP_LEVEL_DESTINATIONS) {
      composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + tld.route).assertIsDisplayed()
    }

    // Check that we can close the menu
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CLOSE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CLOSE_BUTTON).performClick()

    // Check that the menu is closed
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CONTENT).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CLOSE_BUTTON).assertIsNotDisplayed()

    // Check that we can open the menu again
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()

    // Check that all the menu items can be clicked
    for (tld in LIST_TOP_LEVEL_DESTINATIONS) {
      composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + tld.route).assertIsDisplayed()
      composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + tld.route).assertHasClickAction()
    }

    // Check that we can go to the planned hikes screen
    composeTestRule
        .onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + TopLevelDestinations.PLANNED_HIKES.route)
        .performClick()
    composeTestRule.onNodeWithTag(TEST_TAG_MAP).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(PLANNED_HIKES).assertIsDisplayed()

    // Check that we can go back to the map
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + TopLevelDestinations.MAP.route)
        .performClick()
    composeTestRule.onNodeWithTag(TEST_TAG_MAP).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PLANNED_HIKES).assertIsNotDisplayed()

    // Check that we can go to the profile screen
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + TopLevelDestinations.PROFILE.route)
        .performClick()
    composeTestRule.onNodeWithTag(TEST_TAG_MAP).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(PROFILE).assertIsDisplayed()

    // Check that we can go back to the map
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + TopLevelDestinations.MAP.route)
        .performClick()
    composeTestRule.onNodeWithTag(TEST_TAG_MAP).assertIsDisplayed()
  }
}
