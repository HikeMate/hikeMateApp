package ch.hikemate.app.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

class CreateAccountScreenTest : TestCase() {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navigationActions: NavigationActions
  private lateinit var authRepository: AuthRepository
  private lateinit var authViewModel: AuthViewModel

  private val TEST_NAME = "Test Name"
  private val TEST_EMAIL = "test@example.com"
  private val TEST_WRONG_EMAIL = "test"
  private val TEST_PASSWORD = "password"
  private val TEST_DIFFERENT_PASSWORD = "different_password"

  @Before
  fun setUp() {
    navigationActions = mock(NavigationActions::class.java)
    authRepository = mock(AuthRepository::class.java)
    authViewModel = AuthViewModel(authRepository)

    composeTestRule.setContent {
      CreateAccountScreen(navigationActions = navigationActions, authViewModel = authViewModel)
    }
  }

  @Test
  fun everythingIsOnScreen() {
    composeTestRule.onNodeWithTag(BackButton.BACK_BUTTON_TEST_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateAccountScreen.TEST_TAG_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateAccountScreen.TEST_TAG_NAME_INPUT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateAccountScreen.TEST_TAG_EMAIL_INPUT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateAccountScreen.TEST_TAG_PASSWORD_INPUT).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_CONFIRM_PASSWORD_INPUT)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateAccountScreen.TEST_TAG_SIGN_UP_BUTTON).assertIsDisplayed()
  }

  @Test
  fun canWriteToAllFields() {
    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_NAME_INPUT)
        .performTextInput(TEST_NAME)
    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_EMAIL_INPUT)
        .performTextInput(TEST_EMAIL)
    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_PASSWORD_INPUT)
        .performTextInput(TEST_PASSWORD)
    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_CONFIRM_PASSWORD_INPUT)
        .performTextInput(TEST_DIFFERENT_PASSWORD)

    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_NAME_INPUT)
        .assertTextContains(TEST_NAME)
    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_EMAIL_INPUT)
        .assertTextContains(TEST_EMAIL)
  }

  @Test
  fun clickOnCreateAccountButton() {
    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_NAME_INPUT)
        .performTextInput(TEST_NAME)
    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_EMAIL_INPUT)
        .performTextInput(TEST_EMAIL)
    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_PASSWORD_INPUT)
        .performTextInput(TEST_PASSWORD)
    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_CONFIRM_PASSWORD_INPUT)
        .performTextInput(TEST_PASSWORD)
    composeTestRule.onNodeWithTag(CreateAccountScreen.TEST_TAG_SIGN_UP_BUTTON).performClick()

    verify(authRepository, times(1))
        .createAccountWithEmailAndPassword(any(), any(), eq(TEST_EMAIL), eq(TEST_PASSWORD))
  }

  @Test
  fun clickOnCreateAccountButtonWithEmptyFields() {
    composeTestRule.onNodeWithTag(CreateAccountScreen.TEST_TAG_SIGN_UP_BUTTON).performClick()

    verify(authRepository, times(0)).createAccountWithEmailAndPassword(any(), any(), any(), any())
  }

  @Test
  fun clickOnCreateAccountWithDummyEmail() {
    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_NAME_INPUT)
        .performTextInput(TEST_NAME)
    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_EMAIL_INPUT)
        .performTextInput(TEST_WRONG_EMAIL)
    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_PASSWORD_INPUT)
        .performTextInput(TEST_PASSWORD)
    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_CONFIRM_PASSWORD_INPUT)
        .performTextInput(TEST_PASSWORD)
    composeTestRule.onNodeWithTag(CreateAccountScreen.TEST_TAG_SIGN_UP_BUTTON).performClick()

    verify(authRepository, times(0)).createAccountWithEmailAndPassword(any(), any(), any(), any())
  }

  @Test
  fun clickOnCreateAccountButtonWithDifferentPasswords() {
    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_NAME_INPUT)
        .performTextInput(TEST_NAME)
    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_EMAIL_INPUT)
        .performTextInput(TEST_EMAIL)
    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_PASSWORD_INPUT)
        .performTextInput(TEST_PASSWORD)
    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_CONFIRM_PASSWORD_INPUT)
        .performTextInput(TEST_DIFFERENT_PASSWORD)
    composeTestRule.onNodeWithTag(CreateAccountScreen.TEST_TAG_SIGN_UP_BUTTON).performClick()

    verify(authRepository, times(0))
        .createAccountWithEmailAndPassword(any(), any(), eq(TEST_EMAIL), eq(TEST_PASSWORD))
  }
}
