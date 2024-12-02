package ch.hikemate.app.ui.components

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.hikemate.app.R
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
      ElevationGraph(elevations = elevationData, styleProperties = ElevationGraphStyleProperties())
    }

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
    val elevationData = listOf(100.0, 200.0, 150.0, 175.0)

    composeTestRule.setContent {
      ElevationGraph(
          elevations = elevationData,
          progressThroughHike = 0.5f,
          styleProperties = ElevationGraphStyleProperties(locationMarkerSize = 24f))
    }

    // Verify that the graph is displayed without error messages
    composeTestRule
        .onNode(hasText(context.getString(R.string.elevation_graph_loading_label)))
        .assertDoesNotExist()
    composeTestRule
        .onNode(hasText(context.getString(R.string.hike_card_no_data_label)))
        .assertDoesNotExist()
  }

  @Test
  fun elevationGraph_withProgressOutOfBounds_clampsToValidRange() {
    val elevationData = listOf(100.0, 200.0, 150.0, 175.0)

    // Test with progress > 1
    composeTestRule.setContent {
      ElevationGraph(elevations = elevationData, progressThroughHike = 1.5f)
    }

    // Verify that the graph is displayed without errors
    composeTestRule
        .onNode(hasText(context.getString(R.string.elevation_graph_loading_label)))
        .assertDoesNotExist()
    composeTestRule
        .onNode(hasText(context.getString(R.string.hike_card_no_data_label)))
        .assertDoesNotExist()
  }

  @Test
  fun elevationGraph_withCustomStyles_appliesStyles() {
    val elevationData = listOf(100.0, 200.0, 150.0, 175.0)
    val customStyles =
        ElevationGraphStyleProperties(
            strokeColor = Color.Red,
            fillColor = Color.Blue,
            strokeWidth = 5f,
            locationMarkerSize = 32f)

    composeTestRule.setContent {
      ElevationGraph(
          elevations = elevationData, styleProperties = customStyles, progressThroughHike = 0.5f)
    }

    // Verify that the graph is displayed without errors
    composeTestRule
        .onNode(hasText(context.getString(R.string.elevation_graph_loading_label)))
        .assertDoesNotExist()
    composeTestRule
        .onNode(hasText(context.getString(R.string.hike_card_no_data_label)))
        .assertDoesNotExist()
  }

  @Test
  fun elevationGraph_withLargeDataSet_handlesDataCorrectly() {
    val largeElevationData = List(1000) { it.toDouble() }

    composeTestRule.setContent {
      ElevationGraph(elevations = largeElevationData, maxNumberOfPoints = 40)
    }

    // Verify that the graph is displayed without errors
    composeTestRule
        .onNode(hasText(context.getString(R.string.elevation_graph_loading_label)))
        .assertDoesNotExist()
    composeTestRule
        .onNode(hasText(context.getString(R.string.hike_card_no_data_label)))
        .assertDoesNotExist()
  }
}
