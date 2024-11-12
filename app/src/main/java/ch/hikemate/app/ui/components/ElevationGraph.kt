package ch.hikemate.app.ui.components

import android.graphics.Paint
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.core.math.MathUtils
import ch.hikemate.app.R
import kotlin.math.ceil

/**
 * Properties to customize the appearance of the [ElevationGraph].
 *
 * @param fillColor The color of the fill of the graph (default to black)
 * @param strokeColor The color of the line of the graph (default to black)
 * @param strokeWidth The width of the line the graph (default to 3f)
 */
data class ElevationGraphStyleProperties(
    val strokeColor: Color = Color.Black,
    val fillColor: Color = Color.Black,
    val strokeWidth: Float = 3f,
)

/**
 * A composable function to display an elevation graph. The graph is averaged to ensure that the
 * number of points is not too high, and thus that performance is not too impacted. The graph is
 * drawn using a Canvas, which implies that it is not interactive.
 *
 * @param elevations The list of elevation data
 * @param maxNumberOfPoints The maximum number of points to display (default to 40)
 * @param modifier The modifier for the graph (default to fillMaxSize)
 * @param styleProperties The style properties for the graph
 * @param graphInnerPaddingPercentage The padding inside the graph, defined as a percentage of the
 *   canvas height, used so that the minimal point doesn't overlap the bottom line of the graph
 *   (default to 20.0f)
 */
@Composable
fun ElevationGraph(
    elevations: List<Double>?,
    modifier: Modifier = Modifier,
    maxNumberOfPoints: Int = 40,
    styleProperties: ElevationGraphStyleProperties = ElevationGraphStyleProperties(),
    graphInnerPaddingPercentage: Float = 10.0f
) {
  val context = LocalContext.current

  val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

  val elevationData = if (elevations.isNullOrEmpty()) listOf(0.0) else elevations
  Log.d("ElevationGraph", "Elevation data for the graph: $elevationData")

  // Averaging the elevation data to reduce the number of points
  val averageSize = ceil(elevationData.size.toDouble() / maxNumberOfPoints).toInt()
  val elevationDataAveraged = elevationData.chunked(averageSize) { chunk -> chunk.average() }

  var height by remember { mutableFloatStateOf(0.0f) }
  var width by remember { mutableFloatStateOf(0.0f) }

  // Bunch of variables to scale the graph in height
  val maxElevation = elevationDataAveraged.maxOrNull() ?: 0.0
  val minElevation = elevationDataAveraged.minOrNull() ?: 0.0
  val elevationRange = maxElevation - minElevation
  val elevationStep = if (elevationRange == 0.0) 1.0 else elevationRange / height

  val widthStep = width / (elevationDataAveraged.size - 1)

  val graphInnerPadding = height * graphInnerPaddingPercentage / 100

  // Build (x, y) tuples to draw the path
  // Scale the points width to fit the whole width
  // Scale the points height to fit the whole height, but keeping the scale.
  // The height is inverted because the origin of the canvas is at the top left corner
  val scaledData =
      elevationDataAveraged
          .mapIndexed { index, elevation ->
            Pair(index * widthStep, (height - (elevation - minElevation) / elevationStep).toFloat())
          }
          .map {
            // We clamp the points to ensure they are not outside the canvas
            Pair(
                it.first,
                MathUtils.clamp(
                    it.second,
                    styleProperties.strokeWidth,
                    height - styleProperties.strokeWidth - graphInnerPadding))
          }

  val path = Path()
  path.moveTo(scaledData.first().first, scaledData.first().second)

  for ((x, y) in scaledData.drop(1)) {
    path.lineTo(x, y)
  }

  path.lineTo(width, height)
  path.lineTo(0f, height)
  path.close()

  Canvas(
      modifier.onGloballyPositioned { coordinates ->
        height = coordinates.size.height.toFloat()
        width = coordinates.size.width.toFloat()
      }) {
        if (elevations == null) {
          // If there is no data, we draw a text in the middle of the canvas
          drawIntoCanvas {
            it.nativeCanvas.drawText(
                context.getString(R.string.elevation_graph_loading_label),
                10f,
                height / 2,
                Paint().apply {
                  color = textColor
                  textSize = 40f
                })
          }
        } else if (elevations.isEmpty()) {
          // If there is no data, we draw a text in the middle of the canvas
          drawIntoCanvas {
            it.nativeCanvas.drawText(
                context.getString(R.string.hike_card_no_data_label),
                10f,
                height / 2,
                Paint().apply {
                  color = textColor
                  textSize = 40f
                })
          }
        } else {
          drawPath(
              path,
              styleProperties.strokeColor,
              style = Stroke(width = styleProperties.strokeWidth))
          clipPath(path, clipOp = ClipOp.Intersect) { drawRect(color = styleProperties.fillColor) }
        }
      }
}
