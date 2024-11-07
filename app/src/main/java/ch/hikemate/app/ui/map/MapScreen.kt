package ch.hikemate.app.ui.map

import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.hikemate.app.R
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.ListOfHikeRoutesViewModel
import ch.hikemate.app.ui.components.HikeCard
import ch.hikemate.app.ui.navigation.LIST_TOP_LEVEL_DESTINATIONS
import ch.hikemate.app.ui.navigation.NavigationActions
import ch.hikemate.app.ui.navigation.Route
import ch.hikemate.app.ui.navigation.Screen
import ch.hikemate.app.ui.navigation.SideBarNavigation
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

object MapScreen {
  /**
   * (Config) Arbitrary number defined empirically to avoid performance issues caused by drawing too
   * many hikes on the map when the user zoomed out too much and launched a search.
   *
   * The number is arbitrary and can be adjusted based on the performance of the app. As an
   * indication, it was tested with 149 hikes and already skipped a few frames, enough for a user to
   * be surprised by the lag.
   */
  const val MAX_HIKES_DRAWN_ON_MAP = 100

  /**
   * (Config) Height of the bottom sheet when it is collapsed. The height is defined empirically to
   * show a few items of the list of hikes and allow the user to expand it to see more.
   */
  val BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT = 400.dp

  /**
   * (Config) Initial zoom level of the map. The zoom level is defined empirically to show a
   * reasonable area of the map when the user opens the screen.
   */
  const val MAP_INITIAL_ZOOM = 12.0

  /**
   * (Config) Maximum zoom level of the map. The zoom level is defined empirically to avoid the user
   * zooming in too much and seeing the map tiles pixelated.
   */
  const val MAP_MAX_ZOOM = 18.0

  /**
   * (Config) Minimum zoom level of the map. The zoom level is defined empirically to avoid the user
   * zooming out too much and seeing too much of the blank background behind the map tiles while
   * still being able to see a reasonable area of the world map.
   */
  const val MAP_MIN_ZOOM = 2.5

  /** (Config) Initial position of the center of the map. */
  val MAP_INITIAL_CENTER = GeoPoint(46.5, 6.6)

  /** (Config) Width of the stroke of the lines that represent the hikes on the map. */
  const val STROKE_WIDTH = 10f

  /**
   * (Config) Interval in milliseconds between updates of the user's location. The interval is
   * defined empirically to avoid draining the battery too much while still providing a good
   * experience to the user.
   */
  const val USER_LOCATION_UPDATE_INTERVAL = 5000L

  /**
   * (Config) Duration in milliseconds of the animation when centering the map on the user's
   * location. The duration is defined empirically to make the transition smooth and not too fast.
   * The value is arbitrary and can be adjusted based on the user's experience.
   */
  private const val CENTER_MAP_ANIMATION_TIME = 500L

  const val MIN_HUE = 0
  const val MAX_HUE = 360
  const val MIN_SATURATION = 42
  const val MAX_SATURATION = 98
  const val MIN_LIGHTNESS = 40
  const val MAX_LIGHTNESS = 90

  const val LOG_TAG = "MapScreen"

  const val TEST_TAG_MAP = "map"
  const val TEST_TAG_SEARCH_BUTTON = "searchButton"
  const val TEST_TAG_HIKES_LIST = "hikesList"
  const val TEST_TAG_HIKE_ITEM = "hikeItem"
  const val TEST_TAG_EMPTY_HIKES_LIST_MESSAGE = "emptyHikesListMessage"
  const val TEST_TAG_SEARCHING_MESSAGE = "searchingMessage"
  const val TEST_TAG_SEARCH_LOADING_ANIMATION = "searchLoadingAnimation"

  /**
   * Draws a new marker on the map representing the user's position. The previous marker is cleared
   * to avoid duplicates. The map is invalidated to redraw the map with the new marker.
   *
   * @param previous The previous marker representing the user's position
   * @param mapView The map view where the marker will be displayed
   * @param location The new location of the user
   * @return The new marker representing the user's position
   */
  fun updateUserPosition(previous: Marker?, mapView: MapView, location: Location): Marker {
    // Clear the previous marker to avoid duplicates
    previous?.let { mapView.overlays.remove(it) }

    // Create a new marker with the new position
    val newMarker =
        Marker(mapView).apply {
          position = GeoPoint(location.latitude, location.longitude)
          setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        }

    // Add the new marker to the map
    mapView.overlays.add(newMarker)
    mapView.invalidate()

    return newMarker
  }

