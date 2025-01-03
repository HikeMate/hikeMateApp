package ch.hikemate.app.endtoend

import android.content.Context
import android.location.Location
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import ch.hikemate.app.MainActivity
import ch.hikemate.app.R
import ch.hikemate.app.ui.auth.CreateAccountScreen
import ch.hikemate.app.ui.auth.SignInScreen
import ch.hikemate.app.ui.auth.SignInWithEmailScreen
import ch.hikemate.app.ui.map.HikeDetailScreen
import ch.hikemate.app.ui.map.MapScreen
import ch.hikemate.app.ui.map.RunHikeScreen
import ch.hikemate.app.ui.map.ZoomMapButton
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.utils.LocationUtils
import ch.hikemate.app.utils.MapUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import java.util.UUID
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EndToEndTest4 {
  @get:Rule val composeTestRule = createEmptyComposeRule()
  private var scenario: ActivityScenario<MainActivity>? = null
  private val auth = FirebaseAuth.getInstance()
  private val myUuid = UUID.randomUUID()
  private val myUuidAsString = myUuid.toString()
  private val email = "$myUuidAsString@gmail.com"
  private val password = "password"

  private var locationCallback = mockk<LocationCallback>()

  @OptIn(ExperimentalPermissionsApi::class)
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

    mockkObject(LocationUtils)
    mockkObject(MapUtils)
    every { LocationUtils.hasLocationPermission(any()) } returns true
    every { LocationUtils.getUserLocation(any(), any(), any(), any()) } answers
        {
          val locCallback = arg<(Location?) -> Unit>(1)
          locCallback(
              Location("gps").apply {
                latitude = 46.5775927207486
                longitude = 6.551607112518172
              })
        }
    every {
      LocationUtils.onLocationPermissionsUpdated(
          any(), any(), any(), any<LocationCallback>(), any(), any())
    } answers
        {
          val locCallback = arg<LocationCallback>(3)
          locationCallback = locCallback
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

  @Test
  @OptIn(ExperimentalTestApi::class)
  fun test() {
    val context = ApplicationProvider.getApplicationContext<Context>()
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

    composeTestRule.waitUntilExactlyOneExists(hasTestTag(Screen.MAP), timeoutMillis = 30000)

    // ---- Navigate to a hike's details screen ----

    composeTestRule.onNodeWithTag(MapScreen.TEST_TAG_CENTER_MAP_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // We want to zoom in to be sure of the selected hike
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_IN_BUTTON).performClick()
    Thread.sleep(1000)
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_IN_BUTTON).performClick()
    Thread.sleep(1000)
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_IN_BUTTON).performClick()
    Thread.sleep(1000)
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_IN_BUTTON).performClick()
    Thread.sleep(1000)
    composeTestRule.onNodeWithTag(ZoomMapButton.ZOOM_IN_BUTTON).performClick()
    Thread.sleep(1000)

    composeTestRule
        .onNodeWithTag(MapScreen.TEST_TAG_SEARCH_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // Click on the first of the found hikes
    composeTestRule.waitUntilAtLeastOneExists(
        hasTestTag(MapScreen.TEST_TAG_HIKE_ITEM), timeoutMillis = 30000)

    composeTestRule.onAllNodesWithTag(MapScreen.TEST_TAG_HIKE_ITEM)[0].performClick()

    composeTestRule.waitUntilExactlyOneExists(
        hasTestTag(HikeDetailScreen.TEST_TAG_MAP), timeoutMillis = 30000)

    // ---- Navigate to RunHike Screen ----

    composeTestRule.waitUntilExactlyOneExists(
        hasTestTag(HikeDetailScreen.TEST_TAG_BOTTOM_SHEET), timeoutMillis = 30000)

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HikeDetailScreen.TEST_TAG_BOTTOM_SHEET).performTouchInput {
      swipeUp()
    }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(HikeDetailScreen.TEST_TAG_RUN_HIKE_BUTTON)
        .assertExists()
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitUntilExactlyOneExists(hasTestTag(Screen.RUN_HIKE), timeoutMillis = 10000)

    composeTestRule.onNodeWithTag(RunHikeScreen.TEST_TAG_BOTTOM_SHEET).performTouchInput {
      swipeUp()
    }

    composeTestRule.waitForIdle()

    locationCallback.onLocationResult(
        LocationResult.create(
            listOf(
                Location("gps").apply {
                  latitude = 46.5775927207486
                  longitude = 6.551607112518172
                })))

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(RunHikeScreen.TEST_TAG_PROGRESS_TEXT)
        .assertIsDisplayed()
        .assertTextEquals(
            String.format(
                context.getString(R.string.run_hike_screen_progress_percentage_format), 0))
    composeTestRule
        .onNodeWithTag(RunHikeScreen.TEST_TAG_CURRENT_ELEVATION_TEXT)
        .assertIsDisplayed()
        .onChildAt(1)
        .assertTextEquals(
            String.format(
                context.getString(R.string.run_hike_screen_value_format_current_elevation), 485))

    locationCallback.onLocationResult(
        LocationResult.create(
            listOf(
                Location("gps").apply {
                  latitude = 46.57808286327073
                  longitude = 6.551269708196024
                })))

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(RunHikeScreen.TEST_TAG_PROGRESS_TEXT)
        .assertIsDisplayed()
        .assertTextEquals(
            String.format(
                context.getString(R.string.run_hike_screen_progress_percentage_format), 5))
    composeTestRule
        .onNodeWithTag(RunHikeScreen.TEST_TAG_CURRENT_ELEVATION_TEXT)
        .assertIsDisplayed()
        .onChildAt(1)
        .assertTextEquals(
            String.format(
                context.getString(R.string.run_hike_screen_value_format_current_elevation), 485))

    locationCallback.onLocationResult(
        LocationResult.create(
            listOf(
                Location("gps").apply {
                  latitude = 46.579277394466864
                  longitude = 6.543243182558365
                })))

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(RunHikeScreen.TEST_TAG_PROGRESS_TEXT)
        .assertIsDisplayed()
        .assertTextEquals(
            String.format(
                context.getString(R.string.run_hike_screen_progress_percentage_format), 62))
    composeTestRule
        .onNodeWithTag(RunHikeScreen.TEST_TAG_CURRENT_ELEVATION_TEXT)
        .assertIsDisplayed()
        .onChildAt(1)
        .assertTextEquals(
            String.format(
                context.getString(R.string.run_hike_screen_value_format_current_elevation), 476))

    // ---- RunHike Screen ----

    composeTestRule
        .onNodeWithTag(RunHikeScreen.TEST_TAG_STOP_HIKE_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // ---- Back to HikeDetail Screen by stopping the run----

    composeTestRule.waitUntilExactlyOneExists(
        hasTestTag(HikeDetailScreen.TEST_TAG_MAP), timeoutMillis = 10000)
  }
}
