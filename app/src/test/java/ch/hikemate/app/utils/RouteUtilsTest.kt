package ch.hikemate.app.utils

import ch.hikemate.app.model.elevation.ElevationServiceRepository
import ch.hikemate.app.model.route.HikeDifficulty
import ch.hikemate.app.model.route.LatLong
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteUtilsTest {

  @Test
  fun test_calculateElevationGain_returns_correct_elevation_gain() {
    val elevations = listOf(100.0, 150.0, 140.0, 200.0, 190.0)
    val elevationGain = RouteUtils.calculateElevationGain(elevations)
    assertEquals(110.0, elevationGain, 0.01)
  }

  @Test
  fun test_calculateElevationGain_descending_paths() {
    val elevations = listOf(200.0, 190.0, 180.0)
    val elevationGain = RouteUtils.calculateElevationGain(elevations)
    assertEquals(0.0, elevationGain, 0.01)
  }

  @Test
  fun test_computeTotalDistance_returns_correct_distance() {
    val points = listOf(LatLong(46.0, 7.0), LatLong(46.0, 7.01), LatLong(46.01, 7.01))
    val totalDistance = RouteUtils.computeTotalDistance(points)
    assertEquals(1.88, totalDistance, 0.01)
  }

  @Test
  fun test_computeTotalDistance_with_many_waypoints() {
    // list of 30 coordinates
    val points =
        listOf(
            LatLong(46.5, 6.60),
            LatLong(46.55, 6.75),
            LatLong(46.6, 6.80),
            LatLong(46.65, 6.85),
            LatLong(46.7, 6.90),
            LatLong(46.75, 6.95),
            LatLong(46.8, 7.00),
            LatLong(46.85, 7.05),
            LatLong(46.9, 7.10),
            LatLong(46.95, 7.15),
            LatLong(47.0, 7.20),
            LatLong(47.05, 7.25),
            LatLong(47.1, 7.30),
            LatLong(47.15, 7.35),
            LatLong(47.2, 7.40),
            LatLong(47.25, 7.45),
            LatLong(47.3, 7.50),
            LatLong(47.35, 7.55),
            LatLong(47.4, 7.60),
            LatLong(47.45, 7.65),
            LatLong(47.5, 7.70),
            LatLong(47.55, 7.75),
            LatLong(47.6, 7.80),
            LatLong(47.65, 7.85),
            LatLong(47.7, 7.90),
            LatLong(47.75, 7.95),
            LatLong(47.8, 8.00),
            LatLong(47.85, 8.05),
            LatLong(47.9, 8.10),
            LatLong(47.95, 8.15),
            LatLong(48.0, 8.20))

    assertEquals(207.59, RouteUtils.computeTotalDistance(points), 0.1)
  }

  @Test
  fun computeTotalDistance_returns_zero_for_empty_list() {
    val points = emptyList<LatLong>()
    val totalDistance = RouteUtils.computeTotalDistance(points)
    assertEquals(0.0, totalDistance, 0.1)
  }

  @Test
  fun getElevationGain_returns_correct_elevation_gain_from_service() =
      runTest(timeout = 10.seconds) {
        // mock elevationService
        val mockElevationService = mockk<ElevationServiceRepository>()
        val mockElevations = listOf(100.0, 150.0, 140.0, 200.0, 190.0)

        coEvery { mockElevationService.getElevation(any(), any(), any(), any()) } answers
            {
              val onSuccess = thirdArg<(List<Double>) -> Unit>()
              onSuccess(mockElevations)
            }

        // The coordinates we pass and the hikeId do not matter since we mock the elevation service
        val elevationGain = RouteUtils.getElevationGain(emptyList(), "0", mockElevationService)

        assertEquals(110.0, elevationGain, 0.01)
      }

  @Test
  fun test_determineDifficulty_easy() {
    assertEquals(HikeDifficulty.EASY, RouteUtils.determineDifficulty(1.0, 0.0))
  }

  @Test
  fun test_determineDifficulty_moderate() {
    assertEquals(HikeDifficulty.MODERATE, RouteUtils.determineDifficulty(5.0, 400.0))
  }

  @Test
  fun test_determineDifficulty_difficult() {
    assertEquals(HikeDifficulty.DIFFICULT, RouteUtils.determineDifficulty(11.12, 113.0))
  }
}
