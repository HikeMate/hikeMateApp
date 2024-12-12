package ch.hikemate.app.endtoend

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.hikemate.app.MainActivity
import ch.hikemate.app.ui.auth.CreateAccountScreen
import ch.hikemate.app.ui.auth.SignInScreen
import ch.hikemate.app.ui.auth.SignInWithEmailScreen
import ch.hikemate.app.ui.map.HikeDetailScreen
import ch.hikemate.app.ui.map.MapScreen
import ch.hikemate.app.ui.map.RunHikeScreen
import ch.hikemate.app.ui.navigation.Screen
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
class EndToEndTest4 : TestCase() {
  @get:Rule val composeTestRule = createEmptyComposeRule()
  private var scenario: ActivityScenario<MainActivity>? = null
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
  public override fun tearDown() {
    scenario?.close()
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun test() {
    // ---- Sign-In ----

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

    // Wait for the sign-in to be performed and the map to load
    composeTestRule.waitUntilExactlyOneExists(
        hasTestTag(MapScreen.TEST_TAG_MAP), timeoutMillis = 10000)

    // ---- Navigate to a hike's details screen ----

    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()

    // Click on the first of the found hikes
    composeTestRule.waitUntilAtLeastOneExists(
        hasTestTag(MapScreen.TEST_TAG_HIKE_ITEM), timeoutMillis = 10000)
    composeTestRule.onAllNodesWithTag(MapScreen.TEST_TAG_HIKE_ITEM)[0].performClick()

    // Wait for the transition to complete
    composeTestRule.waitUntilExactlyOneExists(
        hasTestTag(HikeDetailScreen.TEST_TAG_MAP), timeoutMillis = 10000)

    // ---- Navigate to RunHike Screen ----

    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_RUN_HIKE_BUTTON)
        .assertExists()
        .assertIsDisplayed()
        .performClick()

    // Wait for the transition to complete
    composeTestRule.waitUntilExactlyOneExists(
        hasTestTag(RunHikeScreen.TEST_TAG_MAP), timeoutMillis = 10000)

    // ---- RunHike Screen ----

    composeTestRule.onNodeWithTag(RunHikeScreen.TEST_TAG_MAP).assertIsDisplayed()
  }
}
