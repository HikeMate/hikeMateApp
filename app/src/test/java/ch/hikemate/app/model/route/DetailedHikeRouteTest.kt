import ch.hikemate.app.model.elevation.ElevationServiceRepository
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.DetailedHikeRoute
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.LatLong
import ch.hikemate.app.utils.RouteUtils
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
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

    coEvery {
      mockElevationService.getElevation(
          any(),
          any(),
          any(),
      )
    } answers
        {
          val onSuccess = secondArg<(List<Double>) -> Unit>()
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
    val expectedTotalDistance = RouteUtils.computeTotalDistance(hikeRoute.ways)
    val expectedElevationGain = RouteUtils.calculateElevationGain(mockElevations)
    val expectedEstimatedTime =
        RouteUtils.estimateTime(expectedTotalDistance, expectedElevationGain)
    val expectedDifficulty =
        RouteUtils.determineDifficulty(expectedTotalDistance, expectedElevationGain)

    assertEquals(hikeRoute, detailedHikeRoute.route)

    assertEquals(expectedTotalDistance, detailedHikeRoute.totalDistance, 1.0)
    assertEquals(expectedElevationGain, detailedHikeRoute.elevationGain, 0.0001)
    assertEquals(expectedEstimatedTime, detailedHikeRoute.estimatedTime, 1.0)
    assertEquals(expectedDifficulty, detailedHikeRoute.difficulty)
  }
}
