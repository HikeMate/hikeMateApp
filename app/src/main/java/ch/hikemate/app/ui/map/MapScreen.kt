package ch.hikemate.app.ui.map

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ch.hikemate.app.R
import ch.hikemate.app.model.map.ListOfHikeRoutesViewModel
import ch.hikemate.app.ui.theme.MapMenuButtonBackground
import ch.hikemate.app.ui.theme.MapMenuButtonForeground
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@Composable
fun MapScreen(hikingRoutesViewModel: ListOfHikeRoutesViewModel) {
  val context = LocalContext.current
  // Avoid re-creating the MapView on every recomposition
  val mapView = remember { MapView(context) }

  Configuration.getInstance().apply {
    // Set user-agent to avoid rejected requests
    userAgentValue = context.packageName
    // TODO : Are there more configuration options that need to be set?
    // TODO : How does OSMDroid manage the cache?
  }

  mapView.apply {
    // TODO : Define a level of zoom to start at. User settings is too much for this sprint but could be a good idea in the long run.
    controller.setZoom(15.0)
    // TODO : Define where the map should be centered. User location might be a bit too much for this sprint though.
    controller.setCenter(GeoPoint(46.5, 6.6))
    // Enable touch-controls such as pinch to zoom
    setMultiTouchControls(true)
    // Update hiking routes every time the user moves the map
    // TODO : Those updates could be quite frequent, have a cooldown to avoid sending a request each time?
    addMapListener(object : MapListener {
      override fun onScroll(event: ScrollEvent?): Boolean {
        Log.d("MapScreen", "onScroll")
        hikingRoutesViewModel.setArea(mapView.boundingBox)
        return true
      }

      override fun onZoom(event: ZoomEvent?): Boolean {
        Log.d("MapScreen", "onZoom")
        hikingRoutesViewModel.setArea(mapView.boundingBox)
        return true
      }
    })
  }

  Box(modifier = Modifier.fillMaxSize()) {
    AndroidView(
      factory = { mapView },
      modifier = Modifier.fillMaxSize()
    )

    IconButton(
      onClick = {
        // TODO : Adapt the map screen to navigation
        Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show()
      },
      modifier = Modifier
        .padding(16.dp)
        .align(Alignment.TopStart)
        // Clip needs to be before background
        .clip(RoundedCornerShape(8.dp))
        .background(MapMenuButtonBackground)
    ) {
      Icon(
        Icons.Default.Menu,
        contentDescription = context.getString(R.string.map_screen_menu_button_content_description),
        tint = MapMenuButtonForeground
      )
    }

    CollapsibleHikesList(hikingRoutesViewModel)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsibleHikesList(hikingRoutesViewModel: ListOfHikeRoutesViewModel) {
  val scaffoldState = rememberBottomSheetScaffoldState()
  val routes = hikingRoutesViewModel.hikeRoutes.collectAsState()

  BottomSheetScaffold(
    scaffoldState = scaffoldState,
    sheetContent = {

      Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
      ) {
        Text("Hikes")
        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
        ) {
          items(routes.value.size) { index: Int ->
            Text(routes.value[index])
          }
        }
      }
    },
    sheetPeekHeight = 400.dp
  ) { }

  // TODO : Is this the right place to call getRoutes()?
  hikingRoutesViewModel.getRoutes()
}