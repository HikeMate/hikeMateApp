package ch.hikemate.app.ui.map

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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

    // Allow for faster loading of tiles. Default OSMDroid value is 2.
    tileDownloadThreads = 4

    // Maximum number of tiles that can be downloaded at once. Default is 40.
    tileDownloadMaxQueueSize = 40

    // Maximum number of bytes that can be used by the tile file system cache. Default is 600MB.
    tileFileSystemCacheMaxBytes = 600L * 1024L * 1024L
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

  // Initialize the list of hiking routes once when the map is loaded
  // Leave subsequent updates to the map listener
  LaunchedEffect(Unit) {
    hikingRoutesViewModel.setArea(mapView.boundingBox)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsibleHikesList(hikingRoutesViewModel: ListOfHikeRoutesViewModel) {
  val scaffoldState = rememberBottomSheetScaffoldState()
  val routes = hikingRoutesViewModel.hikeRoutes.collectAsState()
  val context = LocalContext.current

  BottomSheetScaffold(
    scaffoldState = scaffoldState,
    sheetContainerColor = MaterialTheme.colorScheme.surface,
    sheetContent = {

      Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
      ) {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
        ) {
          if (routes.value.isEmpty()) {
            item {
              // Use a box to center the Text composable of the empty list message
              Box(
                modifier = Modifier
                  .fillMaxWidth(),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = context.getString(R.string.map_screen_empty_hikes_list_message),
                  style = MaterialTheme.typography.bodyLarge,
                  // Align the text within the Text composable to the center
                  textAlign = TextAlign.Center
                )
              }
            }
          }
          else {
            items(routes.value.size) { index: Int ->
              HikingRouteItem(
                title = routes.value[index],
                altitudeDifference = 1000,
                isSuitable = index % 2 == 0
              )

              Spacer(modifier = Modifier.height(12.dp))
            }
          }
        }
      }
    },
    sheetPeekHeight = 400.dp
  ) { }
}

@Composable
fun HikingRouteItem(
  title: String,
  altitudeDifference: Int,
  isSuitable: Boolean,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(
      modifier = Modifier.weight(1f)
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(8.dp))

      Row (
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min)
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column {
          Text(
            text = "Altitude difference",
            style = MaterialTheme.typography.bodySmall
          )
          Text(
            text = "${altitudeDifference}m",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = if (isSuitable) Icons.Default.Check else Icons.Default.Warning,
          // The icon is only decorative, the following message is enough for accessibility
          contentDescription = null,
          // TODO : Replace suitable and challenging icon colors with theme colors
          tint = if (isSuitable) Color(0xFF4CAF50) else Color(0xFFFFC107),
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text =
            if (isSuitable)
              LocalContext.current.getString(R.string.map_screen_suitable_hike_label)
            else
              LocalContext.current.getString(R.string.map_screen_challenging_hike_label),
          style = MaterialTheme.typography.bodySmall,
          // TODO : Replace suitable and challenging icon colors with theme colors
          color = if (isSuitable) Color(0xFF4CAF50) else Color(0xFFFFC107)
        )
      }
    }

    Spacer(modifier = Modifier.width(4.dp))

    Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = LocalContext.current.getString(
        R.string.map_screen_hike_details_content_description
      ),
      tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
      modifier = Modifier.size(24.dp)
    )
  }
}

@Composable
@Preview
fun BottomListPreview() {
  val viewModel = ListOfHikeRoutesViewModel.Factory.create(ListOfHikeRoutesViewModel::class.java)
  viewModel.getRoutes()
  CollapsibleHikesList(viewModel)
}