package ch.hikemate.app.model.route

import android.util.Log
import ch.hikemate.app.model.elevation.ElevationServiceRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

/**
 * Represents a detailed hike route, a hike route with additional computed attributes:
 * - totalDistance: The total distance of the route in meters
 * - estimatedTime: The estimated time to complete the route in minutes
 * - elevationGain: The total elevation gain in meters
 * - difficulty: The difficulty level of the route
 *
 * @param hikeRoute The base route data, including ID, bounds, waypoints, name, and description
 * @param totalDistance The total distance of the route in meters
 * @param estimatedTime The estimated time to complete the route in minutes
 * @param elevationGain The total elevation gain in meters
 * @param difficulty The difficulty level of the route
 */
data class DetailedHikeRoute(
    val hikeRoute: HikeRoute,
    val totalDistance: Double,
    val estimatedTime: Double,
    val elevationGain: Double,
    val difficulty: String
)

/**
 * Computes a DetailedHikeRoute from a base HikeRoute by calculating additional attributes.
 *
 * @param hikeRoute The base route data from which detailed information will be computed
 * @return A DetailedHikeRoute with computed data
 */
fun createDetailedHikeRoute(hikeRoute: HikeRoute): DetailedHikeRoute {

  val totalDistance = computeTotalDistance(hikeRoute.ways)
  val elevationGain = computeElevationGain(hikeRoute.ways, hikeRoute.id)
  val estimatedTime = estimateTime(totalDistance, elevationGain)
  val difficulty = determineDifficulty(totalDistance, elevationGain)

  return DetailedHikeRoute(
      hikeRoute = hikeRoute,
      totalDistance = totalDistance,
      estimatedTime = estimatedTime,
      elevationGain = elevationGain,
      difficulty = difficulty,
  )
}

/**
 * Helper function to compute the total distance in meters of a hike based on a list of waypoints.
 */
fun computeTotalDistance(ways: List<LatLong>): Double {
  return ways.zipWithNext { point1, point2 -> point1.distanceTo(point2) }.sum()
}

/**
 * Helper function to compute the total elevation gain based on a list of waypoints. Note: Assuming
 * LatLong has an altitude parameter, or adapt this method as needed.
 */
fun computeElevationGain(ways: List<LatLong>, hikeId: String): Double = runBlocking {
  val okHttpClient = OkHttpClient()
  val elevationService = ElevationServiceRepository(client = okHttpClient)

  // Since elevationService.getElevation is asynchronous, we use a CompletableDeferred to wait for
  // the result
  val deferredResult = CompletableDeferred<List<Double>>()

  elevationService.getElevation(
      coordinates = ways,
      hikeID = hikeId,
      onSuccess = { elevation -> deferredResult.complete(elevation) },
      onFailure = { deferredResult.complete(emptyList()) })

  val elevations = deferredResult.await()
  Log.d("Elevation", "Got the elevation from the elevation repository: ${elevations}")

  // calculate the elevation gain
  var elevationGain = 0.0
  for (i in 0 until elevations.size - 1) {
    val diff = elevations[i + 1] - elevations[i]
    if (diff > 0) {
      elevationGain += diff
    }
  }

  Log.d("Elevation", "Calculated elevation gain for hike ${hikeId}: ${elevationGain}")

  return@runBlocking elevationGain
}

/**
 * Helper function to estimate the time based on distance and elevation gain. The calculation is
 * based on Naismith's rule, which assumes reasonable fitness, typical terrain and normal conditions
 * - 0.012 minutes per meter of distance (12 min/km)
 * - 0.1 minutes for every meter of elevation gain (10 min/100m of elevation gain )
 *
 * @link https://en.wikipedia.org/wiki/Naismith%27s_rule
 *
 * TODO parameters
 */
fun estimateTime(distance: Double, elevationGain: Double): Double {
  return (distance * 0.012) + (elevationGain * 0.1)
}

/**
 * Helper function to determine difficulty based on distance and elevation gain. The calculation is
 * loosely based on the California Department of Parks & Recreation's trail difficulty rating
 * system:
 * - Easy: Less than 3 km in length and less than 250 meters of elevation gain
 * - Moderate: 3-6 km in length or 250-500 meters of elevation gain
 * - Hard: more than 6 km in length or more than 500 meters of elevation gain
 *
 * @link https://www.parks.ca.gov/?page_id=24055
 * @todo parameters
 */
fun determineDifficulty(distance: Double, elevationGain: Double): String {
  return when {
    distance < 3000 && elevationGain < 250 -> "Easy"
    distance in 3000.0..6000.0 || elevationGain in 250.0..500.0 -> "Moderate"
    else -> "Difficult"
  }
}
