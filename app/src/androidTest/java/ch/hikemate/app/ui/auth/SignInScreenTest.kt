package ch.hikemate.app.ui.auth

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import ch.hikemate.app.model.authentication.AuthViewModel
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SignInScreenTest : TestCase() {
  @get:Rule val composeTestRule = createComposeRule()

  private val mockViewModel = mockk<AuthViewModel>(relaxed = true)

  @Before
  fun setUp() {
    Intents.init()
    composeTestRule.setContent { SignInScreen(mockk(), mockViewModel) }
  }

  // Release Intents after each test
  @After
  fun tearDown() {
    Intents.release()
  }

  @Test
  fun everythingIsOnScreen() {
    composeTestRule.onNodeWithTag("appIcon").assertIsDisplayed()

    composeTestRule.onNodeWithTag("appNameText").assertIsDisplayed()
    composeTestRule.onNodeWithTag("appNameText").assertTextEquals("HikeMate")

    composeTestRule.onNodeWithTag("loginButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("loginButton").assertHasClickAction()
  }

  @Test
  fun loginButtonCallsSignIn() {
    composeTestRule.onNodeWithTag("loginButton").performClick()

    // Verify that the signIn function was called
    verify { mockViewModel.signInWithGoogle(any(), any(), any(), any()) }
  }
}
