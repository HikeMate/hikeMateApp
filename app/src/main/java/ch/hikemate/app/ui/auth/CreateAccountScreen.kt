package ch.hikemate.app.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import ch.hikemate.app.R
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.ui.components.BackButton
import ch.hikemate.app.ui.components.BigButton
import ch.hikemate.app.ui.components.ButtonType
import ch.hikemate.app.ui.components.CustomTextField
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen

object CreateAccountScreen {
  const val TEST_TAG_TITLE = "create_account_title"
  const val TEST_TAG_NAME_INPUT = "create_account_name_input"
  const val TEST_TAG_EMAIL_INPUT = "create_account_email_input"
  const val TEST_TAG_PASSWORD_INPUT = "create_account_password_input"
  const val TEST_TAG_CONFIRM_PASSWORD_INPUT = "create_account_confirm_password_input"
  const val TEST_TAG_SIGN_UP_BUTTON = "create_account_sign_up_button"
}

/**
 * A composable that displays the create account.
 *
 * @param navigationActions The navigation actions.
 * @param authViewModel The authentication view model.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateAccountScreen(
    navigationActions: NavigationActions,
    authViewModel: AuthViewModel,
) {
  val context = LocalContext.current

  val scrollState = rememberScrollState()

  // Define it here because it's used in the onClick lambda which is not a composable
  val mismatchErrorMessage = stringResource(R.string.create_account_password_mismatch_error)
  val emailWrongFormatErrorMessage = stringResource(R.string.create_account_email_format_error)
  val fieldMustBeFilledErrorMessage =
      stringResource(R.string.create_account_fields_must_be_filled_error)

  var name by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }

  val onSignUpButtonClick = {
    when {
      password != confirmPassword -> {
        Toast.makeText(context, mismatchErrorMessage, Toast.LENGTH_SHORT).show()
      }
      email.isEmpty() || password.isEmpty() || name.isEmpty() -> {
        Toast.makeText(context, fieldMustBeFilledErrorMessage, Toast.LENGTH_SHORT).show()
      }
      !email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$".toRegex()) -> {
        Toast.makeText(context, emailWrongFormatErrorMessage, Toast.LENGTH_SHORT).show()
      }
      else -> {
        authViewModel.createAccountWithEmailAndPassword(
            name,
            email,
            password,
            onSuccess = {
              // Navigate to the map screen
              navigationActions.navigateTo(Route.MAP)
            },
            onErrorAction = {
              // Show an error message in a toast
              Toast.makeText(context, context.getString(it), Toast.LENGTH_SHORT).show()
            },
            context = context)
      }
    }
  }

  Column(
      modifier =
          Modifier.testTag(Screen.CREATE_ACCOUNT)
              .padding(
                  start = 16.dp,
                  end = 16.dp,
              )
              .verticalScroll(scrollState)
              .imeNestedScroll()
              .safeDrawingPadding(),
      verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BackButton(navigationActions)
        Text(
            stringResource(R.string.create_account_title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.testTag(CreateAccountScreen.TEST_TAG_TITLE))

        CustomTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.create_account_name_label),
            maxLength = CustomTextField.MAX_NAME_LENGTH,
            modifier = Modifier.testTag(CreateAccountScreen.TEST_TAG_NAME_INPUT),
        )

        CustomTextField(
            value = email,
            onValueChange = { email = it },
            label = stringResource(R.string.create_account_email_label),
            maxLength = CustomTextField.MAX_EMAIL_LENGTH,
            modifier = Modifier.testTag(CreateAccountScreen.TEST_TAG_EMAIL_INPUT),
        )

        CustomTextField(
            value = password,
            onValueChange = { password = it },
            isPassword = true,
            label = stringResource(R.string.create_account_password_label),
            maxLength = CustomTextField.MAX_PASSWORD_LENGTH,
            modifier = Modifier.testTag(CreateAccountScreen.TEST_TAG_PASSWORD_INPUT),
        )

        CustomTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            isPassword = true,
            label = stringResource(R.string.create_account_repeat_password_label),
            maxLength = CustomTextField.MAX_PASSWORD_LENGTH,
            modifier = Modifier.testTag(CreateAccountScreen.TEST_TAG_CONFIRM_PASSWORD_INPUT),
        )

        BigButton(
            modifier = Modifier.fillMaxWidth().testTag(CreateAccountScreen.TEST_TAG_SIGN_UP_BUTTON),
            buttonType = ButtonType.PRIMARY,
            label = stringResource(R.string.create_account_create_account_button),
            onClick = onSignUpButtonClick,
        )
      }
}
