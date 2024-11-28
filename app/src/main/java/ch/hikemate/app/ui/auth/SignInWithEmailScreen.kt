package ch.hikemate.app.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.hikemate.app.R
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.ui.components.BackButton
import ch.hikemate.app.ui.components.BigButton
import ch.hikemate.app.ui.components.ButtonType
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.theme.primaryColor

object SignInWithEmailScreen {
  const val TEST_TAG_TITLE = "sign_in_with_email_title"
  const val TEST_TAG_EMAIL_INPUT = "sign_in_with_email_name_input"
  const val TEST_TAG_PASSWORD_INPUT = "sign_in_with_email_password_input"
  const val TEST_TAG_SIGN_IN_BUTTON = "sign_in_with_email_sign_in_button"
  const val TEST_TAG_GO_TO_SIGN_UP_BUTTON = "sign_in_with_email_go_to_sign_up_button"
}

/**
 * A composable that displays the sign in with email screen.
 *
 * @param navigationActions The navigation actions.
 * @param authViewModel The authentication view model.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SignInWithEmailScreen(
    navigationActions: NavigationActions,
    authViewModel: AuthViewModel,
) {
  val context = LocalContext.current

  val scrollState = rememberScrollState()

  // Define the colors for the input fields
  val inputColors =
      OutlinedTextFieldDefaults.colors()
          .copy(
              focusedLabelColor = primaryColor,
              focusedIndicatorColor = primaryColor,
              cursorColor = primaryColor,
              textSelectionColors =
                  TextSelectionColors(
                      handleColor = primaryColor,
                      backgroundColor = primaryColor,
                  ))

  // Define the email and password state
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }

  Column(
      modifier =
          Modifier.testTag(Screen.SIGN_IN_WITH_EMAIL)
              .padding(
                  start = 16.dp,
                  end = 16.dp,
              )
              .imeNestedScroll()
              .safeDrawingPadding()
              .verticalScroll(scrollState)
              .height(IntrinsicSize.Max),
      verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BackButton(navigationActions)
        Text(
            stringResource(R.string.sign_in_with_email_title),
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp),
            modifier = Modifier.testTag(SignInWithEmailScreen.TEST_TAG_TITLE))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().testTag(SignInWithEmailScreen.TEST_TAG_EMAIL_INPUT),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = inputColors,
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.sign_in_with_email_email_label)) },
            singleLine = true)

        OutlinedTextField(
            modifier =
                Modifier.fillMaxWidth().testTag(SignInWithEmailScreen.TEST_TAG_PASSWORD_INPUT),
            visualTransformation = PasswordVisualTransformation(),
            colors = inputColors,
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.sign_in_with_email_password_label)) },
            singleLine = true)

        BigButton(
            modifier =
                Modifier.fillMaxWidth().testTag(SignInWithEmailScreen.TEST_TAG_SIGN_IN_BUTTON),
            buttonType = ButtonType.PRIMARY,
            label = stringResource(R.string.sign_in_with_email_sign_in_button),
            onClick = {
              authViewModel.signInWithEmailAndPassword(
                  email,
                  password,
                  onSuccess = {
                    // Navigate to the map screen
                    navigationActions.navigateTo(Route.MAP)
                  },
                  onErrorAction = {
                    // Show an error message in a toast
                    Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                  })
            })

        Column(
            modifier = Modifier.padding(bottom = 16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally) {
              TextButton(
                  onClick = {
                    // Navigate to the sign up screen
                    navigationActions.navigateTo(Screen.CREATE_ACCOUNT)
                  },
                  modifier = Modifier.testTag(SignInWithEmailScreen.TEST_TAG_GO_TO_SIGN_UP_BUTTON),
              ) {
                Text(
                    stringResource(R.string.sign_in_with_email_go_to_sign_up),
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                )
              }
            }
      }
}
