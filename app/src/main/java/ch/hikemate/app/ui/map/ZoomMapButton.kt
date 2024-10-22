package ch.hikemate.app.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

const val ZOOM_MAP_BUTTON = "ZoomMapButton"
const val ZOOM_IN_BUTTON = "ZoomInButton"
const val ZOOM_OUT_BUTTON = "ZoomOutButton"

@Composable
fun ZoomMapButton(onZoomIn: () -> Unit, onZoomOut: () -> Unit, modifier: Modifier) {
  Column(modifier = modifier.testTag(ZOOM_MAP_BUTTON)) {
    Button(onClick = onZoomIn, modifier = Modifier.testTag(ZOOM_IN_BUTTON)) {
      Icon(imageVector = Icons.Filled.Add, contentDescription = "Zoom In")
    }
    Button(onClick = onZoomOut, modifier = Modifier.testTag(ZOOM_OUT_BUTTON)) {
      Icon(imageVector = Icons.Filled.Remove, contentDescription = "Zoom Out")
    }
  }
}
