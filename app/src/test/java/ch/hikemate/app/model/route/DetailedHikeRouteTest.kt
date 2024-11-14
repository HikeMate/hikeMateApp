import ch.hikemate.app.model.elevation.ElevationServiceRepository
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.DetailedHikeRoute
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.LatLong
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DetailedHikeRouteTest {

  private lateinit var mockElevationService: ElevationServiceRepository
  private lateinit var mockDeferred: CompletableDeferred<Double>
  private var mockElevations = listOf(0.0)

  @Before
  fun setUp() {
    mockElevationService = mockk()
    mockDeferred = mockk()

    coEvery { mockElevationService.getElevation(any(), any(), any(), any()) } answers
        {
          val onSuccess = thirdArg<(List<Double>) -> Unit>()
          onSuccess(mockElevations)
        }
  }

  @Test
  fun testCreateDetailHikeRoute() {
    val hikeRoute =
        HikeRoute(
            id = "1",
            name = "Test Hike",
            description = "This is a test hike",
            bounds = Bounds(1.0, 2.0, 3.0, 4.0),
            ways = listOf(LatLong(1.0, 2.0), LatLong(2.0, 3.0), LatLong(3.0, 4.0)))

    val detailedHikeRoute = DetailedHikeRoute.create(hikeRoute, mockElevationService)

    // assert the rest of the data is still accessible as expected
    assertEquals(hikeRoute, detailedHikeRoute.route)

    // assert detail calculations are correct
    assertEquals(hikeRoute, detailedHikeRoute.route)

    assertEquals(314.4, detailedHikeRoute.totalDistance, 1.0)
    assertEquals(0.0, detailedHikeRoute.elevationGain, 0.0001)
    assertEquals(3772.0, detailedHikeRoute.estimatedTime, 1.0)
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

    val detailedHikeRoute = DetailedHikeRoute.create(hikeRoute, mockElevationService)

    assertEquals(hikeRoute, detailedHikeRoute.route)
    assertEquals(1.322, detailedHikeRoute.totalDistance, 1.0)
    assertEquals(0.0, detailedHikeRoute.elevationGain, 0.0001)
    assertEquals(18.0, detailedHikeRoute.estimatedTime, 1.0)
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

    val detailedHikeRoute = DetailedHikeRoute.create(hikeRoute, mockElevationService)

    assertEquals(hikeRoute, detailedHikeRoute.route)
    assertEquals(5.56, detailedHikeRoute.totalDistance, 1.0)
    assertEquals(0.0, detailedHikeRoute.elevationGain, 0.0001)
    assertEquals(66.0, detailedHikeRoute.estimatedTime, 1.0)
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

    val detailedHikeRoute = DetailedHikeRoute.create(hikeRoute, mockElevationService)

    assertEquals(hikeRoute, detailedHikeRoute.route)
    assertEquals(11.12, detailedHikeRoute.totalDistance, 1.0)
    assertEquals(0.0, detailedHikeRoute.elevationGain, 0.0001)
    assertEquals(133.0, detailedHikeRoute.estimatedTime, 1.0)
    assertEquals("Difficult", detailedHikeRoute.difficulty)
  }

  @Test
  fun testCreateDetailHikeRouteHikeWithElevationGain() =
      runTest(timeout = 10.seconds) {

        // the elevations returned by the mock service are not non-zero
        mockElevations = listOf(0.0, 200.0)

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

        val detailedHikeRoute = DetailedHikeRoute.create(hikeRoute, mockElevationService)

        assertEquals(hikeRoute, detailedHikeRoute.route)
        assertEquals(12.75, detailedHikeRoute.totalDistance, 1.0)
        assertEquals(200.0, detailedHikeRoute.elevationGain, 0.0001)
        assertEquals(173.0, detailedHikeRoute.estimatedTime, 1.0)
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
                // list of 30 coordinates
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

    // Elevation gain of 400m. It should not matter that there are only 5 elevations even though
    // there are 30 waypoints
    mockElevations = listOf(200.0, 300.0, 400.0, 500.0, 600.0)

    val detailedHikeRoute = DetailedHikeRoute.create(hikeRoute, mockElevationService)

    assertEquals(hikeRoute, detailedHikeRoute.route)
    assertEquals(207.59, detailedHikeRoute.totalDistance, 0.1)
    assertEquals(400.0, detailedHikeRoute.elevationGain, 0.0001)
    assertEquals(2531.0, detailedHikeRoute.estimatedTime, 1.0)
    assertEquals("Difficult", detailedHikeRoute.difficulty)
  }
}
