package ch.hikemate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.hikemate.app.R
import ch.hikemate.app.ui.navigation.NavigationActions

object BackButton {
  const val BACK_BUTTON_TEST_TAG = "backButton"
}

/**
 * A composable that displays a back button.
 *
 * @param navigationActions The navigation actions.
 */
@Composable
fun BackButton(
    navigationActions: NavigationActions,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { navigationActions.goBack() }
) {

  var wantsToClick by remember { mutableStateOf(false) }

  LaunchedEffect(wantsToClick) {
    if (wantsToClick) {
      onClick()
      wantsToClick = false
    }
  }

  IconButton(
      onClick = { wantsToClick = true },
      modifier =
          modifier
              .testTag(BackButton.BACK_BUTTON_TEST_TAG)
              .size(50.dp)
              .padding(8.dp)
              .background(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20))
              .border(
                  width = 1.dp,
                  color = MaterialTheme.colorScheme.onSurface,
                  shape = RoundedCornerShape(20))) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            modifier = Modifier.fillMaxSize(),
            contentDescription = stringResource(R.string.back_button_content_description),
            tint = MaterialTheme.colorScheme.onSurface)
      }
}
