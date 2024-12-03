package ch.hikemate.app.ui.components

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
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
    val elevationData = listOf(100.0, 200.0, 150.0, 175.0)

    composeTestRule.setContent {
      ElevationGraph(
          elevations = elevationData,
          modifier = Modifier.size(200.dp),
          progressThroughHike = 0.5f,
          styleProperties = ElevationGraphStyleProperties(locationMarkerSize = 24f))
    }

    composeTestRule.waitForIdle()

    // Since we can't directly test Canvas drawing, we verify no error states are shown
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
      ElevationGraph(
          elevations = largeElevationData, modifier = Modifier.size(200.dp), maxNumberOfPoints = 40)
    }

    composeTestRule.waitForIdle()

    // Verify no error states are shown
    composeTestRule
        .onNode(hasText(context.getString(R.string.elevation_graph_loading_label)))
        .assertDoesNotExist()
    composeTestRule
        .onNode(hasText(context.getString(R.string.hike_card_no_data_label)))
        .assertDoesNotExist()
  }
}
