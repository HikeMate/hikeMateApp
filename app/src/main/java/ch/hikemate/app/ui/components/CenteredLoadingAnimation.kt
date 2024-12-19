package ch.hikemate.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

object CenteredLoadingAnimation {
  const val TEST_TAG_CENTERED_LOADING_ANIMATION = "CenteredLoadingAnimation"
}

/**
 * Displays a centered loading animation (moving circle).
 *
 * Provides [CenteredLoadingAnimation.TEST_TAG_CENTERED_LOADING_ANIMATION] as a test tag for the
 * loading animation component.
 *
 * Takes the whole parent space with [Modifier.fillMaxSize].
 *
 * @param text The text to display below (or above) the loading animation.
 * @param textAboveLoadingAnimation If true, the text will be displayed above the loading animation.
 */
@Composable
fun CenteredLoadingAnimation(text: String? = null, textAboveLoadingAnimation: Boolean = false) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
          if (text != null && textAboveLoadingAnimation) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
          }
          CircularProgressIndicator(
              modifier =
                  Modifier.testTag(CenteredLoadingAnimation.TEST_TAG_CENTERED_LOADING_ANIMATION))
          if (text != null && !textAboveLoadingAnimation) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
          }
        }
  }
}
