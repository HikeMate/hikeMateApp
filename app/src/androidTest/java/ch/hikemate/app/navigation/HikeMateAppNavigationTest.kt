package ch.hikemate.app.navigation

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.hikemate.app.HikeMateApp
import ch.hikemate.app.ui.auth.SignInScreen
import ch.hikemate.app.ui.auth.SignInWithEmailScreen
import ch.hikemate.app.ui.components.BackButton
import ch.hikemate.app.ui.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HikeMateAppNavigationTest {
  // Set up the Compose test rule
  @get:Rule val composeTestRule = createComposeRule()
  private val auth = FirebaseAuth.getInstance()

  @Before
  fun setUp() {
    auth.signOut()
    composeTestRule.setContent { HikeMateApp() }
  }

  @Test
  fun testNavigationThroughAuthScreens() {
    composeTestRule.onNodeWithTag(Screen.AUTH).assertIsDisplayed()

    // Navigate to the sign in screen
    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_EMAIL).performClick()
    composeTestRule.onNodeWithTag(Screen.SIGN_IN_WITH_EMAIL).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SignInWithEmailScreen.TEST_TAG_GO_TO_SIGN_UP_BUTTON)
        .assertHasClickAction()
        .assertIsDisplayed()
        .performClick()
    composeTestRule.onNodeWithTag(Screen.CREATE_ACCOUNT).assertIsDisplayed()

    // Navigate back to the sign in screen
    composeTestRule.onNodeWithTag(BackButton.BACK_BUTTON_TEST_TAG).performClick()
    composeTestRule.onNodeWithTag(Screen.SIGN_IN_WITH_EMAIL).assertIsDisplayed()
    composeTestRule.onNodeWithTag(BackButton.BACK_BUTTON_TEST_TAG).performClick()
    composeTestRule.onNodeWithTag(Screen.AUTH).assertIsDisplayed()
  }
}
