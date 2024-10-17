package ch.hikemate.app.ui.map

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.hikemate.app.R
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.LatLong
import ch.hikemate.app.model.route.ListOfHikeRoutesViewModel
import ch.hikemate.app.ui.navigation.LIST_TOP_LEVEL_DESTINATIONS
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.navigation.SideBarNavigation
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

object MapScreen {
  const val TEST_TAG_MAP = "map"
  const val TEST_TAG_HIKES_LIST = "hikesList"
  const val TEST_TAG_HIKE_ITEM = "hikeItem"

  const val MIN_HUE = 0
  const val MAX_HUE = 360
  const val MIN_SATURATION = 42
  const val MAX_SATURATION = 98
  const val MIN_LIGHTNESS = 40
  const val MAX_LIGHTNESS = 90

  const val STROKE_WIDTH = 10f
}

/**
 * Generates a random color in the HSL color space. The function uses HSL to generate a color
 * instead of ARGB because it makes it easier to have a range of colors that we know will render
 * well on the map.
 *
 * @return The generated color as an [Int].
 */
fun getRandomColor(): Int {
  fun randomInt(min: Int, max: Int): Int {
    return (min..max).random()
  }

  val h = randomInt(MapScreen.MIN_HUE, MapScreen.MAX_HUE).toFloat() // All colors
  val s =
      randomInt(MapScreen.MIN_SATURATION, MapScreen.MAX_SATURATION) /
          100.0f // Saturation between 42% and 98%
  val l =
      randomInt(MapScreen.MIN_LIGHTNESS, MapScreen.MAX_LIGHTNESS) /
          100.0f // Lightness between 40% and 90%

  return Color.hsl(h, s, l).toArgb()
}

/**
 * Shows a hike on the map.
 *
 * @param mapView The map view where the hike will be shown.
 * @param hike The hike to be shown.
 * @param color The color of the hike.
 */
fun showHikeOnMap(mapView: MapView, hike: HikeRoute, color: Int) {
  val line = Polyline()

  line.setPoints(hike.ways.map { GeoPoint(it.lat, it.lon) })
  line.outlinePaint.color = color
  line.outlinePaint.strokeWidth = MapScreen.STROKE_WIDTH

  line.setOnClickListener { _, _, _ ->
    Toast.makeText(
            mapView.context,
            "Hike details not implemented yet. Hike ID: ${hike.id}",
            Toast.LENGTH_SHORT)
        .show()
    true
  }

  mapView.overlays.add(line)
}

