package ch.hikemate.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ch.hikemate.app.R
import ch.hikemate.app.model.facilities.FacilitiesViewModel
import ch.hikemate.app.model.facilities.Facility
import ch.hikemate.app.model.facilities.FacilityType.Companion.mapFacilityTypeToDrawable
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.DetailedHike
import ch.hikemate.app.model.route.LatLong
import ch.hikemate.app.ui.map.HikeDetailScreen
import ch.hikemate.app.ui.map.MapInitialValues
import ch.hikemate.app.ui.map.MapScreen
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

object MapUtils {
  private const val LOG_TAG = "MapUtils"
  private const val MIN_DISTANCE_BETWEEN_FACILITIES = 15
  const val FACILITIES_RELATED_OBJECT_NAME = "facility_marker"
  const val ROUTE_PRIORITY_DISPLAY = 0

  /**
   * Shows a hike on the map.
   *
   * @param mapView The map view where the hike will be shown.
   * @param waypoints The points that compose the line to show on the map.
   * @param color The color of the hike.
   * @param withMarker Whether to show a marker at the starting point of the hike.
   * @param onLineClick To be called when the line on the map is clicked.
   */
  fun showHikeOnMap(
      mapView: MapView,
      waypoints: List<LatLong>,
      color: Int,
      withMarker: Boolean = false,
      onLineClick: () -> Unit,
  ) {
    val line = Polyline()

    line.setPoints(waypoints.map { GeoPoint(it.lat, it.lon) })
    line.outlinePaint.color = color
    line.outlinePaint.strokeWidth = MapScreen.STROKE_WIDTH

    line.setOnClickListener { _, _, _ ->
      onLineClick()
      true
    }

    // The index provides the lowest priority so that the facilities and other overlays
    // are always displayed on top of it.
    mapView.overlays.add(ROUTE_PRIORITY_DISPLAY, line)

    if (withMarker) {
      val startingMarker =
          Marker(mapView).apply {
            // Dynamically create the custom icon
            icon =
                AppCompatResources.getDrawable(mapView.context, R.drawable.location_on)?.apply {
                  setTint(color)
                }
            position = GeoPoint(waypoints.first().lat, waypoints.first().lon)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setOnMarkerClickListener({ _, _ ->
              onLineClick()
              true
            })
          }
      mapView.overlays.add(startingMarker)
    }
    mapView.invalidate()
  }

  /**
   * Calculates the geographical center of a given bounding box. This function computes the center
   * latitude as the average of the minimum and maximum latitudes, and the center longitude as the
   * average of the minimum and maximum longitudes. It also handles edge cases where the longitude
   * crosses the Date Line (±180 degrees), adjusting the center longitude accordingly to ensure it
   * stays within the valid range of -180 to 180 degrees.
   *
   * @param bounds The bounding box (minLat, maxLat, minLon, maxLon) defining the area for which to
   *   calculate the center.
   * @return A `GeoPoint` representing the geographical center of the provided bounds.
   */
  fun getGeographicalCenter(bounds: Bounds): GeoPoint {
    Log.d(
        LOG_TAG,
        "minLat: ${bounds.minLat}, maxLat: ${bounds.maxLat}, minLon: ${bounds.minLon}, maxLon: ${bounds.maxLon}")
    val minLong = bounds.minLon
    val maxLong = bounds.maxLon
    val minLat = bounds.minLat
    val maxLat = bounds.maxLat

    val centerLat = (minLat + maxLat) / 2
    val centerLong = (minLong + maxLong) / 2

    Log.d(LOG_TAG, "getGeographicalCenter: centerLat: $centerLat, centerLong: $centerLong")

    return GeoPoint(centerLat, centerLong)
  }

