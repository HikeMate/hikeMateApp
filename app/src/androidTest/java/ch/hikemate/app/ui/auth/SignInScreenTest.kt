package ch.hikemate.app.ui.auth

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import ch.hikemate.app.MainActivity
import ch.hikemate.app.model.authentication.AuthViewModel
import com.google.firebase.auth.FirebaseUser
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SignInScreenTest : TestCase() {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockAuthViewModel: AuthViewModel
  private val mockUserStateFlow = MutableStateFlow<FirebaseUser?>(null)

  @Before
  fun setUp() {
    Intents.init()
    // Mock AuthViewModel
    mockAuthViewModel = mockk(relaxed = true)
    // Replace the currentUser StateFlow with a mock
    every { mockAuthViewModel.currentUser } returns mockUserStateFlow

    composeTestRule.setContent {
      SignInScreen(
        navigationActions = mockk(),
        authViewModel = mockAuthViewModel
      )
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

    composeTestRule.onNodeWithTag("loginButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("loginButton").assertHasClickAction()
  }

  @Test
  fun loginButtonCallsAuthViewModel() {
    // Set up the SignInScreen with mocked AuthViewModel


    // Simulate clicking the login button
    composeTestRule.onNodeWithTag(TEST_TAG_LOGIN_BUTTON).performClick()

    // Verify that signInWithGoogle was called
    verify { mockAuthViewModel.signInWithGoogle(any(), any(), any()) }
  }
}