@Composable
fun MapScreen(
    hikingRoutesViewModel: ListOfHikeRoutesViewModel =
        viewModel(factory = ListOfHikeRoutesViewModel.Factory),
    navigationActions: NavigationActions
) {
  val context = LocalContext.current
  // Avoid re-creating the MapView on every recomposition
  val mapView = remember { MapView(context) }

  // Associate each hike with a random color to show it on the map and in the list
  val hikesWithColors: MutableList<Pair<HikeRoute, Int>> = mutableListOf()

  // TODO: Replace the following code with the actual repository implementation
  val hikes =
      listOf(
          HikeRoute(
              id = "1",
              bounds = Bounds(46.5, 6.6, 46.6, 6.7),
              ways = listOf(LatLong(46.5, 6.6), LatLong(46.55, 6.65), LatLong(46.6, 6.7))),
          HikeRoute(
              id = "2",
              bounds = Bounds(46.6, 6.7, 46.7, 6.8),
              ways = listOf(LatLong(46.6, 6.7), LatLong(46.65, 6.75), LatLong(46.7, 6.8))),
          HikeRoute(
              id = "3",
              bounds = Bounds(46.7, 6.8, 46.8, 6.9),
              ways = listOf(LatLong(46.7, 6.8), LatLong(46.75, 6.85), LatLong(46.8, 6.9))))

  hikes.forEach {
    val color = getRandomColor()
    showHikeOnMap(mapView, it, color)
    hikesWithColors.add(Pair(it, color))
  }

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
    controller.setZoom(12.0)
    controller.setCenter(GeoPoint(46.5, 6.6))
    // Enable touch-controls such as pinch to zoom
    setMultiTouchControls(true)
    // Update hiking routes every time the user moves the map
    addMapListener(
        object : MapListener {
          override fun onScroll(event: ScrollEvent?): Boolean {
            hikingRoutesViewModel.setArea(mapView.boundingBox)
            return true
          }

          override fun onZoom(event: ZoomEvent?): Boolean {
            hikingRoutesViewModel.setArea(mapView.boundingBox)
            return true
          }
        })
  }

  SideBarNavigation(
      onTabSelect = { navigationActions.navigateTo(it) },
      tabList = LIST_TOP_LEVEL_DESTINATIONS,
      selectedItem = Route.MAP) {
        Box(modifier = Modifier.fillMaxSize().testTag(Screen.MAP)) {
          // Jetpack Compose is a relatively recent framework for implementing Android UIs. OSMDroid
          // is
          // an older library that uses Activities, the previous way of doing. The composable
          // AndroidView
          // allows us to use OSMDroid's legacy MapView in a Jetpack Compose layout.
          AndroidView(
              factory = { mapView },
              modifier = Modifier.fillMaxSize().testTag(MapScreen.TEST_TAG_MAP))

          CollapsibleHikesList(hikingRoutesViewModel)
          // Put SideBarNavigation after to make it appear on top of the map and HikeList
        }
      }

  // Load hikes list on first composition of the map screen, but avoid reloading the list
  // on each recomposition, as this will be handled by map events such as zoom or scroll
  LaunchedEffect(Unit) { hikingRoutesViewModel.setArea(mapView.boundingBox) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsibleHikesList(hikingRoutesViewModel: ListOfHikeRoutesViewModel) {
  val scaffoldState = rememberBottomSheetScaffoldState()
  val routes = hikingRoutesViewModel.hikeRoutes.collectAsState()
  val context = LocalContext.current

  // BottomSheetScaffold adds a layout at the bottom of the screen that the user can expand to view
  // the list of hikes
  BottomSheetScaffold(
      scaffoldState = scaffoldState,
      sheetContainerColor = MaterialTheme.colorScheme.surface,
      sheetContent = {
        Column(modifier = Modifier.fillMaxSize().testTag(MapScreen.TEST_TAG_HIKES_LIST)) {
          LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (routes.value.isEmpty()) {
              item {
                // Use a box to center the Text composable of the empty list message
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                  Text(
                      text = context.getString(R.string.map_screen_empty_hikes_list_message),
                      style = MaterialTheme.typography.bodyLarge,
                      // Align the text within the Text composable to the center
                      textAlign = TextAlign.Center)
                }
              }
            } else {
              items(routes.value.size) { index: Int ->
                HikingRouteItem(
                    title = routes.value[index],
                    altitudeDifference = 1000,
                    isSuitable = index % 2 == 0,
                    onClick = {
                      Toast.makeText(
                              context, "Hike details not implemented yet", Toast.LENGTH_SHORT)
                          .show()
                    })
              }
            }
          }
        }
      },
      sheetPeekHeight = 400.dp) {}
}

@Composable
fun HikingRouteItem(
    title: String,
    altitudeDifference: Int,
    isSuitable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

  val suitableLabelColor = if (isSuitable) Color(0xFF4CAF50) else Color(0xFFFFC107)
  val suitableLabelText =
      if (isSuitable) LocalContext.current.getString(R.string.map_screen_suitable_hike_label)
      else LocalContext.current.getString(R.string.map_screen_challenging_hike_label)

  Row(
      modifier =
          modifier
              .fillMaxWidth()
              .clickable(onClick = onClick)
              .padding(16.dp, 8.dp)
              .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
              .testTag(MapScreen.TEST_TAG_HIKE_ITEM),
      verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
              text = title,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(8.dp))

          Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(
                    modifier =
                        Modifier.weight(1f)
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)))

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                  Text(text = "Altitude difference", style = MaterialTheme.typography.bodySmall)
                  Text(
                      text = "${altitudeDifference}m",
                      style = MaterialTheme.typography.bodyLarge,
                      fontWeight = FontWeight.Bold)
                }
              }

          Spacer(modifier = Modifier.height(4.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isSuitable) Icons.Default.Check else Icons.Default.Warning,
                // The icon is only decorative, the following message is enough for accessibility
                contentDescription = null,
                tint = suitableLabelColor,
                modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = suitableLabelText,
                style = MaterialTheme.typography.bodySmall,
                color = suitableLabelColor)
          }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Arrow icon to indicate that the item is clickable
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription =
                LocalContext.current.getString(
                    R.string.map_screen_hike_details_content_description),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp))
      }
}
