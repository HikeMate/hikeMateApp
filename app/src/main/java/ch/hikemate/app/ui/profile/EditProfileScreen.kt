package ch.hikemate.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.hikemate.app.R
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.model.profile.HikingLevel
import ch.hikemate.app.model.profile.Profile
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.ui.components.AsyncStateHandler
import ch.hikemate.app.ui.components.BackButton
import ch.hikemate.app.ui.components.BigButton
import ch.hikemate.app.ui.components.ButtonType
import ch.hikemate.app.ui.components.CenteredLoadingAnimation
import ch.hikemate.app.ui.components.CustomTextField
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.theme.primaryColor

object EditProfileScreen {
  const val TEST_TAG_TITLE = "editProfileScreenTitle"
  const val TEST_TAG_NAME_INPUT = "editProfileScreenNameInput"
  const val TEST_TAG_HIKING_LEVEL_LABEL = "editProfileScreenHikingLevelLabel"
  const val TEST_TAG_HIKING_LEVEL_CHOICE_BEGINNER = "editProfileScreenHikingLevelChoiceBeginner"
  const val TEST_TAG_HIKING_LEVEL_CHOICE_INTERMEDIATE =
      "editProfileScreenHikingLevelChoiceIntermediate"
  const val TEST_TAG_HIKING_LEVEL_CHOICE_EXPERT = "editProfileScreenHikingLevelChoiceExpert"
  const val TEST_TAG_SAVE_BUTTON = "editProfileScreenSaveButton"
}

/**
 * A composable that displays the edit profile screen.
 *
 * @param navigationActions The navigation actions.
 * @param profileViewModel The profile view model.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    navigationActions: NavigationActions,
    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
    authViewModel: AuthViewModel
) {
  val context = LocalContext.current

  val scrollState = rememberScrollState()

  val errorMessageIdState = profileViewModel.errorMessageId.collectAsState()
  val profileState = profileViewModel.profile.collectAsState()

  AsyncStateHandler(
      errorMessageIdState = errorMessageIdState,
      actionContentDescriptionStringId = R.string.go_back,
      // Whenever there's an error the user needs to re-authenticate
      // thus forcing him to sign out and navigate to the Auth screen
      actionOnErrorAction = { authViewModel.signOut { navigationActions.navigateTo(Route.AUTH) } },
      valueState = profileState,
  ) { profile ->
    var name by remember { mutableStateOf(profile.name) }
    var hikingLevel by remember {
      mutableIntStateOf(HikingLevel.values().indexOf(profile.hikingLevel))
    }

    var savedProfile by remember { mutableStateOf<Profile?>(null) }

    var isLoading by remember { mutableStateOf(false) }

    if (savedProfile == profile) {
      isLoading = false
      savedProfile = null
      navigationActions.goBack()
      return@AsyncStateHandler
    }

    if (isLoading) {
      // Display a loading animation while the profile is being saved
      CenteredLoadingAnimation()
    }

    Column(
        modifier =
            Modifier.testTag(Screen.EDIT_PROFILE)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                )
                .imeNestedScroll()
                .safeDrawingPadding()
                .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
          BackButton(navigationActions)
          Text(
              context.getString(R.string.edit_profile_screen_title),
              style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp),
              modifier = Modifier.testTag(EditProfileScreen.TEST_TAG_TITLE))

          CustomTextField(
              value = name,
              onValueChange = { name = it },
              label = context.getString(R.string.profile_screen_name_label),
              maxLength = CustomTextField.MAX_NAME_LENGTH,
              modifier = Modifier.testTag(EditProfileScreen.TEST_TAG_NAME_INPUT),
          )

          Column(
              verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
                context.getString(R.string.profile_screen_hiking_level_label),
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                modifier = Modifier.testTag(EditProfileScreen.TEST_TAG_HIKING_LEVEL_LABEL))
            SingleChoiceSegmentedButtonRow {
              HikingLevel.values().forEachIndexed { index, fitLevel ->
                SegmentedButton(
                    modifier =
                        Modifier.testTag(
                            when (fitLevel) {
                              HikingLevel.BEGINNER ->
                                  EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_BEGINNER
                              HikingLevel.INTERMEDIATE ->
                                  EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_INTERMEDIATE
                              HikingLevel.EXPERT ->
                                  EditProfileScreen.TEST_TAG_HIKING_LEVEL_CHOICE_EXPERT
                            }),
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index, count = HikingLevel.values().size),
                    colors =
                        SegmentedButtonDefaults.colors()
                            .copy(
                                activeContainerColor = primaryColor,
                                activeContentColor = Color.White,
                            ),
                    onClick = { hikingLevel = index },
                    selected = hikingLevel == index,
                ) {
                  Text(fitLevel.getDisplayString(context))
                }
              }
            }
          }

          BigButton(
              modifier = Modifier.fillMaxWidth().testTag(EditProfileScreen.TEST_TAG_SAVE_BUTTON),
              buttonType = ButtonType.PRIMARY,
              label = context.getString(R.string.edit_profile_screen_save_button_text),
              onClick = {
                savedProfile =
                    Profile(
                        profile.id,
                        name,
                        profile.email,
                        HikingLevel.values()[hikingLevel],
                        profile.joinedDate)
                profileViewModel.updateProfile(profile = savedProfile!!)
                isLoading = true
              })
        }
  }
}
