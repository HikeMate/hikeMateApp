package ch.hikemate.app.endtoend

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.hikemate.app.MainActivity
import ch.hikemate.app.R
import ch.hikemate.app.ui.auth.CreateAccountScreen
import ch.hikemate.app.ui.auth.SignInScreen
import ch.hikemate.app.ui.auth.SignInWithEmailScreen
import ch.hikemate.app.ui.components.BackButton
import ch.hikemate.app.ui.components.HikeCard
import ch.hikemate.app.ui.map.HikeDetailScreen
import ch.hikemate.app.ui.map.MapScreen
import ch.hikemate.app.ui.map.ZoomMapButton
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.navigation.TEST_TAG_MENU_ITEM_PREFIX
import ch.hikemate.app.ui.navigation.TopLevelDestinations
import ch.hikemate.app.ui.profile.EditProfileScreen
import ch.hikemate.app.ui.profile.ProfileScreen
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
class EndToEndTest3 : TestCase() {
  @get:Rule val composeTestRule = createEmptyComposeRule()
  private var scenario: ActivityScenario<MainActivity>? = null
  private val auth = FirebaseAuth.getInstance()
  private val myUuid = UUID.randomUUID()
  private val myUuidAsString = myUuid.toString()
  private val email = "$myUuidAsString@gmail.com"
  private val password = "password"

  private val name = "John Doe"

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
  public override fun tearDown() {
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
    // MAP SCREEN
    // ==========================================

    // Check that we are on the map
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_MAP).assertIsDisplayed()

    // Check that the zoom out button is displayed
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_OUT_BUTTON).assertIsDisplayed()

    // Click once on the zoom out button
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_OUT_BUTTON).performClick()

    // Wait for animation
    composeTestRule.waitForIdle()

    // Check that the zoom in button is displayed
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_IN_BUTTON).assertIsDisplayed()

    // Click twice (one here and one below) on the zoom in button, since we need to zoom in twice to
    // enable
    // the search hike here button
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_IN_BUTTON).performClick()

    // Wait for animation
    composeTestRule.waitForIdle()

    // Click a second time (see above)
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_IN_BUTTON).performClick()

    // Wait for animation
    composeTestRule.waitForIdle()

    // Check that the search hike here button is displayed
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).assertIsDisplayed()

    // Click on the search hike here button
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON).performClick()

    // Check that there is a hikes list displayed
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_HIKES_LIST).assertIsDisplayed()

    // Wait for the hikes to load
    composeTestRule.waitUntilAtLeastOneExists(
        hasTestTag(MapScreen.TEST_TAG_HIKE_ITEM), timeoutMillis = 10003)

    // Check that there are at least 5 hikes elevation data loaded
    composeTestRule.waitUntil(
        timeoutMillis = 10004,
    ) {
      composeTestRule
          .onAllNodesWithTag(HikeCard.TEST_TAG_IS_SUITABLE_TEXT, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .size >= 5
    }

    // Make the bottom sheet expand
    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_HIKES_LIST).performTouchInput {
      swipeUp(startY = centerY, endY = 0f, durationMillis = 500)
    }

    // Wait for the bottom sheet to expand
    composeTestRule.waitForIdle()

    // Scroll in the bottom sheet list to find a hike that is challenging
    composeTestRule
        .onAllNodesWithText(
            context.getString(R.string.map_screen_challenging_hike_label), useUnmergedTree = true)[
            0]
        .performScrollTo()

    // Click on the hike card that is challenging
    composeTestRule
        .onAllNodesWithText(
            context.getString(R.string.map_screen_challenging_hike_label), useUnmergedTree = true)[
            0]
        .performClick()

    // Wait for the transition to complete
    composeTestRule.waitUntilExactlyOneExists(
        hasTestTag(HikeDetailScreen.TEST_TAG_MAP), timeoutMillis = 10005)

    // ==========================================
    // DETAILS SCREENS
    // ==========================================

    // Check that the hike is a challenge for the user
    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_APPROPRIATENESS_MESSAGE)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_APPROPRIATENESS_MESSAGE)
        .assertTextEquals(context.getString(R.string.map_screen_challenging_hike_label))

    // Click on the bookmark icon
    composeTestRule.onNodeWithTag(HikeDetailScreen.TEST_TAG_BOOKMARK_ICON).performClick()

    // Wait for the bookmark to be saved
    composeTestRule.waitForIdle()

    // Go back to the map
    composeTestRule.onNodeWithTag(BackButton.BACK_BUTTON_TEST_TAG).performClick()

    // Wait for the transition to complete
    composeTestRule.waitUntilExactlyOneExists(
        hasTestTag(MapScreen.TEST_TAG_MAP), timeoutMillis = 10006)

    // ==========================================
    // Change the profile
    // ==========================================

    // Go to the profile screen
    composeTestRule
        .onNodeWithTag(TEST_TAG_MENU_ITEM_PREFIX + TopLevelDestinations.PROFILE.route)
        .performClick()

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

    // ==========================================
    // SAVED HIKES SCREEN
    // ==========================================

    // Go to the saved hikes screen
    composeTestRule
        .onNodeWithTag(TEST_TAG_MENU_ITEM_PREFIX + TopLevelDestinations.SAVED_HIKES.route)
        .performClick()

    // Wait for the transition to complete
    composeTestRule.waitUntilExactlyOneExists(
        hasTestTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_SECTION_CONTAINER), timeoutMillis = 10010)

    // Check that we are on the saved hikes screen
    composeTestRule
        .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_SECTION_CONTAINER)
        .assertIsDisplayed()

    // Check that the saved hikes section is displayed
    composeTestRule
        .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_SECTION_CONTAINER)
        .assertIsDisplayed()

    // Check that the saved hikes section has one item
    composeTestRule.waitUntilExactlyOneExists(
        hasTestTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_HIKE_CARD), timeoutMillis = 10011)

    // Check that the saved hike is now suitable
    composeTestRule
        .onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_HIKE_CARD)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(SavedHikesScreen.TEST_TAG_SAVED_HIKES_HIKE_CARD).performClick()

    // Wait for the transition to complete
    composeTestRule.waitUntilExactlyOneExists(
        hasTestTag(HikeDetailScreen.TEST_TAG_MAP), timeoutMillis = 10012)

    // Check that the hike is a good challenge for the user
    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_APPROPRIATENESS_MESSAGE)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_APPROPRIATENESS_MESSAGE)
        .assertTextEquals(context.getString(R.string.map_screen_suitable_hike_label))
  }
}
