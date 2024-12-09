package ch.hikemate.app.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.ui.components.BackButton
import ch.hikemate.app.ui.navigation.NavigationActions
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DeleteAccountScreenTest : TestCase() {
  private lateinit var authViewModel: AuthViewModel
  private lateinit var navigationActions: NavigationActions

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    navigationActions = mockk(relaxed = true)
    authViewModel = mockk(relaxed = true)

    every { navigationActions.goBack() } returns Unit
  }

  private fun setUpActivityEmailProvider() {
    every { authViewModel.isEmailProvider() } returns true
    composeTestRule.setContent { DeleteAccountScreen(navigationActions, authViewModel) }
  }

  private fun setUpActivityNotEmailProvider() {
    every { authViewModel.isEmailProvider() } returns false
    composeTestRule.setContent { DeleteAccountScreen(navigationActions, authViewModel) }
  }

  @Test
  fun isEverythingDisplayed() {
    setUpActivityEmailProvider()
    composeTestRule.onNodeWithTag(BackButton.BACK_BUTTON_TEST_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(DeleteAccountScreen.TEST_TAG_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(DeleteAccountScreen.TEST_TAG_INFO_TEXT).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(DeleteAccountScreen.TEST_TAG_DELETE_ACCOUNT_BUTTON)
        .assertIsDisplayed()
  }

  @Test
  fun passwordFieldIsDisplayedWhenEmailProvider() {
    setUpActivityEmailProvider()
    composeTestRule.onNodeWithTag(DeleteAccountScreen.TEST_TAG_PASSWORD_INPUT).assertIsDisplayed()
  }

  @Test
  fun passwordFieldIsNotDisplayedWhenNotEmailProvider() {
    setUpActivityNotEmailProvider()
    composeTestRule.onNodeWithTag(DeleteAccountScreen.TEST_TAG_PASSWORD_INPUT).assertDoesNotExist()
  }

  @Test
  fun checkCanGoBack() {
    setUpActivityEmailProvider()
    composeTestRule.onNodeWithTag(BackButton.BACK_BUTTON_TEST_TAG).performClick()

    verify { navigationActions.goBack() }
  }

  @Test
  fun checkCanDeleteAccount() {
    setUpActivityEmailProvider()

    val password = "password"

    composeTestRule
        .onNodeWithTag(DeleteAccountScreen.TEST_TAG_PASSWORD_INPUT)
        .performTextInput(password)
    composeTestRule.onNodeWithTag(DeleteAccountScreen.TEST_TAG_DELETE_ACCOUNT_BUTTON).performClick()

    verify(exactly = 1) { authViewModel.deleteAccount(eq(password), any(), any(), any(), any()) }
    verify(exactly = 0) { authViewModel.deleteAccount(neq(password), any(), any(), any(), any()) }
  }

  @Test
  fun checkCannotDeleteAccountWithoutEnteringPassword() {
    setUpActivityEmailProvider()

    composeTestRule.onNodeWithTag(DeleteAccountScreen.TEST_TAG_DELETE_ACCOUNT_BUTTON).performClick()

    verify(exactly = 0) { authViewModel.deleteAccount(any(), any(), any(), any(), any()) }
  }
}
