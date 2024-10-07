package ch.hikemate.app.navigation

import androidx.compose.material3.DrawerValue.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
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

  @Test
  fun sidebarNavigation_withSelectedItem() {
    val selectedItem = Route.OVERVIEW
    val onIconSelect: (TopLevelDestination) -> Unit = {}

    composeTestRule.setContent {
      SideBarNavigation(onIconSelect, LIST_TOP_LEVEL_DESTINATIONS, selectedItem)
    }

    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + Route.OVERVIEW).assertIsSelected()
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + Route.MAP).assertIsNotSelected()
  }

  @Test
  fun sideBarNavigation_withEmptyTabList() {
    composeTestRule.setContent {
      SideBarNavigation(onIconSelect = {}, tabList = emptyList(), selectedItem = "")
    }

    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()

    LIST_TOP_LEVEL_DESTINATIONS.forEach { tab ->
      composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + tab.route).assertIsNotDisplayed()
    }
  }

  @Test
  fun sideBarNavigation_withInvalidSelectedItem() {
    val invalidSelectedItem = "invalid_route"
    composeTestRule.setContent {
      SideBarNavigation(
          onIconSelect = {},
          tabList = LIST_TOP_LEVEL_DESTINATIONS,
          selectedItem = invalidSelectedItem)
    }

    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()
    LIST_TOP_LEVEL_DESTINATIONS.forEach { tab ->
      if (tab.route == invalidSelectedItem) {
        // It should not be marked as selected
        composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + tab.route).assertIsNotSelected()
      } else {
        // Other tabs should not be affected
        composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + tab.route).assertExists()
      }
    }
  }

  @Test
  fun sideBarNavigation_allTabsAreClickable() {
    var selectedItem: String = ""
    val onIconSelect: (TopLevelDestination) -> Unit = { selectedItem = it.route }
    composeTestRule.setContent {
      SideBarNavigation(
          onIconSelect = onIconSelect,
          tabList = LIST_TOP_LEVEL_DESTINATIONS,
          selectedItem = selectedItem)
    }

    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()

    LIST_TOP_LEVEL_DESTINATIONS.forEach { tab ->
      composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + tab.route).performClick()

      assertEquals(tab.route, selectedItem)
    }
  }

  @Test
  fun sideBarNavigation_drawerOpensWhenListEmpty() {
    composeTestRule.setContent {
      SideBarNavigation(onIconSelect = {}, tabList = emptyList(), selectedItem = "")
    }

    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()

    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CONTENT).assertIsDisplayed()
  }

  @Test
  fun sideBarNavigation_correctIconDisplayedForEachTab() {
    composeTestRule.setContent {
      SideBarNavigation(onIconSelect = {}, tabList = LIST_TOP_LEVEL_DESTINATIONS, selectedItem = "")
    }

    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()

    LIST_TOP_LEVEL_DESTINATIONS.forEach { tab ->
      composeTestRule
          .onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + tab.route)
          .assert(hasContentDescription(tab.textId))
    }
  }

  @Test
  fun sideBarNavigation_multipleClicksOnSameTabKeepDrawerClosed() {
    val selectedItem = LIST_TOP_LEVEL_DESTINATIONS.first().route
    composeTestRule.setContent {
      SideBarNavigation(
          onIconSelect = {}, tabList = LIST_TOP_LEVEL_DESTINATIONS, selectedItem = selectedItem)
    }

    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()

    val firstTabTag = TEST_TAG_DRAWER_ITEM_PREFIX + selectedItem
    composeTestRule.onNodeWithTag(firstTabTag).performClick()
    composeTestRule.onNodeWithTag(firstTabTag).performClick()

    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CONTENT).assertIsNotDisplayed()
  }

  @Test
  fun sideBarNavigation_accessibilityTest() {
    composeTestRule.setContent {
      SideBarNavigation(onIconSelect = {}, tabList = LIST_TOP_LEVEL_DESTINATIONS, selectedItem = "")
    }

    composeTestRule
        .onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON)
        .assertHasClickAction()
        .assertContentDescriptionEquals("SideBar")

    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()

    LIST_TOP_LEVEL_DESTINATIONS.forEach { tab ->
      composeTestRule
          .onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + tab.route)
          .assertHasClickAction()
          .assertTextEquals(tab.textId)
    }
  }

  @Test
  fun sideBarNavigation_drawerGesturesDisabled() {
    composeTestRule.setContent {
      SideBarNavigation(onIconSelect = {}, tabList = LIST_TOP_LEVEL_DESTINATIONS, selectedItem = "")
    }

    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CONTENT).assertIsNotDisplayed()

    composeTestRule.onRoot().performTouchInput { swipeRight() }

    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CONTENT).assertIsNotDisplayed()
  }

  @Test
  fun sideBarNavigation_drawerWidth() {
    composeTestRule.setContent {
      SideBarNavigation(onIconSelect = {}, tabList = LIST_TOP_LEVEL_DESTINATIONS, selectedItem = "")
    }

    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()

    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CONTENT).assertWidthIsEqualTo(180.dp)
  }

  @Test
  fun sideBarNavigation_rapidTabSwitching() {
    var selectedTab by mutableStateOf(LIST_TOP_LEVEL_DESTINATIONS.first().route)

    composeTestRule.setContent {
      SideBarNavigation(
          onIconSelect = { selectedTab = it.route },
          tabList = LIST_TOP_LEVEL_DESTINATIONS,
          selectedItem = selectedTab)
    }

    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()

    LIST_TOP_LEVEL_DESTINATIONS.forEach { tab ->
      composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + tab.route).performClick()
    }

    assertEquals(LIST_TOP_LEVEL_DESTINATIONS.last().route, selectedTab)
  }

  @Test
  fun sideBarNavigation_emptyTabList() {
    composeTestRule.setContent {
      SideBarNavigation(onIconSelect = {}, tabList = emptyList(), selectedItem = "")
    }

    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CONTENT).assertExists()
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CONTENT).onChildren().assertCountEquals(2)
  }

  @Test
  fun sideBarNavigation_singleItemTabList() {
    val singleItem = LIST_TOP_LEVEL_DESTINATIONS.first()
    composeTestRule.setContent {
      SideBarNavigation(
          onIconSelect = {}, tabList = listOf(singleItem), selectedItem = singleItem.route)
    }

    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + singleItem.route)
        .assertExists()
        .assertIsSelected()
  }

  @Test
  fun sideBarNavigation_selectedItemNotInList() {
    val nonExistentRoute = "non_existent_route"
    composeTestRule.setContent {
      SideBarNavigation(
          onIconSelect = {}, tabList = LIST_TOP_LEVEL_DESTINATIONS, selectedItem = nonExistentRoute)
    }

    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()
    LIST_TOP_LEVEL_DESTINATIONS.forEach { tab ->
      composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + tab.route).assertIsNotSelected()
    }
  }

  @Test
  fun sideBarNavigation_onIconSelectCallback() {
    var selectedDestination: TopLevelDestination? = null

    composeTestRule.setContent {
      SideBarNavigation(
          onIconSelect = { selectedDestination = it },
          tabList = LIST_TOP_LEVEL_DESTINATIONS,
          selectedItem = "")
    }

    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()

    LIST_TOP_LEVEL_DESTINATIONS.forEach { destination ->
      composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + destination.route).performClick()
      assertEquals(destination, selectedDestination)
    }
  }

  @Test
  fun navigationItemDrawer_displaysLabel() {
    composeTestRule.setContent {
      SideBarNavigation(onIconSelect = {}, tabList = LIST_TOP_LEVEL_DESTINATIONS, selectedItem = "")
    }

    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()

    LIST_TOP_LEVEL_DESTINATIONS.forEach { tab ->
      composeTestRule
          .onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + tab.route)
          .assertTextEquals(tab.textId)
    }
  }
}
