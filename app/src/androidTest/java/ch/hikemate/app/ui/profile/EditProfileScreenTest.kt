package ch.hikemate.app.ui.profile

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import ch.hikemate.app.R
import ch.hikemate.app.model.authentication.AuthRepository
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.model.profile.HikingLevel
import ch.hikemate.app.model.profile.Profile
import ch.hikemate.app.model.profile.ProfileRepository
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.ui.components.BackButton
import ch.hikemate.app.ui.components.CenteredErrorAction
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import com.google.firebase.Timestamp
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

class EditProfileScreenTest : TestCase() {
  private lateinit var profileRepository: ProfileRepository
  private lateinit var profileViewModel: ProfileViewModel
  private lateinit var navigationActions: NavigationActions
  private lateinit var context: Context
  private lateinit var authViewModel: AuthViewModel
  private lateinit var authRepository: AuthRepository

  @get:Rule val composeTestRule = createComposeRule()

  private val profile =
      Profile(
          id = "1",
          name = "John Doe",
          email = "john-doe@gmail.com",
          hikingLevel = HikingLevel.INTERMEDIATE,
          joinedDate = Timestamp.now())

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    authRepository = mock(AuthRepository::class.java)
    navigationActions = mock(NavigationActions::class.java)
    profileRepository = mock(ProfileRepository::class.java)
    authViewModel = AuthViewModel(authRepository, profileRepository)
    profileViewModel = ProfileViewModel(profileRepository)

    `when`(profileRepository.getProfileById(eq(profile.id), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(Profile) -> Unit>(1)
      onSuccess(profile)
    }
    profileViewModel.getProfileById(profile.id)
  }

  @Test
  fun isEverythingDisplayed() {
    composeTestRule.setContent {
      EditProfileScreen(
          profileViewModel = profileViewModel,
          navigationActions = navigationActions,
          authViewModel = authViewModel)
    }
    composeTestRule.onNodeWithTag(BackButton.BACK_BUTTON_TEST_TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditProfileScreen.TEST_TAG_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditProfileScreen.TEST_TAG_NAME_INPUT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditProfileScreen.TEST_TAG_HIKING_LEVEL_LABEL).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_BEGINNER)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_INTERMEDIATE)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_EXPERT)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditProfileScreen.TEST_TAG_SAVE_BUTTON).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_MESSAGE)
        .assertIsNotDisplayed()
  }

  @Test
  fun checkCorrectProfileInfo() {
    composeTestRule.setContent {
      EditProfileScreen(
          profileViewModel = profileViewModel,
          navigationActions = navigationActions,
          authViewModel = authViewModel)
    }
    `when`(profileRepository.getProfileById(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(Profile) -> Unit>(1)
      onSuccess(profile)
    }
    profileViewModel.getProfileById(profile.id)

    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_NAME_INPUT)
        .assertTextContains(profile.name)

    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_INTERMEDIATE)
        .assertIsSelected()
    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_BEGINNER)
        .assertIsNotSelected()
    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_EXPERT)
        .assertIsNotSelected()
    composeTestRule
        .onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_MESSAGE)
        .assertIsNotDisplayed()
  }

  @Test
  fun checkCanGoBack() {
    composeTestRule.setContent {
      EditProfileScreen(
          profileViewModel = profileViewModel,
          navigationActions = navigationActions,
          authViewModel = authViewModel)
    }
    composeTestRule.onNodeWithTag(BackButton.BACK_BUTTON_TEST_TAG).performClick()
    composeTestRule
        .onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_MESSAGE)
        .assertIsNotDisplayed()
    verify(navigationActions).goBack()
  }

  @Test
  fun checkInputsAreEditable() {
    composeTestRule.setContent {
      EditProfileScreen(
          profileViewModel = profileViewModel,
          navigationActions = navigationActions,
          authViewModel = authViewModel)
    }
    composeTestRule.onNodeWithTag(EditProfileScreen.TEST_TAG_NAME_INPUT).performClick()
    composeTestRule.onNodeWithTag(EditProfileScreen.TEST_TAG_NAME_INPUT).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_NAME_INPUT)
        .performTextInput("Jane Doe")
    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_NAME_INPUT)
        .assertTextContains("Jane Doe")

    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_BEGINNER)
        .performClick()
    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_BEGINNER)
        .assertIsSelected()
    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_INTERMEDIATE)
        .assertIsNotSelected()
    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_EXPERT)
        .assertIsNotSelected()

    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_EXPERT)
        .performClick()
    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_EXPERT)
        .assertIsSelected()
    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_BEGINNER)
        .assertIsNotSelected()
    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_INTERMEDIATE)
        .assertIsNotSelected()

    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_INTERMEDIATE)
        .performClick()
    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_INTERMEDIATE)
        .assertIsSelected()
    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_BEGINNER)
        .assertIsNotSelected()
    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_EXPERT)
        .assertIsNotSelected()
    composeTestRule
        .onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_MESSAGE)
        .assertIsNotDisplayed()
  }

  @Test
  fun errorIsShownWhenEmptyProfileReturnedAndUserCanGoBackHome() {
    `when`(profileRepository.getProfileById(any(), any(), any())).thenAnswer {
      val onError = it.getArgument<(Exception) -> Unit>(2)
      onError(Exception("No profile found"))
    }
    `when`(authRepository.signOut(any())).thenAnswer {
      val onSuccess = it.getArgument<() -> Unit>(0)
      onSuccess()
    }

    profileViewModel.getProfileById(profile.id)

    composeTestRule.setContent {
      EditProfileScreen(
          profileViewModel = profileViewModel,
          navigationActions = navigationActions,
          authViewModel = authViewModel)
    }

    composeTestRule
        .onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_MESSAGE)
        .assertIsDisplayed()
        .assertTextEquals(context.getString(R.string.an_error_occurred_while_fetching_the_profile))
    composeTestRule.onNodeWithTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_BUTTON).performClick()

    verify(authRepository).signOut(any())
    verify(navigationActions).navigateTo(Route.AUTH)
    assertNull(authViewModel.currentUser.value)
  }
}
