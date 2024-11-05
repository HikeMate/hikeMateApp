package ch.hikemate.app.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.LatLong
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun HikeDetailScreen() { // route: HikeRoute, onBack: () -> Unit) {

  val scaffoldState = rememberBottomSheetScaffoldState()

  val context = LocalContext.current

  val mapView = remember {
    MapView(context).apply {
      setMultiTouchControls(true)
      zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
    }
  }

  BottomSheetScaffold(
      scaffoldState = scaffoldState,
      sheetContainerColor = MaterialTheme.colorScheme.surface,
      sheetContent = {
        Column(modifier = Modifier.fillMaxSize().testTag(MapScreen.TEST_TAG_HIKES_LIST)) {}
      },
      sheetPeekHeight = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT) {}
}

// PreviewParameterProvider for HikeRoute
class HikeRoutePreviewParameterProvider : PreviewParameterProvider<HikeRoute> {
  override val values: Sequence<HikeRoute>
    get() =
        sequenceOf(
            HikeRoute(
                id = "Hike 1",
                description = "Hike 1 description",
                bounds = Bounds(0.0, 0.0, 1.0, 1.0),
                ways = listOf(LatLong(0.0, 0.0), LatLong(1.0, 1.0))))
}

// Preview function for HikeDetails
@Preview
@Composable
fun HikeDetailsPreview(
    @PreviewParameter(HikeRoutePreviewParameterProvider::class) route: HikeRoute
) {
  HikeDetails(route = route) {
    // Handle back action if needed
  }
}

@Composable
fun HikeDetails(route: HikeRoute, onBack: () -> Unit) {

  Box(modifier = Modifier.fillMaxSize()) {
    Text(
        text = "Hike Detail Screen",
        textAlign = TextAlign.Center,
        modifier = Modifier.align(Alignment.Center))
  }
}
