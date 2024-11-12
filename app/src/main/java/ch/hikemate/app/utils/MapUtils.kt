package ch.hikemate.app.utils

import android.util.Log
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.ui.map.MapScreen
import kotlin.math.cos
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

object MapUtils {

  const val LOG_TAG = "MapUtils"

  /**
   * Shows a hike on the map.
   *
   * @param mapView The map view where the hike will be shown.
   * @param hike The hike to be shown.
   * @param color The color of the hike.
   */
  fun showHikeOnMap(
      mapView: MapView,
      hike: HikeRoute,
      color: Int,
      onLineClick: () -> Unit,
  ) {
    val line = Polyline()

    line.setPoints(hike.ways.map { GeoPoint(it.lat, it.lon) })
    line.outlinePaint.color = color
    line.outlinePaint.strokeWidth = MapScreen.STROKE_WIDTH

    line.setOnClickListener { _, _, _ ->
      onLineClick()
      true
    }

    mapView.overlays.add(line)
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
    val maxDegreeDiff = kotlin.math.max(latDiff, adjustedLongDiff)

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
}
