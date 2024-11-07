package ch.hikemate.app.ui.auth

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.hikemate.app.R
import ch.hikemate.app.ui.components.AppIcon
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.navigation.TopLevelDestinations
import ch.hikemate.app.ui.theme.kaushanTitleFontFamily
import ch.hikemate.app.ui.theme.primaryColor

object SignInScreen {
  const val TEST_TAG_TITLE = "sign_in_title"
  const val TEST_TAG_SIGN_IN_WITH_EMAIL = "sign_in_with_email_button"
  const val TEST_TAG_SIGN_IN_WITH_GOOGLE = "sign_in_with_google_button"
}

/** A composable function to display the sign in screen */
@Composable
fun SignInScreen(navigationActions: NavigationActions) {
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
                  modifier = Modifier.testTag(SignInScreen.TEST_TAG_TITLE),
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
                // TODO: Implement the sign in with Google functionality
                // This bypasses all security and should not be used in production
                navigationActions.navigateTo(TopLevelDestinations.MAP)
              }
            }
          }
        }
      },
  )
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
                  color = MaterialTheme.colorScheme.onSurface, // Text color
                  fontSize = 18.sp, // Font size
                  fontWeight = FontWeight.Bold,
              )
            }
      }
}
