package ch.hikemate.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import ch.hikemate.app.R

object AppIcon {
  const val TEST_TAG = "appIcon"
}

/**
 * A composable that displays the app icon.
 *
 * @param size The size of the icon.
 */
@Composable
fun AppIcon(size: Dp) {
  Image(
      painter = painterResource(id = R.drawable.app_icon),
      contentDescription = stringResource(R.string.app_icon_content_description),
      modifier = Modifier.size(size).testTag(AppIcon.TEST_TAG),
  )
}
