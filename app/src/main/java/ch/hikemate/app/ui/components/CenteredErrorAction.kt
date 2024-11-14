package ch.hikemate.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

object CenteredErrorAction {
  const val TEST_TAG_CENTERED_ERROR_MESSAGE = "CenteredErrorMessage"
  const val TEST_TAG_CENTERED_ERROR_BUTTON = "CenteredErrorButton"
}

/**
 * Displays an error message that takes the whole parent size and an action button.
 *
 * For an action button to be displayed, the [actionIcon] parameter and the
 * [actionContentDescription] parameter must both not be null.
 *
 * Provides two test tags, [CenteredErrorAction.TEST_TAG_CENTERED_ERROR_MESSAGE] for the text
 * component, and [CenteredErrorAction.TEST_TAG_CENTERED_ERROR_BUTTON] for the action button.
 *
 * @param errorMessage The string resource ID of the error message to display.
 * @param actionIcon The icon to display on the action button. If null, no action button will be
 *   displayed.
 * @param actionContentDescription The string resource ID of the content description for the action
 *   button. If null, no action button will be displayed.
 * @param onAction The action to perform when the action button is clicked. If null, an empty
 *   callback will be used instead.
 */
@Composable
fun CenteredErrorAction(
    errorMessage: Int,
    actionIcon: ImageVector? = null,
    actionContentDescription: Int? = null,
    onAction: (() -> Unit)? = null
) {

  // Box to take the entire space of the parent and center the content
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

    // Column so that the error message and action button are displayed vertically and not on top of
    // each other
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
          text = stringResource(errorMessage),
          style = MaterialTheme.typography.bodyLarge,
          modifier =
              Modifier.padding(16.dp).testTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_MESSAGE))

      // Only display the action button if both an icon and a content description are provided
      if (actionIcon != null && actionContentDescription != null) {
        IconButton(
            onClick = onAction ?: {},
            modifier = Modifier.testTag(CenteredErrorAction.TEST_TAG_CENTERED_ERROR_BUTTON)) {
              Icon(
                  imageVector = actionIcon,
                  contentDescription = stringResource(actionContentDescription))
            }
      }
    }
  }
}
