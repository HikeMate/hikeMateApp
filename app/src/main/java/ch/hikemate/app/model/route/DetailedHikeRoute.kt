package ch.hikemate.app.model.route

import ch.hikemate.app.model.elevation.ElevationRepository
import ch.hikemate.app.model.elevation.ElevationServiceRepository
import ch.hikemate.app.utils.RouteUtils
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

/**
 * Represents a detailed hike route, a hike route with additional computed attributes:
 * - totalDistance: The total distance of the route in meters
 * - estimatedTime: The estimated time to complete the route in minutes
 * - elevationGain: The total elevation gain in meters
 * - difficulty: The difficulty level of the route
 *
 * @param route The base route data, including ID, bounds, waypoints, name, and description
 * @param totalDistance The total distance of the route in km
 * @param estimatedTime The estimated time to complete the route in minutes
 * @param elevationGain The total elevation gain in meters
 * @param difficulty The difficulty level of the route
 */
data class DetailedHikeRoute(
    val route: HikeRoute,
    val totalDistance: Double,
    val estimatedTime: Double,
    val elevationGain: Double,
    val difficulty: HikeDifficulty
) {

  /** Companion object that creates the detailed attributes for the hike route */
  companion object {
    /**
     * Creates a detailed hike route with computed attributes
     *
     * @param hikeRoute The route for which detailed information will be computed
     * @param elevationRepository The elevation repository to use for computing elevation gain.
     *   Initialized automatically by default
     * @return A DetailedHikeRoute object with the computed attributes: totalDistance,
     *   elevationGain, estimatedTime, and difficulty
     */
    fun create(
        hikeRoute: HikeRoute,
        elevationRepository: ElevationRepository = ElevationServiceRepository(OkHttpClient())
    ): DetailedHikeRoute {

      val totalDistance = RouteUtils.computeTotalDistance(hikeRoute.ways)
      val elevationGain = runBlocking {
        RouteUtils.getElevationGain(hikeRoute.ways, elevationRepository)
      }
      val estimatedTime = RouteUtils.estimateTime(totalDistance, elevationGain)
      val difficulty = RouteUtils.determineDifficulty(totalDistance, elevationGain)

      return DetailedHikeRoute(
          route = hikeRoute,
          totalDistance = totalDistance,
          estimatedTime = estimatedTime,
          elevationGain = elevationGain,
          difficulty = difficulty)
    }
  }
}
