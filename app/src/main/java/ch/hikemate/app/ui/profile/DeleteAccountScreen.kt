package ch.hikemate.app.ui.profile

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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

object DeleteAccountScreen {
  const val TEST_TAG_TITLE = "delete_account_title"
  const val TEST_TAG_INFO_TEXT = "delete_account_info_text"
  const val TEST_TAG_PASSWORD_INPUT = "delete_account_password_input"
  const val TEST_TAG_DELETE_ACCOUNT_BUTTON = "delete_account_delete_account_button"
}

/**
 * A composable to display the delete account screen.
 *
 * @param navigationActions The navigation actions.
 * @param authViewModel The authentication view model.
 */
@Composable
fun DeleteAccountScreen(navigationActions: NavigationActions, authViewModel: AuthViewModel) {
  val context = LocalContext.current

  val passwordMustNotBeEmptyError =
      stringResource(R.string.delete_account_password_must_be_filled_error)

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

  var password by remember { mutableStateOf("") }

  Column(
      modifier =
          Modifier.testTag(Screen.SIGN_IN_WITH_EMAIL)
              .padding(
                  // Add padding to the sidebar padding
                  start = 16.dp,
                  end = 16.dp,
                  top = 16.dp,
              ),
      verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BackButton(navigationActions)
        Text(
            stringResource(R.string.delete_account_title),
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp),
            modifier = Modifier.testTag(DeleteAccountScreen.TEST_TAG_TITLE))

        Text(
            stringResource(R.string.delete_account_info),
            style = TextStyle(fontSize = 16.sp),
            modifier = Modifier.testTag(DeleteAccountScreen.TEST_TAG_INFO_TEXT))

        if (authViewModel.isEmailProvider())
            OutlinedTextField(
                modifier =
                    Modifier.fillMaxWidth().testTag(DeleteAccountScreen.TEST_TAG_PASSWORD_INPUT),
                visualTransformation = PasswordVisualTransformation(),
                colors = inputColors,
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.delete_account_password_label)) })

        BigButton(
            modifier =
                Modifier.fillMaxWidth().testTag(DeleteAccountScreen.TEST_TAG_DELETE_ACCOUNT_BUTTON),
            buttonType = ButtonType.PRIMARY,
            label = stringResource(R.string.delete_account_delete_button),
            onClick = {
              if (authViewModel.isEmailProvider() && password.isEmpty()) {
                Toast.makeText(context, passwordMustNotBeEmptyError, Toast.LENGTH_SHORT).show()
              } else {
                authViewModel.deleteAccount(
                    password,
                    context as Activity,
                    { navigationActions.navigateTo(Route.AUTH) },
                    { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() })
              }
            })
      }
}
