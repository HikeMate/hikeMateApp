package ch.hikemate.app.endtoend

import android.content.Context
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.hikemate.app.MainActivity
import ch.hikemate.app.ui.auth.SignInScreen
import ch.hikemate.app.ui.auth.SignInWithEmailScreen
import ch.hikemate.app.ui.map.MapScreen.TEST_TAG_MAP
import ch.hikemate.app.ui.navigation.LIST_TOP_LEVEL_DESTINATIONS
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.navigation.Screen.PROFILE
import ch.hikemate.app.ui.navigation.TEST_TAG_DRAWER_CLOSE_BUTTON
import ch.hikemate.app.ui.navigation.TEST_TAG_DRAWER_CONTENT
import ch.hikemate.app.ui.navigation.TEST_TAG_DRAWER_ITEM_PREFIX
import ch.hikemate.app.ui.navigation.TEST_TAG_SIDEBAR_BUTTON
import ch.hikemate.app.ui.navigation.TopLevelDestinations
import ch.hikemate.app.ui.saved.TEST_TAG_SAVED_HIKES_SECTION_CONTAINER
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
  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()
  private val auth = FirebaseAuth.getInstance()
  private val myUuid = UUID.randomUUID()
  private val myUuidAsString = myUuid.toString()
  private val email = "$myUuidAsString@gmail.com"
  private val password = "password"

  @Before
  fun setupFirebase() {
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
  fun test() {
    // Check that we are on the login screen
    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_EMAIL).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_GOOGLE).assertIsDisplayed()

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

    // Check that we are on the map
    composeTestRule.onNodeWithTag(TEST_TAG_MAP).assertIsDisplayed()

    // Check that we can open the menu
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()

    // Check that the menu is open
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CONTENT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CLOSE_BUTTON).assertIsDisplayed()

    // Check that the menu items are there in respect to LIST_TOP_LEVEL_DESTINATIONS
    for (tld in LIST_TOP_LEVEL_DESTINATIONS) {
      composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + tld.route).assertIsDisplayed()
    }

    // Check that we can close the menu
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CLOSE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CLOSE_BUTTON).performClick()

    // Check that the menu is closed
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CONTENT).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CLOSE_BUTTON).assertIsNotDisplayed()

    // Check that we can open the menu again
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()

    // Check that all the menu items can be clicked
    for (tld in LIST_TOP_LEVEL_DESTINATIONS) {
      composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + tld.route).assertIsDisplayed()
      composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + tld.route).assertHasClickAction()
    }

    // Check that we can go to the saved hikes screen
    composeTestRule
        .onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + TopLevelDestinations.SAVED_HIKES.route)
        .performClick()
    composeTestRule.onNodeWithTag(TEST_TAG_MAP).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_SAVED_HIKES_SECTION_CONTAINER).assertIsDisplayed()

    // Check that we can go back to the map
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + TopLevelDestinations.MAP.route)
        .performClick()
    composeTestRule.onNodeWithTag(TEST_TAG_MAP).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TEST_TAG_SAVED_HIKES_SECTION_CONTAINER).assertIsNotDisplayed()

    // Check that we can go to the profile screen
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + TopLevelDestinations.PROFILE.route)
        .performClick()
    composeTestRule.onNodeWithTag(TEST_TAG_MAP).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(PROFILE).assertIsDisplayed()

    // Check that we can go back to the map
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(TEST_TAG_DRAWER_ITEM_PREFIX + TopLevelDestinations.MAP.route)
        .performClick()
    composeTestRule.onNodeWithTag(TEST_TAG_MAP).assertIsDisplayed()
  }
}
