package ch.hikemate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.hikemate.app.R
import ch.hikemate.app.ui.map.MapScreen

@Composable
fun HikeCard(
    title: String,
    altitudeDifference: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    messageIcon: Painter? = null,
    messageContent: String? = null,
    messageColor: Color? = null
) {
  val displayMessage = messageContent != null && messageIcon != null && messageColor != null

  Row(
      modifier =
          modifier
              .fillMaxWidth()
              .clickable(onClick = onClick)
              .padding(16.dp, 8.dp)
              .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
              .testTag(MapScreen.TEST_TAG_HIKE_ITEM),
      verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
              text = title,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(8.dp))

          Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(
                    modifier =
                        Modifier.weight(1f)
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)))

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                  Text(text = "Altitude difference", style = MaterialTheme.typography.bodySmall)
                  Text(
                      text = "${altitudeDifference}m",
                      style = MaterialTheme.typography.bodyLarge,
                      fontWeight = FontWeight.Bold)
                }
              }

          if (displayMessage) {
            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  painter = messageIcon!!,
                  // The icon is only decorative, the following message is enough for accessibility
                  contentDescription = null,
                  tint = messageColor!!,
                  modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                  text = messageContent!!,
                  style = MaterialTheme.typography.bodySmall,
                  color = messageColor)
            }
          }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Arrow icon to indicate that the item is clickable
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription =
                LocalContext.current.getString(
                    R.string.map_screen_hike_details_content_description),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp))
      }
}