  /**
   * Centers the map on the user's location.
   *
   * Animates the map so that the transition is smooth and not instant.
   *
   * @param mapView The map view to center
   * @param location The user's location to center the map on
   */
  fun centerMapOnUserLocation(mapView: MapView, location: Location) {
    mapView.controller.animateTo(
        GeoPoint(location.latitude, location.longitude),
        mapView.zoomLevelDouble,
        CENTER_MAP_ANIMATION_TIME)
  }
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

/**
 * Clears all hikes that are displayed on the map. Intended to be used when the list of hikes
 * changes and new hikes need to be drawn.
 */
fun clearHikesFromMap(mapView: MapView) {
  mapView.overlays.clear()
  mapView.invalidate()
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    navigationActions: NavigationActions,
    hikingRoutesViewModel: ListOfHikeRoutesViewModel =
        viewModel(factory = ListOfHikeRoutesViewModel.Factory),
    mapInitialZoomLevel: Double = MapScreen.MAP_INITIAL_ZOOM,
    mapMaxZoomLevel: Double = MapScreen.MAP_MAX_ZOOM,
    mapMinZoomLevel: Double = MapScreen.MAP_MIN_ZOOM,
    mapInitialCenter: GeoPoint = MapScreen.MAP_INITIAL_CENTER,
) {
  val context = LocalContext.current
  val locationPermissionState =
      rememberMultiplePermissionsState(
          permissions =
              listOf(
                  android.Manifest.permission.ACCESS_FINE_LOCATION,
                  android.Manifest.permission.ACCESS_COARSE_LOCATION))

  // Only do the configuration on the first composition, not on every recomposition
  LaunchedEffect(Unit) {
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
  }

  // Avoid re-creating the MapView on every recomposition
  val mapView = remember {
    MapView(context).apply {
      // Set map's initial state
      controller.setZoom(mapInitialZoomLevel)
      controller.setCenter(mapInitialCenter)
      // Limit the zoom to avoid the user zooming out or out too much
      minZoomLevel = mapMinZoomLevel
      maxZoomLevel = mapMaxZoomLevel
      // Avoid repeating the map when the user reaches the edge or zooms out
      isHorizontalMapRepetitionEnabled = false
      isVerticalMapRepetitionEnabled = false
      // Disable built-in zoom controls since we have our own
      zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
      // Enable touch-controls such as pinch to zoom
      setMultiTouchControls(true)
    }
  }
  var userLocationMarker: Marker? by remember { mutableStateOf(null) }

  val locationUpdatedCallback =
      object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
          Log.d(MapScreen.LOG_TAG, "Location updated: $locationResult")
          val location = locationResult.lastLocation
          if (location != null) {
            userLocationMarker = MapScreen.updateUserPosition(userLocationMarker, mapView, location)
          } else {
            Log.e(MapScreen.LOG_TAG, "Location is null in callback")
            // TODO : Localize the toast's text
            Toast.makeText(context, "No location available", Toast.LENGTH_LONG).show()
          }
        }
      }
  LaunchedEffect(locationPermissionState.revokedPermissions) {
    Log.d(
        MapScreen.LOG_TAG,
        "Location permission state changed. Revoked permissions: ${locationPermissionState.revokedPermissions}")
    // If the user has granted at least coarse location, periodically update the user's location
    if (locationPermissionState.revokedPermissions.size !=
        locationPermissionState.permissions.size) {
      Log.d(MapScreen.LOG_TAG, "Location permission granted, requesting location updates")
      val fusedLocationClient: FusedLocationProviderClient =
          LocationServices.getFusedLocationProviderClient(context)

      try {
        fusedLocationClient.requestLocationUpdates(
            LocationRequest.Builder(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    MapScreen.USER_LOCATION_UPDATE_INTERVAL)
                .build(),
            locationUpdatedCallback,
            Looper.getMainLooper())
        Log.d(MapScreen.LOG_TAG, "Location updates requested")
      } catch (e: SecurityException) {
        Log.e(MapScreen.LOG_TAG, "Security exception while accessing location", e)
        // TODO : Localize the toast's text
        Toast.makeText(
                context, "Permission error while accessing your location.", Toast.LENGTH_LONG)
            .show()
      }
    }
  }

  // Keep track of whether a search for hikes is ongoing
  var isSearching by remember { mutableStateOf(false) }

  // Show hikes on the map
  val routes by hikingRoutesViewModel.hikeRoutes.collectAsState()
  LaunchedEffect(routes) {
    clearHikesFromMap(mapView)
    if (routes.size <= MapScreen.MAX_HIKES_DRAWN_ON_MAP) {
      routes.forEach { showHikeOnMap(mapView, it, getRandomColor()) }
      Log.d(MapScreen.LOG_TAG, "Displayed ${routes.size} hikes on the map")
    } else {
      routes.subList(0, MapScreen.MAX_HIKES_DRAWN_ON_MAP).forEach {
        showHikeOnMap(mapView, it, getRandomColor())
      }
      Toast.makeText(
              context,
              context.getString(
                  R.string.map_screen_too_many_hikes_message, MapScreen.MAX_HIKES_DRAWN_ON_MAP),
              Toast.LENGTH_LONG)
          .show()
      Log.d(MapScreen.LOG_TAG, "Too many hikes (${routes.size}) to display on the map")
    }
  }

  SideBarNavigation(
      onTabSelect = { navigationActions.navigateTo(it) },
      tabList = LIST_TOP_LEVEL_DESTINATIONS,
      selectedItem = Route.MAP) {
        Box(modifier = Modifier.fillMaxSize().testTag(Screen.MAP)) {
          // Jetpack Compose is a relatively recent framework for implementing Android UIs.
          // OSMDroid
          // is
          // an older library that uses Activities, the previous way of doing. The composable
          // AndroidView
          // allows us to use OSMDroid's legacy MapView in a Jetpack Compose layout.
          AndroidView(
              factory = { mapView },
              modifier =
                  Modifier.fillMaxSize()
                      .testTag(MapScreen.TEST_TAG_MAP)
                      .padding(bottom = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT))

          // Button to center the map on the user's location
          MapMyLocationButton(
              onClick = {
                // If the user has granted at least one of the two permissions, center the map on
                // the user's location
                if (locationPermissionState.revokedPermissions.size !=
                    locationPermissionState.permissions.size) {
                  val fusedLocationClient: FusedLocationProviderClient =
                      LocationServices.getFusedLocationProviderClient(context)

                  try {
                    fusedLocationClient
                        .getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            CancellationTokenSource().token)
                        .addOnSuccessListener {
                          if (it != null) {
                            MapScreen.centerMapOnUserLocation(mapView, it)
                          } else {
                            Log.e(MapScreen.LOG_TAG, "Location is null")
                            // TODO : Localize the toast's text
                            Toast.makeText(context, "No location available", Toast.LENGTH_SHORT)
                                .show()
                          }
                        }
                        .addOnFailureListener {
                          Log.e(MapScreen.LOG_TAG, "Error while accessing location", it)
                          // TODO : Localize the toast's text
                          Toast.makeText(
                                  context,
                                  "Error while accessing your location.",
                                  Toast.LENGTH_LONG)
                              .show()
                        }
                    //                  fusedLocationClient.lastLocation.addOnSuccessListener {
                    // location ->
                    //                    if (location != null) {
                    //                      mapView.controller.setCenter(GeoPoint(location.latitude,
                    // location.longitude))
                    //                    }
                    //                    else {
                    //                      Log.e(MapScreen.LOG_TAG, "Location is null")
                    //                      // TODO : Localize the toast's text
                    //                      Toast.makeText(context, "No location available",
                    // Toast.LENGTH_LONG).show()
                    //                    }
                    //                  }.addOnFailureListener { e ->
                    //                    Log.e(MapScreen.LOG_TAG, "Error while accessing location",
                    // e)
                    //                    // TODO : Localize the toast's text
                    //                    Toast.makeText(context, "Error while accessing your
                    // location.", Toast.LENGTH_LONG).show()
                    //                  }
                  } catch (e: SecurityException) {
                    Log.e(MapScreen.LOG_TAG, "Security exception while accessing location", e)
                    // TODO : Localize the toast's text
                    Toast.makeText(
                            context,
                            "Permission error while accessing your location.",
                            Toast.LENGTH_LONG)
                        .show()
                  }
                }
                // If the user yet needs to grant the permission, show a custom educational alert
                else {
                  // TODO : Show a custom alert that then requests the permission instead of directly requesting it
                  locationPermissionState.launchMultiplePermissionRequest()
                }
              },
              modifier =
                  Modifier.align(Alignment.BottomStart)
                      .padding(bottom = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT + 8.dp))
          // Search button to request OSM for hikes in the displayed area
          if (!isSearching) {
            MapSearchButton(
                onClick = {
                  if (isSearching) return@MapSearchButton
                  isSearching = true
                  hikingRoutesViewModel.setArea(
                      mapView.boundingBox,
                      onSuccess = { isSearching = false },
                      onFailure = {
                        isSearching = false
                        Handler(Looper.getMainLooper()).post {
                          Toast.makeText(
                                  context, "Error while searching for hikes", Toast.LENGTH_SHORT)
                              .show()
                        }
                      })
                },
                modifier =
                    Modifier.align(Alignment.BottomCenter)
                        .padding(bottom = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT + 8.dp))
          }
          // The zoom buttons are displayed on the bottom left of the screen
          ZoomMapButton(
              onZoomIn = { mapView.controller.zoomIn() },
              onZoomOut = { mapView.controller.zoomOut() },
              modifier =
                  Modifier.align(Alignment.BottomEnd)
                      .padding(bottom = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT + 8.dp))
          CollapsibleHikesList(hikingRoutesViewModel, isSearching)
          // Put SideBarNavigation after to make it appear on top of the map and HikeList
        }
      }
}