  /**
   * Calculates the best zoom level for a given bounding box. The zoom level is determined based on
   * the largest degree difference between the latitude and longitude within the bounds, considering
   * latitude compression due to the Earth's spherical shape. This function selects a zoom level
   * that fits the provided area, ensuring the best visibility for the given bounds in a map view.
   *
   * The levels in the var zoomLevels are taken from OSMs 'Zoom levels' documentation:
   * https://wiki.openstreetmap.org/wiki/Zoom_levels
   *
   * @param bounds The bounding box (minLat, maxLat, minLon, maxLon) defining the area for which to
   *   calculate the zoom level.
   * @return An integer representing the best zoom level (lower value means zoomed out, higher value
   *   means zoomed in).
   */
  fun calculateBestZoomLevel(bounds: Bounds): Int {
    val minLong = bounds.minLon
    val maxLong = bounds.maxLon
    val minLat = bounds.minLat
    val maxLat = bounds.maxLat

    val centerLat = (minLat + maxLat) / 2

    val latDiff = maxLat - minLat
    val longDiff = maxLong - minLong

    // Adjust longitude difference based on the latitude compression factor
    val adjustedLongDiff = longDiff * cos(Math.toRadians(centerLat))

    // Calculate the maximum degree difference between lat and adjusted long
    val maxDegreeDiff = max(latDiff, adjustedLongDiff)

    // The coverage of each zoom level in degrees. The Index is the OSM Zoom leve, as per the
    // documentation
    val degreeCoverageIndexedByZoom =
        listOf(
            360.0,
            180.0,
            90.0,
            45.0,
            22.5,
            11.25,
            5.625,
            2.813,
            1.406,
            0.703,
            0.352,
            0.176,
            0.088,
            0.044,
            0.022,
            0.011,
            0.005,
            0.003,
            0.001,
            0.0005,
            0.00025)

    val bestZoomLevel =
        degreeCoverageIndexedByZoom.indexOfFirst { it <= maxDegreeDiff }.takeIf { it != -1 }
            ?: (degreeCoverageIndexedByZoom.size - 1)

    Log.d(LOG_TAG, "calculateBestZoomLevel: bestZoomLevel: $bestZoomLevel")
    return bestZoomLevel
  }

  /**
   * Clears the marker representing the user's position from the map.
   *
   * The map is NOT invalidated. If needed, use [org.osmdroid.views.MapView.invalidate].
   *
   * To update the user's position to a new location, use [updateUserPosition].
   *
   * @param previous The previous marker representing the user's position
   * @param mapView The map view where the marker is displayed
   */
  fun clearUserPosition(previous: Marker?, mapView: MapView, invalidate: Boolean = false) {
    previous?.let { mapView.overlays.remove(it) }
    if (invalidate) {
      mapView.invalidate()
    }
  }

  /**
   * Get the icon to use for the user's location marker.
   *
   * @param context The context where the icon will be used
   * @return The icon to use for the user's location marker
   */
  fun getUserLocationMarkerIcon(context: Context): Drawable {
    // Retrieve the actual drawable resource
    val originalDrawable = AppCompatResources.getDrawable(context, R.drawable.user_location)

    // Resize the vector resource to look good on the map
    val bitmap =
        Bitmap.createBitmap(
            MapScreen.USER_LOCATION_MARKER_ICON_SIZE,
            MapScreen.USER_LOCATION_MARKER_ICON_SIZE,
            Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    originalDrawable?.setBounds(0, 0, canvas.width, canvas.height)
    originalDrawable?.draw(canvas)

    return BitmapDrawable(context.resources, bitmap)
  }

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
    clearUserPosition(previous, mapView)

    // Create a new marker with the new position
    val newMarker =
        Marker(mapView).apply {
          icon = getUserLocationMarkerIcon(mapView.context)
          position = GeoPoint(location.latitude, location.longitude)
          setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
          setOnMarkerClickListener { marker, mapView ->
            mapView.controller.animateTo(
                marker.position,
                mapView.zoomLevelDouble,
                MapScreen.CENTER_MAP_ON_MARKER_ANIMATION_TIME)
            true
          }
        }

    // Add the new marker to the map
    mapView.overlays.add(newMarker)
    mapView.invalidate()

    return newMarker
  }

  /**
   * Retrieves the user's current location and centers the map on it.
   *
   * If the location is not available, the user is notified that they must enable location services.
   *
   * @param context The context where the location is requested
   * @param mapView The map view to center
   * @param userLocationMarker The marker representing the user's location on the map
   * @see [LocationUtils.getUserLocation]
   */
  fun centerMapOnLocation(context: Context, mapView: MapView, userLocationMarker: Marker?) {
    LocationUtils.getUserLocation(
        context = context,
        onLocation = { location ->
          if (location == null) {
            Log.e(LOG_TAG, "User location is null, cannot center map")

            // Clear the user's position from the map
            clearUserPosition(userLocationMarker, mapView, invalidate = true)

            // Notify the user that they must enable location services
            Toast.makeText(
                    context,
                    context.getString(R.string.map_screen_location_not_available),
                    Toast.LENGTH_LONG)
                .show()
          } else {
            // Center the map on the user's location
            centerMapOnLocation(mapView, location)
          }
        })
  }

  /**
   * Centers the map on a given location.
   *
   * Animates the map so that the transition is smooth and not instant.
   *
   * @param mapView The map view to center
   * @param location The location to center the map on
   */
  fun centerMapOnLocation(mapView: MapView, location: Location) {
    mapView.controller.animateTo(
        GeoPoint(location.latitude, location.longitude),
        mapView.zoomLevelDouble,
        MapScreen.CENTER_MAP_ANIMATION_TIME)
  }

