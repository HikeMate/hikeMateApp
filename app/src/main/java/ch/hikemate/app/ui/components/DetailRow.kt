package ch.hikemate.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

object DetailRow {
  const val TEST_TAG_DETAIL_ROW_TAG = "DetailRowTag"
  const val TEST_TAG_DETAIL_ROW_VALUE = "DetailRowValue"
}

/**
 * A composable that displays a row with a label and value. The row spans the full width with the
 * label aligned to the left and value to the right.
 *
 * @param label The text to display as the label on the left side
 * @param value The text to display as the value on the right side
 * @param valueColor The color of the value text. Defaults to the current theme's onSurface color
 */
@Composable
fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
  Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag(DetailRow.TEST_TAG_DETAIL_ROW_TAG))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            modifier = Modifier.testTag(DetailRow.TEST_TAG_DETAIL_ROW_VALUE))
      }
}