@Composable
fun MapSearchButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
  Button(
      onClick = onClick,
      modifier = modifier.testTag(MapScreen.TEST_TAG_SEARCH_BUTTON),
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Text(
            text = LocalContext.current.getString(R.string.map_screen_search_button_text),
            color = MaterialTheme.colorScheme.onSurface)
      }
}

@Composable
fun MapMyLocationButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
  // TODO : Improve the button's appearance
  IconButton(onClick = onClick, modifier = modifier) {
    Icon(
        painter = painterResource(id = R.drawable.my_location),
        contentDescription = "Center map on my position") // TODO : Localize this text
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsibleHikesList(hikingRoutesViewModel: ListOfHikeRoutesViewModel, isSearching: Boolean) {
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
            if (isSearching) {
              item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()) {
                      Text(
                          text = "Searching for hikes...",
                          style = MaterialTheme.typography.bodyLarge,
                          textAlign = TextAlign.Center,
                          modifier =
                              Modifier.padding(bottom = 16.dp)
                                  .testTag(MapScreen.TEST_TAG_SEARCHING_MESSAGE))
                      CircularProgressIndicator(
                          modifier = Modifier.testTag(MapScreen.TEST_TAG_SEARCH_LOADING_ANIMATION))
                    }
              }
            } else if (routes.value.isEmpty()) {
              item {
                // Use a box to center the Text composable of the empty list message
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                  Text(
                      text = context.getString(R.string.map_screen_empty_hikes_list_message),
                      style = MaterialTheme.typography.bodyLarge,
                      // Align the text within the Text composable to the center
                      textAlign = TextAlign.Center,
                      modifier = Modifier.testTag(MapScreen.TEST_TAG_EMPTY_HIKES_LIST_MESSAGE))
                }
              }
            } else {
              items(routes.value.size) { index: Int ->
                val route = routes.value[index]
                val isSuitable = index % 2 == 0
                HikeCardFor(route, isSuitable, hikingRoutesViewModel)
              }
            }
          }
        }
      },
      sheetPeekHeight = MapScreen.BOTTOM_SHEET_SCAFFOLD_MID_HEIGHT) {}
}