  /**
   * Displays a list of facilities into a map, by getting the drawable corresponding to the facility
   * and then drawing it inside the map.
   *
   * @param facilities
   * @param mapView the map view to display the facilities on
   * @param context
   */
  fun displayFacilities(facilities: List<Facility>, mapView: MapView, context: Context) {
    val displayedFacilities = mutableSetOf<GeoPoint>()

    facilities.forEach { facility ->
      // Get the drawable corresponding to the facility type
      val drawable = facility.type.mapFacilityTypeToDrawable(context)

      // Draw the marker in the Map
      drawable?.let {
        val geoPoint = GeoPoint(facility.coordinates.lat, facility.coordinates.lon)
        if (displayedFacilities.none {
          it.distanceToAsDouble(geoPoint) < MIN_DISTANCE_BETWEEN_FACILITIES
        }) {
          displayedFacilities.add(geoPoint)
          Marker(mapView).apply {
            position = geoPoint
            // The icon is the drawable
            icon = it
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            // This disables the click feature since it pops up a text window we haven't
            // handled yet.
            setOnMarkerClickListener { _, _ -> true }
            // The relatedObject makes it easier for them to be all removed at once
            // and enables the possibility of not constantly storing them in memory.
            relatedObject = FACILITIES_RELATED_OBJECT_NAME
            mapView.overlays.add(this)
          }
        }
      }
    }
    // Trigger the map to be drawn again
    mapView.invalidate()
  }
  /**
   * Remove any facility that is being displayed in the map.
   *
   * @param mapView the map view to remove the facilities from
   * @see [displayFacilities]
   */
  fun clearFacilities(mapView: MapView) {
    // Keep track of removed overlays to avoid concurrent modification
    val overlaysToRemove =
        mapView.overlays.filter { overlay ->
          overlay is Marker && overlay.relatedObject == FACILITIES_RELATED_OBJECT_NAME
        }

    mapView.overlays.removeAll(overlaysToRemove)

    // Force garbage collection of markers
    overlaysToRemove.forEach { overlay ->
      if (overlay is Marker) {
        overlay.onDetach(mapView)
      }
    }

    // Clear the MapView's cache
    mapView.invalidate()
  }
  /**
   * Utility function designed to set the mapView listener which updates the state of the
   * BoundingBox and the ZoomLevel
   *
   * @param mapView
   * @param boundingBoxState
   * @param zoomLevelState
   */
  fun setMapViewListenerForStates(
      mapView: MapView,
      boundingBoxState: MutableStateFlow<BoundingBox?>,
      zoomLevelState: MutableStateFlow<Double?>
  ) {
    // Update the map listener to just update the StateFlows
    mapView.addMapListener(
        object : MapListener {
          override fun onScroll(event: ScrollEvent?): Boolean {
            // On a Scroll event the boundingBox will change
            event?.let {
              val newBoundingBox = mapView.boundingBox
              if (newBoundingBox != boundingBoxState.value) {
                boundingBoxState.value = newBoundingBox
              }
            }
            return true
          }

          override fun onZoom(event: ZoomEvent?): Boolean {
            event?.let {
              val newZoomLevel = mapView.zoomLevelDouble
              if (newZoomLevel != zoomLevelState.value) {
                zoomLevelState.value = newZoomLevel
              }
            }
            return true
          }
        })
  }

  /**
   * Centers the map on a given location.
   *
   * Animates the map so that the transition is smooth and not instant.
   *
   * @param mapView The map view to center
   * @param location The location to center the map on
   */
  fun centerMapOnLocation(mapView: MapView, location: GeoPoint) {
    mapView.controller.animateTo(
        location, mapView.zoomLevelDouble, MapScreen.CENTER_MAP_ANIMATION_TIME)
  }

  data class MapViewState(
      val center: GeoPoint = MapInitialValues().mapInitialCenter,
      val zoom: Double = MapInitialValues().mapInitialZoomLevel,
  )

