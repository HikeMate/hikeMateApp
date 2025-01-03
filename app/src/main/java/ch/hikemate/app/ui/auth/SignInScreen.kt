package ch.hikemate.app.ui.auth

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.hikemate.app.R
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.ui.components.AppIcon
import ch.hikemate.app.ui.components.AsyncStateHandler
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.navigation.TopLevelDestinations
import ch.hikemate.app.ui.theme.primaryColor

object SignInScreen {
  const val TEST_TAG_TITLE = "sign_in_title"
  const val TEST_TAG_SIGN_IN_WITH_EMAIL = "sign_in_with_email_button"
  const val TEST_TAG_SIGN_IN_WITH_GOOGLE = "sign_in_with_google_button"
}

/** A composable function to display the sign in screen */
@Composable
fun SignInScreen(
    navigationActions: NavigationActions,
    authViewModel: AuthViewModel,
) {

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  // Create the launcher for adding a Google account in case there is no Google account connected
  // to the device. Necessary since the Google sign-in process requires a Google account to be
  // connected to the device, otherwise the sign-in process will fail with a No Credential Exception
  val addAccountLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            Toast.makeText(
                    context,
                    context.getString(R.string.google_account_connection_ended_or_cancelled),
                    Toast.LENGTH_LONG)
                .show()
            // startAddAccountIntentLauncher is null, since it is only called when the user has no
            // Google account connected to the device, however they just added one.
            authViewModel.signInWithGoogle(coroutineScope, context, null)
            Log.d("MainActivity", "addAccountLauncher result: $result")
          }

  val errorMessageIdState = authViewModel.errorMessageId.collectAsState()
  val loadingState = authViewModel.loading.collectAsState()

  // If the user is already signed in, navigate to the map screen
  LaunchedEffect(authViewModel.currentUser.collectAsState().value) {
    if (authViewModel.isUserLoggedIn()) {
      navigationActions.navigateTo(TopLevelDestinations.MAP)
    }
  }

  AsyncStateHandler(
      errorMessageIdState = errorMessageIdState,
      actionContentDescriptionStringId = R.string.retry,
      actionOnErrorAction = { authViewModel.clearErrorMessage() },
      loadingState = loadingState,
  ) {
    Scaffold(
        modifier = Modifier.fillMaxSize().testTag(Screen.AUTH),
        content = { padding ->
          Box(
              modifier = Modifier.fillMaxSize(),
          ) {
            // I created a box to make the background image take a little more space
            // than the screen size, so that the blur effect doesn't show white edges
            Box(
                modifier = Modifier.fillMaxSize().clipToBounds().scale(1.1f),
            ) {
              Image(
                  painter = painterResource(id = R.drawable.sign_in_background),
                  contentDescription =
                      stringResource(R.string.background_image_content_description),
                  modifier = Modifier.fillMaxSize().blur(10.dp),
                  contentScale = ContentScale.Crop,
              )
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround,
            ) {
              Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                // App Logo Image
                AppIcon(200.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // App name Text
                Text(
                    modifier = Modifier.testTag(SignInScreen.TEST_TAG_TITLE),
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displayLarge)
              }

              // Sign in with email button
              Column {
                SignInButton(
                    text = stringResource(R.string.sign_in_with_email),
                    icon = R.drawable.app_icon,
                    modifier = Modifier.testTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_EMAIL),
                ) {
                  navigationActions.navigateTo(Screen.SIGN_IN_WITH_EMAIL)
                }

                // Sign in with Google button
                SignInButton(
                    text = stringResource(R.string.sign_in_with_google),
                    icon = R.drawable.google_logo,
                    modifier = Modifier.testTag(SignInScreen.TEST_TAG_SIGN_IN_WITH_GOOGLE),
                ) {
                  authViewModel.signInWithGoogle(
                      coroutineScope = coroutineScope,
                      context = context,
                      startAddAccountIntentLauncher = addAccountLauncher)
                }
              }
            }
          }
        },
    )
  }
}

/**
 * A composable function to display the sign in with an icon
 *
 * @param onSignInClick A lambda function to handle the sign in click event
 * @param icon The resource ID of the icon to display on the button
 */
@Composable
fun SignInButton(
    icon: Int,
    text: String,
    modifier: Modifier = Modifier,
    onSignInClick: () -> Unit
) {
  Button(
      onClick = onSignInClick,
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(50),
      modifier =
          modifier
              .padding(8.dp)
              .height(48.dp)
              .border(
                  width = 3.dp, color = primaryColor, shape = RoundedCornerShape(size = 32.dp))) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()) {
              // Load the Google logo from resources
              Image(
                  painter = painterResource(id = icon),
                  contentDescription = null,
                  modifier = Modifier.size(30.dp).padding(end = 8.dp))

              // Text for the button
              Text(
                  text = text,
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurface,
              )
            }
      }
}
