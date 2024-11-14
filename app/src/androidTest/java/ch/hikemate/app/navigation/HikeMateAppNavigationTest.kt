package ch.hikemate.app.navigation

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.hikemate.app.HikeMateApp
import ch.hikemate.app.ui.auth.SignInScreen
import ch.hikemate.app.ui.auth.SignInWithEmailScreen
import ch.hikemate.app.ui.components.BackButton
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.navigation.TEST_TAG_DRAWER_ITEM_PREFIX
import ch.hikemate.app.ui.navigation.TEST_TAG_SIDEBAR_BUTTON
import ch.hikemate.app.ui.saved.SavedHikesScreen
import com.google.firebase.FirebaseApp
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

  private fun setupUser() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    FirebaseApp.initializeApp(context)

    auth.createUserWithEmailAndPassword(email, password)
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

  /**
   * Tests the whole navigation flow from the auth screen to the map screen. All the screens are
   * tested because signing in takes time.
   */
  @Test
  fun testWholeNavigation() {
    // Create a user in order to skip the auth screen
    setupUser()

    composeTestRule.onNodeWithTag(Screen.AUTH).assertIsDisplayed()

    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_EMAIL).performClick()
    composeTestRule.onNodeWithTag(Screen.SIGN_IN_WITH_EMAIL).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SignInWithEmailScreen.TEST_TAG_EMAIL_INPUT)
        .performTextInput(email)
    composeTestRule
        .onNodeWithTag(SignInWithEmailScreen.TEST_TAG_PASSWORD_INPUT)
        .performTextInput(password)
    composeTestRule.onNodeWithTag(SignInWithEmailScreen.TEST_TAG_SIGN_IN_BUTTON).performClick()

    // Wait for the map to load
    Thread.sleep(1000)

    composeTestRule.onNodeWithTag(Screen.MAP).assertIsDisplayed()

    // Go to planned hikes
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + Route.SAVED_HIKES).performClick()
    composeTestRule
        .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_SECTION_CONTAINER)
        .assertIsDisplayed()

    // Go to profile screen
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + Route.PROFILE).performClick()
    composeTestRule.onNodeWithTag(Screen.PROFILE).assertIsDisplayed()
  }
}
