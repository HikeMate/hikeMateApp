package ch.hikemate.app.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.hikemate.app.HikeMateApp
import ch.hikemate.app.model.route.ListOfHikeRoutesViewModel
import ch.hikemate.app.ui.auth.TEST_TAG_LOGIN_BUTTON
import ch.hikemate.app.ui.map.MapScreen
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.navigation.TEST_TAG_DRAWER_ITEM_PREFIX
import ch.hikemate.app.ui.navigation.TEST_TAG_SIDEBAR_BUTTON
import ch.hikemate.app.ui.theme.HikeMateTheme
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HikeMateAppNavigationTest {
  // Set up the Compose test rule
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockNavigationActions: NavigationActions
  private lateinit var mockViewModel: ListOfHikeRoutesViewModel

  @Before
  fun setUp() {
    mockNavigationActions = mockk(relaxed = true)
  }

  @Test
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

  @Test
  fun testNavigationToPlannedHikesScreen() {

    composeTestRule.setContent { MapScreen(mockNavigationActions) }

    composeTestRule.onNodeWithTag(Screen.MAP).assertIsDisplayed()

    // Open the sidebar
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).assertIsDisplayed().performClick()

    composeTestRule
        .onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + Route.SAVED_HIKES)
        .assertIsDisplayed()
        .performClick()

    // TODO continue here, why does it not do this call? maybe someone tmrw can help me out, but i
    // think it might be bc we are nonly movking the navactions or something like that
    verify { mockNavigationActions.navigateTo(Route.SAVED_HIKES) }
  }

  // @Test
  fun testNavigationToProfileScreen() {
    composeTestRule.setContent { MapScreen(mockNavigationActions) }
    composeTestRule.onNodeWithTag(Screen.AUTH).assertIsDisplayed()

    composeTestRule.onNodeWithTag(TEST_TAG_LOGIN_BUTTON).performClick()
    composeTestRule.onNodeWithTag(Screen.MAP).assertIsDisplayed()

    // Open the sidebar
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()

    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + Route.PROFILE).performClick()

    composeTestRule.onNodeWithTag(Screen.PROFILE).assertIsDisplayed()
  }
}
