package ch.hikemate.app.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import ch.hikemate.app.HikeMateApp
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.TopLevelDestinations
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class SignInAndMapNavigationTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navHostController: NavHostController
  private lateinit var navigationActions: NavigationActions

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    navHostController = mock(NavHostController::class.java)
    navigationActions = NavigationActions(navHostController)
  }

  @Test
  fun signInNavigates_toPlannedHikes() {
    composeTestRule.setContent { HikeMateApp() }
    composeTestRule.onNodeWithTag("loginButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("loginButton").performClick()

    verify(navigationActions).navigateTo(eq(TopLevelDestinations.PLANNED_HIKES))
  }
}
