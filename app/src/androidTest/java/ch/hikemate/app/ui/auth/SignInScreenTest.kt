package ch.hikemate.app.ui.auth

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import ch.hikemate.app.MainActivity
import ch.hikemate.app.ui.components.AppIcon
import ch.hikemate.app.model.authentication.AuthViewModel
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

  @Before
  fun setUp() {
    Intents.init()

    mockNavigationActions = mockk(relaxed = true)
    mockAuthViewModel = mockk(relaxed = true)

    // Replace the currentUser StateFlow with a mock, which is iniially null, so not signed in
    every { mockAuthViewModel.currentUser } returns mockUserStateFlow
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
    composeTestRule.onNodeWithTag("appIcon").assertIsDisplayed()

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
    val mockUser = mockk<FirebaseUser>(relaxed = true)
    mockUserStateFlow.value = mockUser

    setupSignInScreen()

    verify { mockNavigationActions.navigateTo(TopLevelDestinations.MAP) }
  }
}
