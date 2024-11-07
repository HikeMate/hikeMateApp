package ch.hikemate.app.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import ch.hikemate.app.model.authentication.AuthRepository
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.ui.components.BackButton
import ch.hikemate.app.ui.navigation.NavigationActions
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.verify

class SignInWithEmailScreenTest : TestCase() {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navigationActions: NavigationActions
  private lateinit var authRepository: AuthRepository
  private lateinit var authViewModel: AuthViewModel

  @Before
  fun setUp() {
    navigationActions = mock(NavigationActions::class.java)
    authRepository = mock(AuthRepository::class.java)
    authViewModel = AuthViewModel(authRepository)

    composeTestRule.setContent {
      SignInWithEmailScreen(navigationActions = navigationActions, authViewModel = authViewModel)
    }
  }

  @Test
  fun everythingIsOnScreen() {
    composeTestRule.onNodeWithTag(BackButton.BACK_BUTTON_TEST_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInWithEmailScreen.TEST_TAG_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInWithEmailScreen.TEST_TAG_EMAIL_INPUT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInWithEmailScreen.TEST_TAG_PASSWORD_INPUT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInWithEmailScreen.TEST_TAG_SIGN_IN_BUTTON).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SignInWithEmailScreen.TEST_TAG_GO_TO_SIGN_UP_BUTTON)
        .assertIsDisplayed()
  }

  @Test
  fun canWriteToEmailAndPasswordFields() {
    composeTestRule
        .onNodeWithTag(SignInWithEmailScreen.TEST_TAG_EMAIL_INPUT)
        .performTextInput("test@gmail.com")
    composeTestRule
        .onNodeWithTag(SignInWithEmailScreen.TEST_TAG_PASSWORD_INPUT)
        .performTextInput("password")
  }

  @Test
  fun clickOnSignInButton() {
    composeTestRule
        .onNodeWithTag(SignInWithEmailScreen.TEST_TAG_EMAIL_INPUT)
        .performTextInput("test@gmail.com")
    composeTestRule
        .onNodeWithTag(SignInWithEmailScreen.TEST_TAG_PASSWORD_INPUT)
        .performTextInput("password")
    composeTestRule.onNodeWithTag(SignInWithEmailScreen.TEST_TAG_SIGN_IN_BUTTON).performClick()

    verify(authRepository, times(1)).signInWithEmailAndPassword(any(), any(), any(), any())
  }
}
