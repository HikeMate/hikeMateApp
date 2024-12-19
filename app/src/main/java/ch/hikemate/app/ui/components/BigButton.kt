package ch.hikemate.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ch.hikemate.app.ui.theme.onPrimaryColor
import ch.hikemate.app.ui.theme.onSecondaryColor
import ch.hikemate.app.ui.theme.primaryColor
import ch.hikemate.app.ui.theme.secondaryColor

/**
 * An enum class representing the different types of buttons that can be displayed.
 *
 * @property backgroundColor The background color of the button.
 * @property textColor The text color of the button.
 * @property border The border of the button.
 */
enum class ButtonType(
    val backgroundColor: Color,
    val textColor: Color,
    val border: BorderStroke? = null
) {
  PRIMARY(primaryColor, onPrimaryColor),
  SECONDARY(secondaryColor, onSecondaryColor, BorderStroke(1.dp, onSecondaryColor)),
}

/**
 * A composable that displays a big button.
 *
 * @param buttonType The type of the button.
 * @param label The label of the button.
 * @param onClick The action to perform when the button is clicked.
 */
@Composable
fun BigButton(
    buttonType: ButtonType,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fillColor: Color = buttonType.backgroundColor,
) {
  Button(
      onClick = onClick,
      colors =
          ButtonDefaults.buttonColors(
              contentColor = buttonType.textColor, containerColor = fillColor),
      shape = RoundedCornerShape(20),
      modifier =
          modifier
              .fillMaxWidth()
              .height(44.dp)
              .border(
                  buttonType.border ?: BorderStroke(0.dp, Color.Transparent),
                  shape = RoundedCornerShape(20))) {
        Text(
            text = label,
            color = buttonType.textColor,
            style = MaterialTheme.typography.titleLarge,
        )
      }
}
