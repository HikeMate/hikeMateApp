package ch.hikemate.app.navigation

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
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HikeMateAppNavigationTest {
  // Set up the Compose test rule
  @get:Rule val composeTestRule = createComposeRule()
  private val auth = FirebaseAuth.getInstance()
  private val myUuid = UUID.randomUUID()
  private val myUuidAsString = myUuid.toString()
  private val email = "$myUuidAsString@gmail.com"
  private val password = "password"

  @Before
  fun setUp() {
    composeTestRule.setContent { HikeMateApp() }
    auth.signOut()
  }

  @After
  fun deleteUser() {
    // Sign out after deleting for sanity check and un-reliability
    val credential = EmailAuthProvider.getCredential(email, password)
    auth.currentUser?.reauthenticate(credential)
    auth.currentUser?.delete()
    auth.signOut()
  }

  @Test
  fun testNavigationThroughAuthScreens() {
    composeTestRule.onNodeWithTag(Screen.AUTH).assertIsDisplayed()

    // Navigate to the sign in screen
    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_EMAIL).performClick()
    composeTestRule.onNodeWithTag(Screen.SIGN_IN_WITH_EMAIL).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SignInWithEmailScreen.TEST_TAG_GO_TO_SIGN_UP_BUTTON)
        .performClick()
    composeTestRule.onNodeWithTag(Screen.CREATE_ACCOUNT).assertIsDisplayed()

    // Navigate back to the sign in screen
    composeTestRule.onNodeWithTag(BackButton.BACK_BUTTON_TEST_TAG).performClick()
    composeTestRule.onNodeWithTag(Screen.SIGN_IN_WITH_EMAIL).assertIsDisplayed()
    composeTestRule.onNodeWithTag(BackButton.BACK_BUTTON_TEST_TAG).performClick()
    composeTestRule.onNodeWithTag(Screen.AUTH).assertIsDisplayed()
  }
}
