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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.hikemate.app.R
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.ui.components.AppIcon
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.navigation.TopLevelDestinations
import ch.hikemate.app.ui.theme.kaushanTitleFontFamily
import ch.hikemate.app.ui.theme.primaryColor
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.StateFlow

const val TEST_TAG_LOGIN_BUTTON = "loginButton"

private const val CONNECTED_ACCOUNT_MESSAGE =
    "Connected Google Account to your device successfully. Please wait while we retry the signup."

/** A composable function to display the sign in screen */
@Composable
fun SignInScreen(
    navigationActions: NavigationActions,
    authViewModel: AuthViewModel,
    currUserStateFlow: StateFlow<FirebaseUser?> = authViewModel.currentUser
) {

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val currUser = currUserStateFlow.collectAsState().value

  // Create the launcher for adding a Google account in case there is no Google account connected
  // to the device. Necessary since the Google sign-in process requires a Google account to be
  // connected to the device, otherwise the sign-in process will fail with a No Credential Exception
  val addAccountLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            Toast.makeText(
                    context,
                CONNECTED_ACCOUNT_MESSAGE,
                    Toast.LENGTH_LONG)
                .show()
            // startAddAccountIntentLauncher is null, since it is only called when the user has no
            // Google account connected to the device, however they just added one.
            authViewModel.signInWithGoogle(coroutineScope, context, null)
            Log.d("MainActivity", "addAccountLauncher result: $result")
          }

  // If the user is already signed in, navigate to the map screen
  LaunchedEffect(currUser) {
    if (currUser != null) {
      navigationActions.navigateTo(TopLevelDestinations.MAP)
    }
  }

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
                contentDescription = "Background Image",
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
                  modifier = Modifier.testTag("appNameText"),
                  text = "HikeMate",
                  style =
                      TextStyle(
                          color = Color.White,
                          fontFamily = kaushanTitleFontFamily,
                          fontSize = 60.sp,
                          fontWeight = FontWeight.Bold,
                      ),
              )
            }
            SignInWithGoogleButton {
              authViewModel.signInWithGoogle(
                  coroutineScope = coroutineScope,
                  context = context,
                  startAddAccountIntentLauncher = addAccountLauncher)
            }
          }
        }
      },
  )
}

/**
 * A composable function to display the sign in with Google button
 *
 * @param onSignInClick A lambda function to handle the sign in click event
 */
@Composable
fun SignInWithGoogleButton(onSignInClick: () -> Unit) {
  Button(
      onClick = onSignInClick,
      colors = ButtonDefaults.buttonColors(containerColor = Color.White),
      shape = RoundedCornerShape(50),
      modifier =
          Modifier.padding(8.dp)
              .height(48.dp)
              .border(width = 3.dp, color = primaryColor, shape = RoundedCornerShape(size = 32.dp))
              .testTag(TEST_TAG_LOGIN_BUTTON)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()) {
              // Load the Google logo from resources
              Image(
                  painter = painterResource(id = R.drawable.google_logo),
                  contentDescription = "Google Logo",
                  modifier = Modifier.size(30.dp).padding(end = 8.dp))

              // Text for the button
              Text(
                  text = "Sign In with Google",
                  color = Color.Black, // Text color
                  fontSize = 18.sp, // Font size
                  fontWeight = FontWeight.Bold,
              )
            }
      }
}
