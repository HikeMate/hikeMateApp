package ch.hikemate.app.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.hikemate.app.R
import ch.hikemate.app.ui.map.MapScreen
import ch.hikemate.app.utils.RouteUtils

object HikeCard {
  const val TEST_TAG_HIKE_CARD_TITLE = "HikeCardTitle"
  const val TEST_TAG_IS_SUITABLE_TEXT = "HikeCardIsSuitableText"
}

// This fix the Sonar rule kotlin:S107, about too many parameters in a function
/**
 * Properties to customize the appearance of the [HikeCard].
 *
 * @param messageIcon The icon to display next to the message.
 * @param messageColor The color of the message.
 * @param graphColor The color of the elevation graph.
 * @see HikeCard
 */
data class HikeCardStyleProperties(
    val messageIcon: Painter? = null,
    val messageColor: Color? = null,
    val graphColor: Color? = null,
)

/**
 * A card that displays information about a hike.
 *
 * Can display a message as well. The message is only displayed if all three of [messageContent],
 * and the icon and color of [styleProperties] are not null. If one of them is null, the message
 * won't be displayed.
 *
 * @param title The title of the hike.
 * @param elevationData The elevation data to display in the graph.
 * @param onClick The callback to be called when the card is clicked.
 * @param modifier The modifier to be applied to the card.
 * @param messageContent The message to display below the elevation graph.
 * @param styleProperties The properties to customize the appearance of the card.
 */
@Composable
fun HikeCard(
    title: String,
    modifier: Modifier = Modifier,
    elevationData: List<Double>?,
    onClick: () -> Unit,
    messageContent: String? = null,
    styleProperties: HikeCardStyleProperties = HikeCardStyleProperties(),
    showGraph: Boolean = true,
) {
  // The message requires a content, a color and an icon to be displayed, otherwise don't display it
  val displayMessage =
      messageContent != null &&
          styleProperties.messageColor != null &&
          styleProperties.messageIcon != null
  Log.i(
      "HikeCard",
      "displayMessage: $displayMessage, title: $title, elevationData: $elevationData, messageContent: $messageContent")
  val elevationGain = RouteUtils.calculateElevationGain(elevationData ?: emptyList())

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
              fontWeight = FontWeight.Bold,
              modifier = Modifier.testTag(HikeCard.TEST_TAG_HIKE_CARD_TITLE))
          Spacer(modifier = Modifier.height(8.dp))

          if (showGraph)
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    ElevationGraph(
                        elevations = elevationData,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        styleProperties =
                            ElevationGraphStyleProperties(
                                strokeColor =
                                    styleProperties.graphColor ?: MaterialTheme.colorScheme.primary,
                                fillColor =
                                    (styleProperties.graphColor
                                            ?: MaterialTheme.colorScheme.primary)
                                        .copy(0.1f)))

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                      Text(
                          text = stringResource(R.string.hike_card_elevation_gain_label),
                          style = MaterialTheme.typography.bodySmall)
                      Text(
                          text =
                              if (elevationData == null)
                                  stringResource(R.string.hike_card_no_data_label)
                              else
                                  stringResource(
                                      R.string.hike_card_elevation_gain_value_template,
                                      elevationGain),
                          style = MaterialTheme.typography.bodyLarge,
                          fontWeight = FontWeight.Bold)
                    }
                  }

          if (displayMessage) {
            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  painter = styleProperties.messageIcon!!,
                  // The icon is only decorative, the following message is enough for accessibility
                  contentDescription = null,
                  tint = styleProperties.messageColor!!,
                  modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                  modifier = Modifier.testTag(HikeCard.TEST_TAG_IS_SUITABLE_TEXT),
                  text = messageContent!!,
                  style = MaterialTheme.typography.bodySmall,
                  color = styleProperties.messageColor)
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
