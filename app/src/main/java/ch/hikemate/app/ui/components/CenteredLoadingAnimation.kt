package ch.hikemate.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

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
 */
@Composable
fun CenteredLoadingAnimation() {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(
        modifier = Modifier.testTag(CenteredLoadingAnimation.TEST_TAG_CENTERED_LOADING_ANIMATION))
  }
}
