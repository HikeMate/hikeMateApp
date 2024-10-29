package ch.hikemate.app.ui.profile

import android.icu.text.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.hikemate.app.R
import ch.hikemate.app.model.profile.FitnessLevel
import ch.hikemate.app.model.profile.Profile
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.ui.components.BigButton
import ch.hikemate.app.ui.components.ButtonType
import ch.hikemate.app.ui.navigation.LIST_TOP_LEVEL_DESTINATIONS
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.navigation.SideBarNavigation
import com.google.firebase.Timestamp

object ProfileScreen {
  const val TEST_TAG_TITLE = "profileScreenTitle"
  const val TEST_TAG_NAME = "profileScreenNameInfo"
  const val TEST_TAG_EMAIL = "profileScreenEmailInfo"
  const val TEST_TAG_HIKING_LEVEL = "profileScreenHikingLevelInfo"
  const val TEST_TAG_JOIN_DATE = "profileScreenJoinDateInfo"
  const val TEST_TAG_EDIT_PROFILE_BUTTON = "profileScreenEditProfileButton"
  const val TEST_TAG_SIGN_OUT_BUTTON = "profileScreenSignOutButton"
}

/**
 * A composable to display an information of the profile.
 *
 * @param label The label of the information.
 * @param value The value of the information.
 */
@Composable
fun DisplayInfo(label: String, value: String, modifier: Modifier = Modifier) {
  Column(
      verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Text(label, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp))
    Text(value, style = TextStyle(fontSize = 20.sp), modifier = modifier)
  }
}

/**
 * A composable that displays the profile screen.
 *
 * @param navigationActions The navigation actions.
 * @param profileViewModel The profile view model.
 */
@Composable
fun ProfileScreen(
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
              FitnessLevel.INTERMEDIATE,
              Timestamp.now())

  SideBarNavigation(
      onTabSelect = { navigationActions.navigateTo(it) },
      tabList = LIST_TOP_LEVEL_DESTINATIONS,
      selectedItem = Route.PROFILE) { p ->
        Column(
            modifier =
                Modifier.testTag(Screen.PROFILE)
                    .padding(p)
                    .padding(
                        // Add padding to the sidebar padding
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                    ),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
              Text(
                  context.getString(R.string.profile_screen_title),
                  style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp),
                  modifier = Modifier.testTag(ProfileScreen.TEST_TAG_TITLE))

              DisplayInfo(
                  context.getString(R.string.profile_screen_name_label),
                  profile.name,
                  Modifier.testTag(ProfileScreen.TEST_TAG_NAME))
              DisplayInfo(
                  context.getString(R.string.profile_screen_email_label),
                  profile.email,
                  Modifier.testTag(ProfileScreen.TEST_TAG_EMAIL))
              DisplayInfo(
                  context.getString(R.string.profile_screen_hiking_level_label),
                  buildString {
                    append(context.getString(R.string.profile_screen_hiking_level_info_prefix))
                    append(" ")
                    append(
                        when (profile.fitnessLevel) {
                          FitnessLevel.BEGINNER ->
                              context.getString(R.string.profile_screen_hiking_level_info_beginner)
                          FitnessLevel.INTERMEDIATE ->
                              context.getString(
                                  R.string.profile_screen_hiking_level_info_intermediate)
                          FitnessLevel.EXPERT ->
                              context.getString(R.string.profile_screen_hiking_level_info_expert)
                        })
                    append(" ")
                    append(context.getString(R.string.profile_screen_hiking_level_info_suffix))
                  },
                  Modifier.testTag(ProfileScreen.TEST_TAG_HIKING_LEVEL))
              DisplayInfo(
                  context.getString(R.string.profile_screen_join_date_label),
                  DateFormat.getDateInstance(DateFormat.LONG).format(profile.joinedDate.toDate()),
                  Modifier.testTag(ProfileScreen.TEST_TAG_JOIN_DATE))

              BigButton(
                  buttonType = ButtonType.PRIMARY,
                  label = context.getString(R.string.profile_screen_edit_profile_button_text),
                  onClick = { navigationActions.navigateTo(Screen.EDIT_PROFILE) },
                  Modifier.testTag(ProfileScreen.TEST_TAG_EDIT_PROFILE_BUTTON))

              BigButton(
                  buttonType = ButtonType.SECONDARY,
                  label = context.getString(R.string.profile_screen_sign_out_button_text),
                  onClick = {
                    // TODO: Sign out
                  },
                  Modifier.testTag(ProfileScreen.TEST_TAG_SIGN_OUT_BUTTON))
            }
      }
}
