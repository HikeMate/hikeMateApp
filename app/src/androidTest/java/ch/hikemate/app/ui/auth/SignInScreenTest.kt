package ch.hikemate.app.ui.auth

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.espresso.intent.Intents
import ch.hikemate.app.MainActivity
import ch.hikemate.app.ui.components.AppIcon
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SignInScreenTest : TestCase() {
  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Before
  fun setUp() {
    Intents.init()
  }

  // Release Intents after each test
  @After
  fun tearDown() {
    Intents.release()
  }

  @Test
  fun everythingIsOnScreen() {
    composeTestRule.onNodeWithTag(AppIcon.TEST_TAG).assertIsDisplayed()

    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_TITLE).assertTextEquals("HikeMate")

    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_EMAIL).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_EMAIL).assertHasClickAction()

    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_GOOGLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_GOOGLE).assertHasClickAction()
  }
}
