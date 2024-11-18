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

  // --- calculateElevationGain tests ---

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

  // --- computeTotalDistance tests ---

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
  fun test_computeTotalDistance_returns_zero_for_empty_list() {
    val points = emptyList<LatLong>()
    val totalDistance = RouteUtils.computeTotalDistance(points)
    assertEquals(0.0, totalDistance, 0.1)
  }

  // --- getElevationGain tests ---

  @Test
  fun test_getElevationGain_returns_correct_elevation_gain_from_service() =
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

  // --- determineDifficulty tests ---

  @Test
  fun test_determineDifficulty_easy() {
    assertEquals(HikeDifficulty.EASY, RouteUtils.determineDifficulty(0.0, 0.0))
  }

  @Test
  fun test_determineDifficulty_easy_upperBoundary() {
    assertEquals(HikeDifficulty.EASY, RouteUtils.determineDifficulty(2.99, 249.9))
  }

  @Test
  fun test_determineDifficulty_moderate_lowerBoundaryDistance() {
    // Exactly on the boundary for distance but within elevation gain for MODERATE
    assertEquals(HikeDifficulty.MODERATE, RouteUtils.determineDifficulty(3.0, 100.0))
  }

  @Test
  fun test_determineDifficulty_moderate_upperBoundaryDistance() {
    // Test for upper limit of distance in MODERATE range
    assertEquals(HikeDifficulty.MODERATE, RouteUtils.determineDifficulty(6.0, 250.0))
  }

  @Test
  fun test_determineDifficulty_moderate_upperBoundaryElevation() {
    // Exactly on the upper limit of elevation gain within MODERATE
    assertEquals(HikeDifficulty.MODERATE, RouteUtils.determineDifficulty(4.0, 500.0))
  }

  @Test
  fun test_determineDifficulty_moderate_upperBoundaryBoth() {
    // Test exactly on the upper boundary for both distance and elevation gain
    assertEquals(HikeDifficulty.MODERATE, RouteUtils.determineDifficulty(6.0, 500.0))
  }

  @Test
  fun test_determineDifficulty_difficult_beyondModerateDistance() {
    // Distance just beyond MODERATE range
    assertEquals(HikeDifficulty.DIFFICULT, RouteUtils.determineDifficulty(6.01, 400.0))
  }

  @Test
  fun test_determineDifficulty_difficult_beyondModerateElevation() {
    // Elevation just beyond MODERATE range
    assertEquals(HikeDifficulty.DIFFICULT, RouteUtils.determineDifficulty(4.0, 500.1))
  }

  @Test
  fun test_determineDifficulty_difficult_beyondModerateBoth() {
    // Both distance and elevation gain beyond MODERATE range
    assertEquals(HikeDifficulty.DIFFICULT, RouteUtils.determineDifficulty(7.0, 600.0))
  }
}