@Composable
fun HikeCardFor(route: HikeRoute, isSuitable: Boolean, viewModel: ListOfHikeRoutesViewModel) {
  // The context is needed to display a toast when the user clicks on the route
  val context = LocalContext.current

  // The color of the card's message is chosen based on whether the hike is suitable or not
  val suitableLabelColor = if (isSuitable) Color(0xFF4CAF50) else Color(0xFFFFC107)

  // The text and icon of the card's message are chosen based on whether the hike is suitable or not
  val suitableLabelText =
      if (isSuitable) LocalContext.current.getString(R.string.map_screen_suitable_hike_label)
      else LocalContext.current.getString(R.string.map_screen_challenging_hike_label)

  // The icon of the card's message is chosen based on whether the hike is suitable or not
  val suitableLabelIcon = if (isSuitable) R.drawable.check_circle else R.drawable.warning

  HikeCard(
      title = route.name ?: stringResource(R.string.map_screen_hike_title_default),
      altitudeDifference = 1000,
      onClick = {
        // The user clicked on the route to select it
        viewModel.selectRoute(route)
        Toast.makeText(context, "Hike details not implemented yet", Toast.LENGTH_SHORT).show()
      },
      messageContent = suitableLabelText,
      messageIcon = painterResource(suitableLabelIcon),
      messageColor = suitableLabelColor)
}
