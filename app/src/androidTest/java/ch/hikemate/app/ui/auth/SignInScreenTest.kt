package ch.hikemate.app.ui.auth

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.credentials.exceptions.NoCredentialException
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.intent.Intents
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.ui.navigation.NavigationActions
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import io.mockk.every
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
    composeTestRule.setContent {
      SignInScreen(NavigationActions(rememberNavController()), mockViewModel)
    }
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

    composeTestRule.onNodeWithTag(TEST_TAG_LOGIN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_LOGIN_BUTTON).assertHasClickAction()
  }

  @Test
  fun loginButtonCallsSignIn() {
    composeTestRule.onNodeWithTag(TEST_TAG_LOGIN_BUTTON).performClick()

    // Verify that the signIn function was called
    verify { mockViewModel.signInWithGoogle(any(), any(), any(), any()) }
  }

  @Test
  @Suppress("UNCHECKED_CAST")
  fun signInWithError() {
    // Mock the signIn function to return an error
    every { mockViewModel.signInWithGoogle(any(), any(), any(), any()) } answers
        {
          val onError = args[3] as (NoCredentialException) -> Unit
          onError(NoCredentialException("No credentials found"))
        }

    // Click the login button
    composeTestRule.onNodeWithTag(TEST_TAG_LOGIN_BUTTON).performClick()

    // The button should still be displayed
    composeTestRule.onNodeWithTag(TEST_TAG_LOGIN_BUTTON).assertIsDisplayed()
  }
}
