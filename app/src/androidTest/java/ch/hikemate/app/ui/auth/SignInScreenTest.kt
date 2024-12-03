package ch.hikemate.app.ui.auth

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import ch.hikemate.app.R
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.ui.components.AppIcon
import ch.hikemate.app.ui.components.CenteredErrorAction
import ch.hikemate.app.ui.components.CenteredLoadingAnimation
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.TopLevelDestinations
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

  private lateinit var mockNavigationActions: NavigationActions
  private lateinit var mockAuthViewModel: AuthViewModel
  private val mockUserStateFlow = MutableStateFlow<FirebaseUser?>(null)
  private val mockLoadingStateFlow = MutableStateFlow(false)

  @Before
  fun setUp() {
    Intents.init()

    mockNavigationActions = mockk(relaxed = true)
    mockAuthViewModel = mockk(relaxed = true)

    // Replace the currentUser StateFlow with a mock, which is initially null, so not signed in
    every { mockAuthViewModel.currentUser } returns mockUserStateFlow
    every { mockAuthViewModel.loading } returns mockLoadingStateFlow
  }

  // Release Intents after each test
  @After
  fun tearDown() {
    Intents.release()
  }

  fun setupSignInScreen() {
    composeTestRule.setContent {
      SignInScreen(navigationActions = mockNavigationActions, authViewModel = mockAuthViewModel)
    }
  }

  @Test
  fun everythingIsOnScreen() {
    setupSignInScreen()
    composeTestRule.onNodeWithTag(AppIcon.TEST_TAG).assertIsDisplayed()

    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_TITLE).assertTextEquals("HikeMate")

    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_EMAIL).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_EMAIL).assertHasClickAction()

    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_GOOGLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_GOOGLE).assertHasClickAction()
  }

  @Test
  fun loginButtonCallsAuthViewModel() {
    setupSignInScreen()
    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_GOOGLE).performClick()

    verify { mockAuthViewModel.signInWithGoogle(any(), any(), any()) }
  }

  @Test
  fun whenUserIsSignedIn_navigatesToMap() {
    every { mockAuthViewModel.isUserLoggedIn() } returns true

    setupSignInScreen()

    verify { mockNavigationActions.navigateTo(TopLevelDestinations.MAP) }
  }

  @Test
  fun errorMessageIsDisplayed() {
    val errorMessage = R.string.error_occurred_while_signing_in_with_google
    val errorMessageIdStateFlow = MutableStateFlow(errorMessage)
    every { mockAuthViewModel.errorMessageId } returns errorMessageIdStateFlow

    setupSignInScreen()

    composeTestRule
        .onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_MESSAGE)
        .assertIsDisplayed()
  }

  @Test
  fun loadingIsDisplayed() {
    val errorMessageIdStateFlow = MutableStateFlow<Int?>(null)
    val loadingStateFlow = MutableStateFlow(true)
    every { mockAuthViewModel.errorMessageId } returns errorMessageIdStateFlow
    every { mockAuthViewModel.loading } returns loadingStateFlow

    setupSignInScreen()

    composeTestRule
        .onNodeWithTag(CenteredLoadingAnimation.TEST_TAG_CENTERED_LOADING_ANIMATION)
        .assertIsDisplayed()
  }

  @Test
  fun errorCanBeDismissed() {
    val errorMessage = R.string.error_occurred_while_signing_in_with_google
    val errorMessageIdStateFlow = MutableStateFlow(errorMessage)
    every { mockAuthViewModel.errorMessageId } returns errorMessageIdStateFlow

    setupSignInScreen()

    composeTestRule
        .onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_MESSAGE)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_BUTTON).performClick()

    verify { mockAuthViewModel.clearErrorMessage() }
  }
}
