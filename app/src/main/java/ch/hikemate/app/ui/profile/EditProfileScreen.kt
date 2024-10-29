package ch.hikemate.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import ch.hikemate.app.model.profile.HikingLevel
import ch.hikemate.app.model.profile.Profile
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.ui.components.BackButton
import ch.hikemate.app.ui.components.BigButton
import ch.hikemate.app.ui.components.ButtonType
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.theme.primaryColor
import com.google.firebase.Timestamp

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
@Composable
fun EditProfileScreen(
    navigationActions: NavigationActions,
    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
) {
  val context = LocalContext.current

  // TODO: show an error if the profile is null. For now display it for test purposes
  val profileState = profileViewModel.profile.collectAsState()

  val profile: Profile =
      profileState.value
          ?: Profile(
              "custom-id",
              "John Doe",
              "john.doe@gmail.com",
              HikingLevel.INTERMEDIATE,
              Timestamp.now())

  var name by remember { mutableStateOf(profile.name) }
  var hikingLevel by remember {
    mutableIntStateOf(HikingLevel.values().indexOf(profile.hikingLevel))
  }

  Column(
      modifier =
          Modifier.testTag(Screen.EDIT_PROFILE)
              .padding(
                  // Add padding to the sidebar padding
                  start = 16.dp,
                  end = 16.dp,
                  top = 16.dp,
              ),
      verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BackButton(navigationActions)
        Text(
            context.getString(R.string.edit_profile_screen_title),
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp),
            modifier = Modifier.testTag(EditProfileScreen.TEST_TAG_TITLE))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().testTag(EditProfileScreen.TEST_TAG_NAME_INPUT),
            colors =
                OutlinedTextFieldDefaults.colors()
                    .copy(
                        focusedLabelColor = primaryColor,
                        focusedIndicatorColor = primaryColor,
                        cursorColor = primaryColor,
                        textSelectionColors =
                            TextSelectionColors(
                                handleColor = primaryColor,
                                backgroundColor = primaryColor,
                            )),
            value = name,
            onValueChange = { name = it },
            label = { Text(context.getString(R.string.profile_screen_name_label)) })

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
                Text(
                    when (fitLevel) {
                      HikingLevel.BEGINNER ->
                          context.getString(R.string.profile_screen_hiking_level_choice_beginner)
                      HikingLevel.INTERMEDIATE ->
                          context.getString(
                              R.string.profile_screen_hiking_level_choice_intermediate)
                      HikingLevel.EXPERT ->
                          context.getString(R.string.profile_screen_hiking_level_choice_expert)
                    })
              }
            }
          }
        }

        BigButton(
            modifier = Modifier.fillMaxWidth().testTag(EditProfileScreen.TEST_TAG_SAVE_BUTTON),
            buttonType = ButtonType.PRIMARY,
            label = context.getString(R.string.edit_profile_screen_save_button_text),
            onClick = {
              // TODO: Save the profile to the database
              // profileViewModel.updateProfile(profile = Profile(profile.id, name, profile.email,
              // HikingLevel.values()[hikingLevel], profile.joinedDate))
              navigationActions.goBack()
            })
      }
}
