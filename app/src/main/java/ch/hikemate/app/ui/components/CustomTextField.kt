package ch.hikemate.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import ch.hikemate.app.R
import ch.hikemate.app.ui.theme.primaryColor
import java.util.Locale

object CustomTextField {
  const val MAX_NAME_LENGTH = 40
  const val MAX_EMAIL_LENGTH = 50
  const val MAX_PASSWORD_LENGTH = 50
}

/**
 * Custom text field with custom colors and optional max length. This field will always be single
 * line.
 *
 * @param value The value of the text field.
 * @param onValueChange The callback when the value changes.
 * @param label The label of the text field.
 * @param modifier The modifier of the text field.
 * @param isPassword Whether the text field is a password field.
 * @param maxLength The maximum length of the text field.
 */
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    maxLength: Int? = null,
) {

  OutlinedTextField(
      modifier = modifier.fillMaxWidth(),
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
      value = value,
      visualTransformation =
          if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
      onValueChange = {
        if (maxLength == null || it.length <= maxLength) {
          onValueChange(it)
        }
      },
      supportingText =
          if (maxLength != null) {
            {
              Text(
                  text =
                      String.format(
                          Locale.getDefault(),
                          stringResource(R.string.field_supportive_text_format),
                          value.length,
                          maxLength),
                  modifier = Modifier.fillMaxWidth(),
                  textAlign = TextAlign.End,
              )
            }
          } else null,
      label = { Text(label) },
      singleLine = true,
  )
}
