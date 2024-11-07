package ch.hikemate.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.copy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.core.math.MathUtils
import kotlin.math.ceil

/**
 * A composable function to display an elevation graph. The graph is averaged to ensure that the
 * number of points is not too high, and thus that performance is not too impacted. The graph is
 * drawn using a Canvas, which implies that it is not interactive.
 *
 * @param elevationData The list of elevation data
 * @param maxNumberOfPoints The maximum number of points to display (default to 40)
 * @param strokeColor The color of the graph (default to black)
 * @param strokeWidth The width of the graph (default to 3f)
 * @param modifier The modifier for the graph (default to fillMaxSize)
 */
@Composable
fun ElevationGraph(
    elevationData: List<Double>,
    modifier: Modifier = Modifier,
    maxNumberOfPoints: Int = 40,
    strokeColor: Color = Color.Black,
    fillColor: Color = Color.Black,
    strokeWidth: Float = 6f,
) {
  if (elevationData.isEmpty()) {
    return
  }

  // Averaging the elevation data to reduce the number of points
  val averageSize = remember { ceil(elevationData.size.toDouble() / maxNumberOfPoints).toInt() }
  val elevationDataAveraged = remember {
    elevationData.chunked(averageSize) { chunk -> chunk.average() }
  }

  var height by remember { mutableFloatStateOf(0.0f) }
  var width by remember { mutableFloatStateOf(0.0f) }

  // Bunch of variables to scale the graph in height
  val maxElevation = remember(elevationDataAveraged) { elevationDataAveraged.maxOrNull() ?: 0.0 }
  val minElevation = remember(elevationDataAveraged) { elevationDataAveraged.minOrNull() ?: 0.0 }
  val elevationRange = maxElevation - minElevation
  val elevationStep = elevationRange / height

  val widthStep = width / (elevationDataAveraged.size - 1)

  // Build (x, y) tuples to draw the path
  // Scale the points width to fit the whole width
  // Scale the points height to fit the whole height, but keeping the scale.
  // The height is inverted because the origin of the canvas is at the top left corner
  val scaledData =
      remember(height, width) {
        elevationDataAveraged
            .mapIndexed { index, elevation ->
              Pair(
                  index * widthStep,
                  (height - (elevation - minElevation) / elevationStep).toFloat())
            }
            .map {
              // We clamp the points to ensure they are not outside the canvas
              Pair(it.first, MathUtils.clamp(it.second, strokeWidth, height - strokeWidth))
            }
      }

  val path = Path()
  path.moveTo(scaledData.first().first, scaledData.first().second)

  for ((x, y) in scaledData.drop(1)) {
    path.lineTo(x, y)
  }

  val outlinePath = path.copy()
  outlinePath.lineTo(width, height)
  outlinePath.lineTo(0f, height)

  Canvas(
      modifier.onGloballyPositioned { coordinates ->
        height = coordinates.size.height.toFloat()
        width = coordinates.size.width.toFloat()
      }) {
        drawPath(path, strokeColor, style = Stroke(width = strokeWidth))
        clipPath(outlinePath, clipOp = androidx.compose.ui.graphics.ClipOp.Intersect) {
          drawRect(color = fillColor)
        }
      }
}
