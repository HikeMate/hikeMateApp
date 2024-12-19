package ch.hikemate.app.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.hikemate.app.ui.navigation.BottomBarNavigation
import ch.hikemate.app.ui.navigation.LIST_TOP_LEVEL_DESTINATIONS
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.TEST_TAG_BOTTOM_BAR
import ch.hikemate.app.ui.navigation.TEST_TAG_MENU_ITEM_PREFIX
import ch.hikemate.app.ui.navigation.TopLevelDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BottomBarNavigationTest {
  // Set up the Compose test rule
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun clickingOnAnItemChangesScreen() {
    var wantedRoute = Route.MAP
    var countDownLatch = 3
    composeTestRule.setContent {
      BottomBarNavigation(
          onTabSelect = {
            if (it.route != wantedRoute) {
              fail("Expected route $wantedRoute but got ${it.route}")
            } else {
              countDownLatch--
            }
          },
          tabList = LIST_TOP_LEVEL_DESTINATIONS,
          selectedItem = Route.MAP) {}
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(TEST_TAG_BOTTOM_BAR).assertIsDisplayed()

    wantedRoute = Route.SAVED_HIKES
    composeTestRule
        .onNodeWithTag(TEST_TAG_MENU_ITEM_PREFIX + TopLevelDestinations.SAVED_HIKES.route)
        .performClick()

    wantedRoute = Route.PROFILE
    composeTestRule
        .onNodeWithTag(TEST_TAG_MENU_ITEM_PREFIX + TopLevelDestinations.PROFILE.route)
        .performClick()

    wantedRoute = Route.TUTORIAL
    composeTestRule
        .onNodeWithTag(TEST_TAG_MENU_ITEM_PREFIX + TopLevelDestinations.TUTORIAL.route)
        .performClick()

    composeTestRule.waitForIdle()
    assertEquals(0, countDownLatch)
  }

  @Test
  fun clickingTwiceOnTheSameItemDoesNotChangeTheScreen() {
    composeTestRule.setContent {
      BottomBarNavigation(
          onTabSelect = { fail("The screen should not change") },
          tabList = LIST_TOP_LEVEL_DESTINATIONS,
          selectedItem = Route.MAP) {}
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(TEST_TAG_BOTTOM_BAR).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(TEST_TAG_MENU_ITEM_PREFIX + TopLevelDestinations.MAP.route)
        .performClick()

    composeTestRule.waitForIdle()
  }
}
