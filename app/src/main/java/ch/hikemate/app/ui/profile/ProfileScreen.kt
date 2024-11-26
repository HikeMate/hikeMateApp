package ch.hikemate.app.ui.profile

import android.icu.text.DateFormat
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.hikemate.app.R
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.model.profile.HikingLevel
import ch.hikemate.app.model.profile.ProfileViewModel
import ch.hikemate.app.ui.components.AsyncStateHandler
import ch.hikemate.app.ui.components.BigButton
import ch.hikemate.app.ui.components.ButtonType
import ch.hikemate.app.ui.navigation.BottomBarNavigation
import ch.hikemate.app.ui.navigation.LIST_TOP_LEVEL_DESTINATIONS
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen

object ProfileScreen {
  const val TEST_TAG_TITLE = "profileScreenTitle"
  const val TEST_TAG_NAME = "profileScreenNameInfo"
  const val TEST_TAG_EMAIL = "profileScreenEmailInfo"
  const val TEST_TAG_HIKING_LEVEL = "profileScreenHikingLevelInfo"
  const val TEST_TAG_JOIN_DATE = "profileScreenJoinDateInfo"
  const val TEST_TAG_EDIT_PROFILE_BUTTON = "profileScreenEditProfileButton"
  const val TEST_TAG_SIGN_OUT_BUTTON = "profileScreenSignOutButton"
  const val TEST_TAG_DELETE_ACCOUNT_BUTTON = "profileScreenDeleteAccountButton"
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
    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
    authViewModel: AuthViewModel
) {
  val context = LocalContext.current

  LaunchedEffect(Unit) {
    if (authViewModel.currentUser.value == null) {
      Log.e("ProfileScreen", "User is not signed in")
      return@LaunchedEffect
    }
    profileViewModel.getProfileById(authViewModel.currentUser.value!!.uid)
  }

  val errorMessageIdState = profileViewModel.errorMessageId.collectAsState()
  val profileState = profileViewModel.profile.collectAsState()

  AsyncStateHandler(
      errorMessageIdState = errorMessageIdState,
      actionContentDescriptionStringId = R.string.go_back,
      actionOnErrorAction = { navigationActions.navigateTo(Route.MAP) },
      valueState = profileState,
  ) { profile ->
    BottomBarNavigation(
        onTabSelect = { navigationActions.navigateTo(it) },
        tabList = LIST_TOP_LEVEL_DESTINATIONS,
        selectedItem = Route.PROFILE) { _ ->
          Column(
              modifier =
                  Modifier.testTag(Screen.PROFILE)
                      .padding(
                          start = 16.dp,
                          end = 16.dp,
                      )
                      .safeDrawingPadding(),
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
                    (when (profile.hikingLevel) {
                      HikingLevel.BEGINNER ->
                          context.getString(R.string.profile_screen_hiking_level_info_beginner)
                      HikingLevel.INTERMEDIATE ->
                          context.getString(R.string.profile_screen_hiking_level_info_intermediate)
                      HikingLevel.EXPERT ->
                          context.getString(R.string.profile_screen_hiking_level_info_expert)
                    }),
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
                      authViewModel.signOut({ navigationActions.navigateTo(Screen.AUTH) })
                    },
                    Modifier.testTag(ProfileScreen.TEST_TAG_SIGN_OUT_BUTTON))

                TextButton(
                    modifier =
                        Modifier.fillMaxWidth()
                            .testTag(ProfileScreen.TEST_TAG_DELETE_ACCOUNT_BUTTON),
                    onClick = { navigationActions.navigateTo(Screen.DELETE_ACCOUNT) }) {
                      Text(
                          "Delete Account",
                          style =
                              TextStyle(
                                  textAlign = TextAlign.Center,
                                  fontWeight = FontWeight.Bold,
                                  fontSize = 20.sp,
                                  color = MaterialTheme.colorScheme.error),
                      )
                    }
              }
        }
  }
}
