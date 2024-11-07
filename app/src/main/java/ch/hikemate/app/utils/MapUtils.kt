package ch.hikemate.app.utils

import android.util.Log
import ch.hikemate.app.model.route.Bounds
import kotlin.math.cos
import org.osmdroid.util.GeoPoint

fun getGeographicalCenter(bounds: Bounds): GeoPoint {
  Log.d(
      "Bounds",
      "minLat: ${bounds.minLat}, maxLat: ${bounds.maxLat}, minLon: ${bounds.minLon}, maxLon: ${bounds.maxLon}")
  val minLong = bounds.minLon
  val maxLong = bounds.maxLon
  val minLat = bounds.minLat
  val maxLat = bounds.maxLat

  val centerLat = (minLat + maxLat) / 2
  var centerLong = (minLong + maxLong) / 2

  // Adjust if the longitude crosses the Date Line (i.e., difference > 180 degrees)
  if (maxLong - minLong > 180) {
    centerLong = (minLong + maxLong + 360) / 2
  } else if (minLong - maxLong > 180) {
    centerLong = (minLong + maxLong - 360) / 2
  }

  // Normalize longitude to be in the range -180 to 180
  if (centerLong > 180) {
    centerLong -= 360
  } else if (centerLong < -180) {
    centerLong += 360
  }

  Log.d("Bounds", "centerLat: $centerLat, centerLong: $centerLong")

  return GeoPoint(centerLat, centerLong)
}

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

  val zoomLevels =
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
      zoomLevels.indexOfFirst { it <= maxDegreeDiff }.takeIf { it != -1 } ?: (zoomLevels.size - 1)

  return bestZoomLevel
}
