package ch.hikemate.app.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.hikemate.app.model.profile.HikingLevel
import ch.hikemate.app.model.profile.Profile
import ch.hikemate.app.model.profile.ProfileRepository
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.ui.components.BACK_BUTTON_TEST_TAG
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
    composeTestRule.onNodeWithTag(BACK_BUTTON_TEST_TAG).assertIsDisplayed()
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
      @Suppress("UNCHECKED_CAST") val callback = it.arguments[1] as (Profile) -> Unit
      callback(profile)
    }
    profileViewModel.getProfileById("1")

    composeTestRule
        .onNodeWithTag(EditProfileScreen.TEST_TAG_NAME_INPUT)
        .assertTextContains(profile.name)
    // I didn't find a way to test the hiking level as it is a segmented button group
    // and I couldn't find a way to get the selected button
  }

  @Test
  fun checkCanGoBack() {
    composeTestRule.onNodeWithTag(BACK_BUTTON_TEST_TAG).performClick()
    verify(navigationActions).goBack()
  }
}
