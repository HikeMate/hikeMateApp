package ch.hikemate.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.ceil

/**
 * A composable function to display an elevation graph. The graph is averaged to ensure that the
 * number of points is not too high, and thus that performance is not too impacted. The graph is
 * drawn using a Canvas, which implies that it is not interactive.
 *
 * @param elevationData The list of elevation data
 * @param maxNumberOfPoints The maximum number of points to display (default to 40)
 * @param color The color of the graph (default to black)
 * @param strokeWidth The width of the graph (default to 3f)
 * @param modifier The modifier for the graph (default to fillMaxSize)
 */
@Composable
fun ElevationGraph(
    elevationData: List<Double>,
    modifier: Modifier = Modifier,
    maxNumberOfPoints: Int = 40,
    color: Color = Color.Black,
    strokeWidth: Float = 3f,
) {
  if (elevationData.isEmpty()) {
    return
  }
  Canvas(modifier) {
    // Averaging the elevation data to reduce the number of points
    val averageSize = ceil(elevationData.size.toDouble() / maxNumberOfPoints).toInt()
    val elevationDataAveraged = elevationData.chunked(averageSize) { chunk -> chunk.average() }

    val height = size.height
    val width = size.width

    // Bunch of variables to scale the graph in height
    val maxElevation = elevationDataAveraged.maxOrNull() ?: 0.0
    val minElevation = elevationDataAveraged.minOrNull() ?: 0.0
    val elevationRange = maxElevation - minElevation
    val elevationStep = elevationRange / height

    val widthStep = width / elevationDataAveraged.size

    // Build (x, y) tuples to draw the path
    // Scale the points width to fit the whole width
    // Scale the points height to fit the whole height, but keeping the scale.
    // The height is inverted because the origin of the canvas is at the top left corner
    val scaledData =
        elevationDataAveraged.mapIndexed { index, elevation ->
          Pair(index * widthStep, (height - (elevation - minElevation) / elevationStep).toFloat())
        }

    val path = Path()
    path.moveTo(scaledData.first().first, scaledData.first().second)

    for ((x, y) in scaledData.drop(1)) {
      path.lineTo(x, y)
    }
    drawPath(path, color, style = Stroke(width = strokeWidth))
  }
}
