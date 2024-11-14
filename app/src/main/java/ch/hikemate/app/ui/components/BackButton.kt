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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
fun BackButton(navigationActions: NavigationActions, modifier: Modifier = Modifier) {
  IconButton(
      onClick = { navigationActions.goBack() },
      modifier =
          modifier
              .testTag(BackButton.BACK_BUTTON_TEST_TAG)
              .size(50.dp)
              .padding(8.dp)
              .background(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20))
              .border(width = 1.dp, color = Color.Black, shape = RoundedCornerShape(20))) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            modifier = Modifier.fillMaxSize(),
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onSurface)
      }
}
