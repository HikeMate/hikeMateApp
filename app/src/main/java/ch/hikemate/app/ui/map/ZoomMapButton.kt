package ch.hikemate.app.ui.map

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.hikemate.app.R

object ZoomMapButton {
  const val ZOOM_MAP_BUTTON = "ZoomMapButton"
  const val ZOOM_IN_BUTTON = "ZoomInButton"
  const val ZOOM_OUT_BUTTON = "ZoomOutButton"
  val ZOOM_BUTTON_WIDTH_SIZE = 52.dp
}

/**
 * Buttons that zoom in and out of the map.
 *
 * @param onZoomIn Called when the user clicks the zoom in button.
 * @param onZoomOut Called when the user clicks the zoom out button.
 * @param modifier Modifier to be applied to the buttons.
 */
@Composable
fun ZoomMapButton(onZoomIn: () -> Unit, onZoomOut: () -> Unit, modifier: Modifier = Modifier) {
  Row(modifier = modifier.testTag(ZoomMapButton.ZOOM_MAP_BUTTON).padding(horizontal = 5.dp)) {
    IconButton(
        onClick = onZoomOut,
        modifier =
            Modifier.testTag(ZoomMapButton.ZOOM_OUT_BUTTON)
                .width(ZoomMapButton.ZOOM_BUTTON_WIDTH_SIZE)
                .padding(horizontal = 1.dp),
        colors =
            IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface)) {
          Icon(
              imageVector = Icons.Filled.Remove,
              contentDescription = stringResource(id = R.string.zoom_out),
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier)
        }
    IconButton(
        onClick = onZoomIn,
        modifier =
            Modifier.testTag(ZoomMapButton.ZOOM_IN_BUTTON)
                .width(ZoomMapButton.ZOOM_BUTTON_WIDTH_SIZE)
                .padding(horizontal = 1.dp),
        colors =
            IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface)) {
          Icon(
              imageVector = Icons.Filled.Add,
              contentDescription = stringResource(id = R.string.zoom_in),
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier)
        }
  }
}
