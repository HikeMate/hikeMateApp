package ch.hikemate.app.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.hikemate.app.ui.navigation.*
import ch.hikemate.app.ui.navigation.LIST_TOP_LEVEL_DESTINATIONS
import ch.hikemate.app.ui.navigation.SideBarNavigation
import ch.hikemate.app.ui.navigation.TopLevelDestination
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test

class SideBarNavigationTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun sideBarNavigation_displaysCorrectContent() {
    composeTestRule.setContent {
      SideBarNavigation(onIconSelect = {}, tabList = LIST_TOP_LEVEL_DESTINATIONS, selectedItem = "")
    }

    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).assertExists()
  }

  @Test
  fun sideBarNavigation_opensDrawerOnClick() {
    composeTestRule.setContent {
      SideBarNavigation(onIconSelect = {}, tabList = LIST_TOP_LEVEL_DESTINATIONS, selectedItem = "")
    }

    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CONTENT).assertExists()
  }

  @Test
  fun sideBarNavigation_displaysAllTabs() {
    composeTestRule.setContent {
      SideBarNavigation(onIconSelect = {}, tabList = LIST_TOP_LEVEL_DESTINATIONS, selectedItem = "")
    }

    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()

    LIST_TOP_LEVEL_DESTINATIONS.forEach { tab ->
      composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + tab.route).assertExists()
    }
  }

  @Test
  fun sideBarNavigation_selectsCorrectTab() {
    val selectedItem = LIST_TOP_LEVEL_DESTINATIONS.first().route
    composeTestRule.setContent {
      SideBarNavigation(
          onIconSelect = {}, tabList = LIST_TOP_LEVEL_DESTINATIONS, selectedItem = selectedItem)
    }

    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + selectedItem).assertIsSelected()
  }

  @Test
  fun sideBarNavigation_callsOnIconSelectWhenTabClicked() {
    var selectedDestination: TopLevelDestination? = null

    val onIconSelect: (TopLevelDestination) -> Unit = { destination ->
      selectedDestination = destination
    }

    composeTestRule.setContent {
      SideBarNavigation(
          onIconSelect = onIconSelect, tabList = LIST_TOP_LEVEL_DESTINATIONS, selectedItem = "")
    }

    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + LIST_TOP_LEVEL_DESTINATIONS.first().route)
        .performClick()

    assertEquals(LIST_TOP_LEVEL_DESTINATIONS.first(), selectedDestination)
  }

  @Test
  fun sideBarNavigation_closesDrawerWhenTabClicked() {
    composeTestRule.setContent {
      SideBarNavigation(onIconSelect = {}, tabList = LIST_TOP_LEVEL_DESTINATIONS, selectedItem = "")
    }

    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + LIST_TOP_LEVEL_DESTINATIONS.first().route)
        .performClick()

    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CONTENT).assertIsNotDisplayed()
  }

  @Test
  fun sideBarNavigation_closesDrawerWhenCloseButtonClicked() {
    composeTestRule.setContent {
      SideBarNavigation(onIconSelect = {}, tabList = LIST_TOP_LEVEL_DESTINATIONS, selectedItem = "")
    }

    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CLOSE_BUTTON).performClick()

    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CONTENT).assertIsNotDisplayed()
  }

  private fun SemanticsNodeInteraction.assertIsSelected() {
    assert(SemanticsMatcher.expectValue(IsSelectedKey, true))
  }
}
