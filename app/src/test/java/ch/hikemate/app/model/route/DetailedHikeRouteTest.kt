import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.DetailedHikeRoute
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.LatLong
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.timeout

class DetailedHikeRouteTest {

  @Test
  fun testCreateDetailHikeRoute() {
    val hikeRoute =
        HikeRoute(
            id = "1",
            name = "Test Hike",
            description = "This is a test hike",
            bounds = Bounds(1.0, 2.0, 3.0, 4.0),
            ways = listOf(LatLong(1.0, 2.0), LatLong(2.0, 3.0), LatLong(3.0, 4.0)))

    val detailedHikeRoute = DetailedHikeRoute.create(hikeRoute)

    // assert the rest of the data is still accessible as expected
    assertEquals(hikeRoute, detailedHikeRoute.route)

    // assert detail calculations are correct
    assertEquals(hikeRoute, detailedHikeRoute.route)

    assertEquals(314.4, detailedHikeRoute.totalDistance, 1.0)
    assertEquals(3772.0, detailedHikeRoute.estimatedTime, 1.0)
    assertEquals(0.0, detailedHikeRoute.elevationGain, 0.0001)
    assertEquals("Difficult", detailedHikeRoute.difficulty)
  }

  @Test
  fun testCreateDetailHikeRouteEasyHike() {
    val hikeRoute =
        HikeRoute(
            id = "",
            name = "",
            description = "",
            bounds = Bounds(0.0, 0.0, 0.0, 0.0),
            ways =
                listOf(
                    LatLong(5.0, 5.0),
                    LatLong(5.01, 5.01),
                ))

    val detailedHikeRoute = DetailedHikeRoute.create(hikeRoute)

    assertEquals(hikeRoute, detailedHikeRoute.route)
    assertEquals(1.322, detailedHikeRoute.totalDistance, 1.0)
    assertEquals(18.0, detailedHikeRoute.estimatedTime, 1.0)
    assertEquals(0.0, detailedHikeRoute.elevationGain, 0.0001)
    assertEquals("Easy", detailedHikeRoute.difficulty)
  }

  @Test
  fun testCreateDetailHikeRouteModerateHike() {
    val hikeRoute =
        HikeRoute(
            id = "",
            name = "",
            description = "",
            bounds = Bounds(0.0, 0.0, 0.0, 0.0),
            ways =
                listOf(
                    LatLong(5.0, 5.0),
                    LatLong(5.0500, 5.0),
                ))

    val detailedHikeRoute = DetailedHikeRoute.create(hikeRoute)

    assertEquals(hikeRoute, detailedHikeRoute.route)
    assertEquals(5.56, detailedHikeRoute.totalDistance, 1.0)
    assertEquals(66.0, detailedHikeRoute.estimatedTime, 1.0)
    assertEquals(0.0, detailedHikeRoute.elevationGain, 0.0001)
    assertEquals("Moderate", detailedHikeRoute.difficulty)
  }

  @Test
  fun testCreateDetailHikeRouteDifficultHike() {
    val hikeRoute =
        HikeRoute(
            id = "",
            name = "",
            description = "",
            bounds = Bounds(0.0, 0.0, 0.0, 0.0),
            ways =
                listOf(
                    LatLong(5.0, 5.0),
                    LatLong(5.1, 5.0),
                ))

    val detailedHikeRoute = DetailedHikeRoute.create(hikeRoute)

    assertEquals(hikeRoute, detailedHikeRoute.route)
    assertEquals(11.12, detailedHikeRoute.totalDistance, 1.0)
    assertEquals(133.0, detailedHikeRoute.estimatedTime, 1.0)
    assertEquals(0.0, detailedHikeRoute.elevationGain, 0.0001)
    assertEquals("Difficult", detailedHikeRoute.difficulty)
  }

  @Test
  fun testCreateDetailHikeRouteHikeWithElevationGain() =
      runTest(timeout = 10.seconds) {
        val hikeRoute =
            HikeRoute(
                id = "",
                name = "",
                description = "",
                bounds = Bounds(0.0, 0.0, 0.0, 0.0),
                ways =
                    listOf(
                        LatLong(46.5, 6.60),
                        LatLong(46.55, 6.75),
                    ))

        var detailedHikeRoute = DetailedHikeRoute.create(hikeRoute)

        // Sometimes the elevationGain is 0, if the Elevation Repository's fetch fails. This retries
        // until the fetch is successful.
        while (detailedHikeRoute.elevationGain == 0.0) {
          detailedHikeRoute = DetailedHikeRoute.create(hikeRoute)
        }

        assertEquals(hikeRoute, detailedHikeRoute.route)
        assertEquals(12.75, detailedHikeRoute.totalDistance, 1.0)
        assertEquals(196.0, detailedHikeRoute.estimatedTime, 1.0)
        assertEquals(433.0, detailedHikeRoute.elevationGain, 0.0001)
        assertEquals("Difficult", detailedHikeRoute.difficulty)
      }

  @Test
  fun testCreateDetailHikeRouteHikeWithManyWaypoints() {
    val hikeRoute =
        HikeRoute(
            id = "",
            name = "",
            description = "",
            bounds = Bounds(0.0, 0.0, 0.0, 0.0),
            ways =
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
                    LatLong(48.0, 8.20)))

    val detailedHikeRoute = DetailedHikeRoute.create(hikeRoute)

    // Sometimes the elevationGain is 0, if the Elevation Repository's fetch fails.
    if (detailedHikeRoute.elevationGain != 0.0) {
      assertEquals(hikeRoute, detailedHikeRoute.route)
      assertEquals(207.0, detailedHikeRoute.totalDistance, 1.0)
      assertEquals(2714.0, detailedHikeRoute.estimatedTime, 1.0)
      assertEquals(2231.0, detailedHikeRoute.elevationGain, 0.0001)
      assertEquals("Difficult", detailedHikeRoute.difficulty)
    } else {
      assertEquals(hikeRoute, detailedHikeRoute.route)
      assertEquals(207.0, detailedHikeRoute.totalDistance, 1.0)
      assertEquals(2491.0, detailedHikeRoute.estimatedTime, 1.0)
      assertEquals(0.0, detailedHikeRoute.elevationGain, 0.0001)
      assertEquals("Difficult", detailedHikeRoute.difficulty)
    }
  }
}
