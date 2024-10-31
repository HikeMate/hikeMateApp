package ch.hikemate.app.ui.profile

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import ch.hikemate.app.R
import ch.hikemate.app.model.authentication.AuthRepository
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.model.profile.HikingLevel
import ch.hikemate.app.model.profile.Profile
import ch.hikemate.app.model.profile.ProfileRepository
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.navigation.TEST_TAG_DRAWER_CONTENT
import ch.hikemate.app.ui.navigation.TEST_TAG_SIDEBAR_BUTTON
import com.google.firebase.Timestamp
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import java.time.LocalDate
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class ProfileScreenTest : TestCase() {
  private lateinit var authRepository: AuthRepository
  private lateinit var profileRepository: ProfileRepository
  private lateinit var profileViewModel: ProfileViewModel
  private lateinit var authViewModel: AuthViewModel
  private lateinit var navigationActions: NavigationActions

  private lateinit var context: Context

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()

    navigationActions = mock(NavigationActions::class.java)
    authRepository = mock(AuthRepository::class.java)
    profileRepository = mock(ProfileRepository::class.java)
    authViewModel = AuthViewModel(authRepository)
    profileViewModel = ProfileViewModel(profileRepository)

    composeTestRule.setContent {
      ProfileScreen(
          profileViewModel = profileViewModel,
          navigationActions = navigationActions,
          authViewModel = authViewModel)
    }
  }

  @Test
  fun isEverythingDisplayed() {
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreen.TEST_TAG_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreen.TEST_TAG_NAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreen.TEST_TAG_EMAIL).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreen.TEST_TAG_HIKING_LEVEL).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreen.TEST_TAG_JOIN_DATE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreen.TEST_TAG_EDIT_PROFILE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreen.TEST_TAG_SIGN_OUT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun checkCorrectProfileInfo() {
    val profile =
        Profile(
            id = "1",
            name = "John Doe",
            email = "john.doe@gmail.com",
            hikingLevel = HikingLevel.INTERMEDIATE,
            joinedDate = Timestamp.now())
    `when`(profileRepository.getProfileById(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(Profile) -> Unit>(1)
      onSuccess(profile)
    }
    profileViewModel.getProfileById(profile.id)

    composeTestRule.onNodeWithTag(ProfileScreen.TEST_TAG_NAME).assertTextEquals(profile.name)
    composeTestRule.onNodeWithTag(ProfileScreen.TEST_TAG_EMAIL).assertTextEquals(profile.email)
    composeTestRule
        .onNodeWithTag(ProfileScreen.TEST_TAG_HIKING_LEVEL)
        .assertTextEquals(
            (when (profile.hikingLevel) {
              HikingLevel.BEGINNER ->
                  context.getString(R.string.profile_screen_hiking_level_info_beginner)
              HikingLevel.INTERMEDIATE ->
                  context.getString(R.string.profile_screen_hiking_level_info_intermediate)
              HikingLevel.EXPERT ->
                  context.getString(R.string.profile_screen_hiking_level_info_expert)
            }))

    val current = LocalDate.now()
    val currentDateDayOfMonth = current.dayOfMonth
    val currentDateMonthText = current.month.toString().lowercase()

    composeTestRule
        .onNodeWithTag(ProfileScreen.TEST_TAG_JOIN_DATE)
        .assertTextContains(currentDateDayOfMonth.toString(), substring = true, ignoreCase = true)
    composeTestRule
        .onNodeWithTag(ProfileScreen.TEST_TAG_JOIN_DATE)
        .assertTextContains(currentDateMonthText, substring = true, ignoreCase = true)
  }

  @Test
  fun checkExpertHikingLevel() {
    val profile =
        Profile(
            id = "1",
            name = "John Doe",
            email = "john.doe@gmail.com",
            hikingLevel = HikingLevel.EXPERT,
            joinedDate = Timestamp.now())
    `when`(profileRepository.getProfileById(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(Profile) -> Unit>(1)
      onSuccess(profile)
    }

    profileViewModel.getProfileById(profile.id)

    composeTestRule
        .onNodeWithTag(ProfileScreen.TEST_TAG_HIKING_LEVEL)
        .assertTextEquals(
            (when (profile.hikingLevel) {
              HikingLevel.BEGINNER ->
                  context.getString(R.string.profile_screen_hiking_level_info_beginner)
              HikingLevel.INTERMEDIATE ->
                  context.getString(R.string.profile_screen_hiking_level_info_intermediate)
              HikingLevel.EXPERT ->
                  context.getString(R.string.profile_screen_hiking_level_info_expert)
            }))
  }

  @Test
  fun checkBeginnerHikingLevel() {
    val profile =
        Profile(
            id = "1",
            name = "John Doe",
            email = "john.doe@gmail.com",
            hikingLevel = HikingLevel.BEGINNER,
            joinedDate = Timestamp.now())
    `when`(profileRepository.getProfileById(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(Profile) -> Unit>(1)
      onSuccess(profile)
    }
    profileViewModel.getProfileById(profile.id)

    composeTestRule
        .onNodeWithTag(ProfileScreen.TEST_TAG_HIKING_LEVEL)
        .assertTextEquals(
            (when (profile.hikingLevel) {
              HikingLevel.BEGINNER ->
                  context.getString(R.string.profile_screen_hiking_level_info_beginner)
              HikingLevel.INTERMEDIATE ->
                  context.getString(R.string.profile_screen_hiking_level_info_intermediate)
              HikingLevel.EXPERT ->
                  context.getString(R.string.profile_screen_hiking_level_info_expert)
            }))
  }

  @Test
  fun checkSidebarCanOpen() {
    composeTestRule.onNodeWithTag(TEST_TAG_SIDEBAR_BUTTON).performClick()
    composeTestRule.onNodeWithTag(TEST_TAG_DRAWER_CONTENT).assertIsDisplayed()
  }

  @Test
  fun checkEditProfileButton() {
    composeTestRule.onNodeWithTag(ProfileScreen.TEST_TAG_EDIT_PROFILE_BUTTON).performClick()
    verify(navigationActions).navigateTo(Screen.EDIT_PROFILE)
  }

  @Test
  fun checkSignOutButton() {
    composeTestRule.onNodeWithTag(ProfileScreen.TEST_TAG_SIGN_OUT_BUTTON).performClick()
    verify(authRepository, times(1)).signOut(any())
  }
}