  /**
   * This LaunchedEffect is used for the flow that updates the facilities for the current state of
   * the MapView. It uses a combinedFlow to make debounced updates to the facilities
   *
   * @param mapView
   * @param boundingBoxState
   * @param zoomLevelState
   * @param facilitiesViewModel
   * @param hike
   * @param context
   */
  @OptIn(FlowPreview::class)
  @Composable
  fun LaunchedEffectFacilitiesDisplay(
      mapView: MapView,
      boundingBoxState: MutableStateFlow<BoundingBox?>,
      zoomLevelState: MutableStateFlow<Double?>,
      facilitiesViewModel: FacilitiesViewModel,
      hike: DetailedHike,
      context: Context
  ) {
    LaunchedEffect(Unit) {
      setMapViewListenerForStates(mapView, boundingBoxState, zoomLevelState)

      // Create our combined flow which is limited by a debounce
      val combinedFlow =
          combine(
              boundingBoxState.debounce(HikeDetailScreen.DEBOUNCE_DURATION),
              zoomLevelState.debounce(HikeDetailScreen.DEBOUNCE_DURATION)) { boundingBox, zoomLevel
                ->
                boundingBox to zoomLevel
              }

      try {
        combinedFlow.collectLatest { (boundingBox, zoomLevel) ->
          if (boundingBoxState.value == null ||
              zoomLevelState.value == null ||
              mapView.repository == null)
              return@collectLatest
          facilitiesViewModel.filterFacilitiesForDisplay(
              bounds = boundingBox!!,
              zoomLevel = zoomLevel!!,
              hikeRoute = hike,
              onSuccess = { newFacilities ->
                clearFacilities(mapView)
                if (newFacilities.isNotEmpty()) {
                  displayFacilities(newFacilities, mapView, context)
                }
              },
              onNoFacilitiesForState = { clearFacilities(mapView) })
        }
      } catch (e: Exception) {
        Log.e(HikeDetailScreen.LOG_TAG, "Error in facility updates flow", e)
      }
    }
  }

  /**
   * This LaunchedEffect used for the HikeDetails and RunHike screens is the one that sets the first
   * display of the facilities.
   *
   * @param facilities
   * @param shouldLoadFacilities
   * @param mapView
   * @param facilitiesViewModel
   * @param hike
   * @param context
   * @return the value of the shouldLoadFacilities
   */
  @Composable
  fun launchedEffectLoadingOfFacilities(
      facilities: List<Facility>?,
      shouldLoadFacilities: Boolean,
      mapView: MapView,
      facilitiesViewModel: FacilitiesViewModel,
      hike: DetailedHike,
      context: Context
  ): Boolean {
    var shouldLoadFacilitiesCopy = shouldLoadFacilities
    LaunchedEffect(facilities, shouldLoadFacilitiesCopy) {
      if (facilities != null && mapView.repository != null) {
        facilitiesViewModel.filterFacilitiesForDisplay(
            bounds = mapView.boundingBox,
            zoomLevel = mapView.zoomLevelDouble,
            hikeRoute = hike,
            onSuccess = { newFacilities ->
              clearFacilities(mapView)
              if (newFacilities.isNotEmpty()) {
                displayFacilities(newFacilities, mapView, context)
              }
            },
            onNoFacilitiesForState = { clearFacilities(mapView) })
        // Reset the flag after loading
        shouldLoadFacilitiesCopy = false
      }
    }
    return shouldLoadFacilitiesCopy
  }

  /**
   * Handles mapview first layout listener.
   *
   * @param mapView
   * @param hike
   * @param boundingBoxState
   * @param zoomLevelState
   * @param withBoundLimits whether or not to limit the Bounds of the Hike.
   */
  @Composable
  fun LaunchedEffectMapviewListener(
      mapView: MapView,
      hike: DetailedHike,
      boundingBoxState: MutableStateFlow<BoundingBox?>,
      zoomLevelState: MutableStateFlow<Double?>,
      withBoundLimits: Boolean = true
  ) {
    mapView.addOnFirstLayoutListener { _, _, _, _, _ ->
      // Limit the vertical scrollable area to avoid the user scrolling too far from the hike
      if (withBoundLimits) {
        mapView.setScrollableAreaLimitLatitude(
            min(MapScreen.MAP_MAX_LATITUDE, mapView.boundingBox.latNorth),
            max(MapScreen.MAP_MIN_LATITUDE, mapView.boundingBox.latSouth),
            HikeDetailScreen.MAP_BOUNDS_MARGIN)
        if (hike.bounds.maxLon < HikeDetailScreen.MAP_MAX_LONGITUDE ||
            hike.bounds.minLon > HikeDetailScreen.MAP_MIN_LONGITUDE) {
          mapView.setScrollableAreaLimitLongitude(
              max(HikeDetailScreen.MAP_MIN_LONGITUDE, mapView.boundingBox.lonWest),
              min(HikeDetailScreen.MAP_MAX_LONGITUDE, mapView.boundingBox.lonEast),
              HikeDetailScreen.MAP_BOUNDS_MARGIN)
        }
        boundingBoxState.value = mapView.boundingBox
        zoomLevelState.value = mapView.zoomLevelDouble
      }
    }
  }
}
