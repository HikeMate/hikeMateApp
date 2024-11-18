package ch.hikemate.app.utils

import ch.hikemate.app.model.elevation.ElevationServiceRepository
import ch.hikemate.app.model.route.HikeDifficulty
import ch.hikemate.app.model.route.LatLong
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking

object RouteUtils {
  /**
   * Helper function to calculate the elevation gain based on a list of elevations.
   *
   * @param elevations A list of elevations in meters.
   * @return The total elevation gain in meters as a `Double`.
   */
  fun calculateElevationGain(elevations: List<Double>): Double {
    var elevationGain = 0.0
    for (i in 0 until elevations.size - 1) {
      val diff = elevations[i + 1] - elevations[i]
      if (diff > 0) {
        elevationGain += diff
      }
    }
    return elevationGain
  }

  /**
   * Helper function to compute the total distance in km of a hike based on a list of waypoints. The
   * distance calculation accounts for Earth's curvature through use of the Haversine formula
   *
   * @param ways A list of `LatLong` objects representing the waypoints of the hike.
   * @return The total distance of the hike in kilometers as a `Double`.
   */
  fun computeTotalDistance(ways: List<LatLong>): Double {
    return ways.zipWithNext { point1, point2 -> point1.distanceTo(point2) }.sum() / 1000
  }

  /**
   * Helper function to compute the total elevation gain based on a list of waypoints.
   *
   * @param ways A list of `LatLong` objects representing the waypoints of the hike.
   * @param hikeId the hikes id, needed for the elevation API request
   * @return The total elevation gain in meters as a `Double`.
   */
  fun getElevationGain(
      ways: List<LatLong>,
      hikeId: String,
      elevationService: ElevationServiceRepository
  ): Double = runBlocking {

    // Since elevationService.getElevation is asynchronous, we use a CompletableDeferred to wait for
    // the result
    val deferredResult = CompletableDeferred<List<Double>>()

    elevationService.getElevation(
        coordinates = ways,
        hikeID = hikeId,
        onSuccess = { elevation -> deferredResult.complete(elevation) },
        onFailure = { deferredResult.complete(emptyList()) })

    val elevations = deferredResult.await()

    return@runBlocking calculateElevationGain(elevations)
  }

  /**
   * Helper function to estimate the time based on distance and elevation gain. The calculation is
   * based on Naismith's rule, which assumes reasonable fitness, typical terrain and normal
   * conditions
   * - 12 minutes per kilometer
   * - 0.1 minutes per meter of elevation gain (10 min/100m of elevation gain)
   *
   * @param distance The distance of the hike in kilometers.
   * @param elevationGain The elevation gain of the hike in meters.
   * @return The estimated time for the hike in minutes as a `Double`.
   * @link https://en.wikipedia.org/wiki/Naismith%27s_rule
   */
  fun estimateTime(distance: Double, elevationGain: Double): Double {
    return (distance * 12) + (elevationGain * 0.1)
  }

  /**
   * Helper function to determine difficulty based on distance and elevation gain. The calculation
   * is loosely based on the California Department of Parks & Recreation's trail difficulty rating
   * system:
   * - Easy: Less than 3 km in length and less than 250 meters of elevation gain
   * - Moderate: 3-6 km in length or 250-500 meters of elevation gain
   * - Hard: more than 6 km in length or more than 500 meters of elevation gain
   *
   * @param distance The total distance of the hike in kilometers.
   * @param elevationGain The total elevation gain of the hike in meters.
   * @return A `String` representing the difficulty level: "Easy", "Moderate", or "Difficult".
   * @link https://www.parks.ca.gov/?page_id=24055
   */
  fun determineDifficulty(distance: Double, elevationGain: Double): HikeDifficulty {
    return when {
      distance < 3 && elevationGain < 250 -> HikeDifficulty.EASY
      distance in 0.0..6.0 && elevationGain in 0.0..500.0 -> HikeDifficulty.MODERATE
      else -> HikeDifficulty.DIFFICULT
    }
  }
}