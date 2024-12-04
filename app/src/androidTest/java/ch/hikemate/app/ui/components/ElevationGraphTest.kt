package ch.hikemate.app.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.hikemate.app.R
import ch.hikemate.app.utils.MapUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ElevationGraphTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun elevationGraph_withValidData_displaysGraph() {
    val elevationData = listOf(100.0, 200.0, 150.0, 175.0)

    composeTestRule.setContent {
      ElevationGraph(
          elevations = elevationData,
          modifier = Modifier.size(200.dp),
          styleProperties = ElevationGraphStyleProperties())
    }

    composeTestRule.waitForIdle()

    // Verify that neither loading nor no data messages are shown
    composeTestRule
        .onNode(hasText(context.getString(R.string.elevation_graph_loading_label)))
        .assertDoesNotExist()
    composeTestRule
        .onNode(hasText(context.getString(R.string.hike_card_no_data_label)))
        .assertDoesNotExist()
  }

  @Test
  fun elevationGraph_withLocationMarker_displaysMarker() {
    val elevationData = listOf(100.0, 20.0, 40.0, 150.0)

    // Get the marker color from the user_location drawable
    val markerDrawable = MapUtils.getUserLocationMarkerIcon(context)
    val markerBitmap = (markerDrawable as android.graphics.drawable.BitmapDrawable).bitmap
    val markerColor = markerBitmap.getPixel(markerBitmap.width / 2, markerBitmap.height / 2)

    composeTestRule.setContent {
      Box(modifier = Modifier.size(200.dp)) {
        ElevationGraph(
            elevations = elevationData,
            modifier = Modifier.fillMaxSize(),
            progressThroughHike = 0.5f,
            styleProperties = ElevationGraphStyleProperties(locationMarkerSize = 50f))
      }
    }

    composeTestRule.waitForIdle()

    val image = composeTestRule.onRoot().captureToImage()
    val pixels = IntArray(image.width * image.height)
    image.readPixels(pixels)

    val markerFound = pixels.contains(markerColor)

    assertTrue("Marker was not found in the image", markerFound)
  }

  @Test
  fun elevationGraph_withoutProgress_doesNotDisplayMarker() {
    val elevationData = listOf(100.0, 200.0, 150.0, 175.0)
    // Get the marker color from the user_location drawable
    val markerDrawable = MapUtils.getUserLocationMarkerIcon(context)
    val markerBitmap = (markerDrawable as android.graphics.drawable.BitmapDrawable).bitmap
    val markerColor = markerBitmap.getPixel(markerBitmap.width / 2, markerBitmap.height / 2)

    composeTestRule.setContent {
      Box(modifier = Modifier.size(200.dp)) {
        ElevationGraph(
            elevations = elevationData,
            modifier = Modifier.fillMaxSize(),
            progressThroughHike = null,
            styleProperties = ElevationGraphStyleProperties(locationMarkerSize = 24f))
      }
    }

    composeTestRule.waitForIdle()

    val image = composeTestRule.onRoot().captureToImage()
    val pixels = IntArray(image.width * image.height)
    image.readPixels(pixels)

    // Check that no marker color exists in the image
    val foundMarker = pixels.any { it == markerColor }
    assertFalse("Marker was found when it should not be displayed", foundMarker)
  }
}
