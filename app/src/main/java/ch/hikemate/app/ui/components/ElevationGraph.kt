package ch.hikemate.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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

    for (i in elevationDataAveraged.indices.take(elevationDataAveraged.size - 1)) {
      // Scale the points to fit the whole width
      val x = i * widthStep
      // Scale the points to fit the whole height, but keeping the scale.
      // The height is inverted because the origin of the canvas is at the top left corner
      val y = (height - (elevationDataAveraged[i] - minElevation) / elevationStep).toFloat()
      val nextX = (i + 1) * widthStep
      val nextY = (height - (elevationDataAveraged[i + 1] - minElevation) / elevationStep).toFloat()
      drawLine(color, Offset(x, y), Offset(nextX, nextY), strokeWidth = strokeWidth)
    }
  }
}
