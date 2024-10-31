package ch.hikemate.app.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import ch.hikemate.app.model.profile.HikingLevel
import ch.hikemate.app.model.profile.Profile
import ch.hikemate.app.model.profile.ProfileRepository
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.ui.components.BackButton
import ch.hikemate.app.ui.navigation.NavigationActions
import com.google.firebase.Timestamp
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.verify

class EditProfileScreenTest : TestCase() {
  private lateinit var profileRepository: ProfileRepository
  private lateinit var profileViewModel: ProfileViewModel
  private lateinit var navigationActions: NavigationActions

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    navigationActions = mock(NavigationActions::class.java)
    profileRepository = mock(ProfileRepository::class.java)
    profileViewModel = ProfileViewModel(profileRepository)

    composeTestRule.setContent {
      EditProfileScreen(profileViewModel = profileViewModel, navigationActions = navigationActions)
    }
  }

  @Test
  fun isEverythingDisplayed() {
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
  }

  @Test
  fun checkCanGoBack() {
    composeTestRule.onNodeWithTag(BackButton.BACK_BUTTON_TEST_TAG).performClick()
    verify(navigationActions).goBack()
  }

  @Test
  fun checkInputsAreEditable() {
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
  }
}
