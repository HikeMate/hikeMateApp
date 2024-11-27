package ch.hikemate.app.endtoend

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.hikemate.app.HikeMateApp
import ch.hikemate.app.ui.auth.CreateAccountScreen
import ch.hikemate.app.ui.auth.SignInScreen
import ch.hikemate.app.ui.auth.SignInWithEmailScreen
import ch.hikemate.app.ui.components.CenteredLoadingAnimation
import ch.hikemate.app.ui.map.MapScreen
import ch.hikemate.app.ui.navigation.LIST_TOP_LEVEL_DESTINATIONS
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.navigation.Screen.PROFILE
import ch.hikemate.app.ui.navigation.TEST_TAG_BOTTOM_BAR
import ch.hikemate.app.ui.navigation.TEST_TAG_MENU_ITEM_PREFIX
import ch.hikemate.app.ui.navigation.TopLevelDestinations
import ch.hikemate.app.ui.saved.SavedHikesScreen
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import junit.framework.TestCase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EndToEndTest : TestCase() {
  @get:Rule val composeTestRule = createComposeRule()
  private val auth = FirebaseAuth.getInstance()
  private val myUuid = UUID.randomUUID()
  private val myUuidAsString = myUuid.toString()
  private val email = "$myUuidAsString@gmail.com"
  private val password = "password"

  @Before
  fun setupFirebase() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    FirebaseApp.initializeApp(context)

    var signedOut = false

    // Wait for sign out to complete
    FirebaseAuth.getInstance().addAuthStateListener {
      if (it.currentUser == null) {
        signedOut = true
      }
    }

    auth.signOut()

    val timeout = System.currentTimeMillis() + 10000 // 10 seconds
    while (!signedOut && System.currentTimeMillis() < timeout) {
      Thread.sleep(100)
    }

    if (!signedOut) {
      throw Exception("Failed to sign out")
    }

    // Make sure the log out is considered in the MainActivity
    composeTestRule.setContent { HikeMateApp() }
  }

  @After
  fun deleteUser() {
    // Sign out after deleting for sanity check and un-reliability
    val credential = EmailAuthProvider.getCredential(email, password)
    auth.currentUser?.reauthenticate(credential)
    auth.currentUser?.delete()
    auth.signOut()
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun test() {
    composeTestRule.waitForIdle()

    // Check that we are on the login screen
    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_EMAIL).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_GOOGLE).assertIsDisplayed()

    // Perform sign in with email and password
    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_EMAIL).performClick()

    composeTestRule.waitUntilExactlyOneExists(
        hasTestTag(Screen.SIGN_IN_WITH_EMAIL), timeoutMillis = 10000)

    composeTestRule
        .onNodeWithTag(SignInWithEmailScreen.TEST_TAG_GO_TO_SIGN_UP_BUTTON)
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()
    composeTestRule.onNodeWithTag(Screen.CREATE_ACCOUNT).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_NAME_INPUT)
        .assertIsDisplayed()
        .performTextInput(myUuidAsString)

    Espresso.closeSoftKeyboard()

    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_EMAIL_INPUT)
        .assertIsDisplayed()
        .performTextInput(email)

    Espresso.closeSoftKeyboard()

    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_PASSWORD_INPUT)
        .assertIsDisplayed()
        .performTextInput(password)

    Espresso.closeSoftKeyboard()

    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_CONFIRM_PASSWORD_INPUT)
        .assertIsDisplayed()
        .performTextInput(password)

    Espresso.closeSoftKeyboard()

    composeTestRule
        .onNodeWithTag(CreateAccountScreen.TEST_TAG_SIGN_UP_BUTTON)
        .assertHasClickAction()
        .assertIsDisplayed()
        .performClick()

    // Wait for the map to load
    composeTestRule.waitUntilExactlyOneExists(
        hasTestTag(MapScreen.TEST_TAG_MAP), timeoutMillis = 10000)

    // Check that we are on the map
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_MAP).assertIsDisplayed()

    // Check that the menu is displayed
    composeTestRule.onNodeWithTag(TEST_TAG_BOTTOM_BAR).assertIsDisplayed()

    // Check that the menu items are there in respect to LIST_TOP_LEVEL_DESTINATIONS
    for (tld in LIST_TOP_LEVEL_DESTINATIONS) {
      composeTestRule.onNodeWithTag(TEST_TAG_MENU_ITEM_PREFIX + tld.route).assertIsDisplayed()
    }

    // Check that all the menu items can be clicked
    for (tld in LIST_TOP_LEVEL_DESTINATIONS) {
      composeTestRule.onNodeWithTag(TEST_TAG_MENU_ITEM_PREFIX + tld.route).assertIsDisplayed()
      composeTestRule.onNodeWithTag(TEST_TAG_MENU_ITEM_PREFIX + tld.route).assertHasClickAction()
    }

    // Check that we can go to the saved hikes screen
    composeTestRule
        .onNodeWithTag(TEST_TAG_MENU_ITEM_PREFIX + TopLevelDestinations.SAVED_HIKES.route)
        .performClick()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_MAP).assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_SECTION_CONTAINER)
        .assertIsDisplayed()

    // Check that we can go back to the map
    composeTestRule
        .onNodeWithTag(TEST_TAG_MENU_ITEM_PREFIX + TopLevelDestinations.MAP.route)
        .performClick()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_MAP).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_SECTION_CONTAINER)
        .assertIsNotDisplayed()

    // Check that we can go to the profile screen
    composeTestRule
        .onNodeWithTag(TEST_TAG_MENU_ITEM_PREFIX + TopLevelDestinations.PROFILE.route)
        .performClick()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_MAP).assertIsNotDisplayed()

    composeTestRule.waitUntilDoesNotExist(
        hasTestTag(CenteredLoadingAnimation.TEST_TAG_CENTERED_LOADING_ANIMATION),
        timeoutMillis = 10000)

    composeTestRule.onNodeWithTag(PROFILE).assertIsDisplayed()

    // Check that we can go back to the map
    composeTestRule
        .onNodeWithTag(TEST_TAG_MENU_ITEM_PREFIX + TopLevelDestinations.MAP.route)
        .performClick()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_MAP).assertIsDisplayed()
  }
}
