package ch.hikemate.app.endtoend

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import ch.hikemate.app.MainActivity
import ch.hikemate.app.R
import ch.hikemate.app.model.guide.Guide
import ch.hikemate.app.ui.auth.CreateAccountScreen
import ch.hikemate.app.ui.auth.SignInScreen
import ch.hikemate.app.ui.auth.SignInWithEmailScreen
import ch.hikemate.app.ui.guide.GuideScreen
import ch.hikemate.app.ui.map.MapScreen
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.navigation.TEST_TAG_MENU_ITEM_PREFIX
import ch.hikemate.app.ui.navigation.TopLevelDestinations
import ch.hikemate.app.ui.profile.EditProfileScreen
import ch.hikemate.app.ui.profile.ProfileScreen
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EndToEndTest3 {
  @get:Rule val composeTestRule = createEmptyComposeRule()
  private var scenario: ActivityScenario<MainActivity>? = null
  private val auth = FirebaseAuth.getInstance()
  private val myUuid = UUID.randomUUID()
  private val myUuidAsString = myUuid.toString()
  private val email = "$myUuidAsString@gmail.com"
  private val password = "password"

  private val name = "John Doe"
  private val guideTopic = Guide.APP_GUIDE_TOPICS[2]

  @Before
  fun setUpFirebase() {
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
    scenario = ActivityScenario.launch(MainActivity::class.java)
  }

  @After
  fun deleteUser() {
    // Sign out after deleting for sanity check and un-reliability
    val credential = EmailAuthProvider.getCredential(email, password)
    auth.currentUser?.reauthenticate(credential)
    auth.currentUser?.delete()
    auth.signOut()
  }

  @After
  fun tearDown() {
    scenario?.close()
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun test() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    composeTestRule.waitForIdle()

    // ==========================================
    // SIGN IN SCREEN
    // ==========================================

    // Check that we are on the login screen
    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_EMAIL).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_GOOGLE).assertIsDisplayed()

    // Perform sign in with email and password
    composeTestRule.onNodeWithTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_EMAIL).performClick()

    composeTestRule.waitUntilExactlyOneExists(
        hasTestTag(Screen.SIGN_IN_WITH_EMAIL), timeoutMillis = 10001)

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

    // Wait for the sign-in to be performed and the map to load
    composeTestRule.waitUntilExactlyOneExists(
        hasTestTag(MapScreen.TEST_TAG_MAP), timeoutMillis = 10002)

    // ==========================================
    // Tutorial
    // ==========================================
    // Go to the tutorial screen
    composeTestRule
        .onNodeWithTag(TEST_TAG_MENU_ITEM_PREFIX + TopLevelDestinations.TUTORIAL.route)
        .performClick()

    // Wait for the transition to complete
    composeTestRule.waitUntilExactlyOneExists(
        hasTestTag(GuideScreen.GUIDE_SCREEN), timeoutMillis = 10010)

    // Open the profile section
    composeTestRule
        .onNodeWithTag("${GuideScreen.TOPIC_CARD}_${guideTopic.titleResId}")
        .performClick()

    // Wait for the transition to complete
    composeTestRule.waitUntilExactlyOneExists(
        hasTestTag("${GuideScreen.TOPIC_CONTENT}_${guideTopic.titleResId}"), timeoutMillis = 10011)

    // Scroll until the "Go to profile" button is visible
    // Click on the "Go to profile" button
    composeTestRule
        .onNodeWithTag("${GuideScreen.NAVIGATION_BUTTON}_${guideTopic.actionRoute}")
        .performScrollTo()
        .performClick()

    // ==========================================
    // Change the profile
    // ==========================================

    // Wait for the transition to complete
    composeTestRule.waitUntilExactlyOneExists(hasTestTag(Screen.PROFILE), timeoutMillis = 10007)

    // Check that the profile screen is displayed
    composeTestRule.onNodeWithTag(Screen.PROFILE).assertIsDisplayed()

    // Click on the edit profile button
    composeTestRule.onNodeWithTag(ProfileScreen.TEST_TAG_EDIT_PROFILE_BUTTON).performClick()

    // Wait for the transition to complete
    composeTestRule.waitUntilExactlyOneExists(
        hasTestTag(Screen.EDIT_PROFILE), timeoutMillis = 10008)

    // Check that the edit profile screen is displayed
    composeTestRule.onNodeWithTag(Screen.EDIT_PROFILE).assertIsDisplayed()

    // Change the name and hiking level
    composeTestRule.onNodeWithTag(EditProfileScreen.TEST_TAG_NAME_INPUT).performTextClearance()
    composeTestRule.onNodeWithTag(EditProfileScreen.TEST_TAG_NAME_INPUT).performTextInput(name)
    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_EXPERT)
        .performClick()

    // Save the changes
    composeTestRule.onNodeWithTag(EditProfileScreen.TEST_TAG_SAVE_BUTTON).performClick()

    // Wait for the transition to complete
    composeTestRule.waitUntilExactlyOneExists(hasTestTag(Screen.PROFILE), timeoutMillis = 10009)

    // Check that the profile screen is displayed
    composeTestRule.onNodeWithTag(Screen.PROFILE).assertIsDisplayed()

    // Check that the name and hiking level have been updated
    composeTestRule.onNodeWithTag(ProfileScreen.TEST_TAG_NAME).assertTextEquals(name)
    composeTestRule
        .onNodeWithTag(ProfileScreen.TEST_TAG_HIKING_LEVEL)
        .assertTextEquals(context.getString(R.string.profile_screen_hiking_level_info_expert))
  }
}
