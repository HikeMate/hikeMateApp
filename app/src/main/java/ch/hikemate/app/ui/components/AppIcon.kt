package ch.hikemate.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import ch.hikemate.app.R

/**
 * A composable that displays the app icon.
 *
 * @param size The size of the icon.
 */
@Composable
fun AppIcon(size: Dp) {
  Image(
      painter = painterResource(id = R.drawable.app_icon),
      contentDescription = "App Logo",
      modifier = Modifier.size(size).testTag("appIcon"))
}
