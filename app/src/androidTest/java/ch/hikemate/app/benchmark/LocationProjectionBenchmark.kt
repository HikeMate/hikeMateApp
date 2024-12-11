package ch.hikemate.app.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.DetailedHike
import ch.hikemate.app.model.route.HikeDifficulty
import ch.hikemate.app.model.route.LatLong
import ch.hikemate.app.utils.LocationUtils.projectLocationOnHike
import com.google.firebase.Timestamp
import kotlin.random.Random
import org.junit.Rule

// @RunWith(AndroidJUnit4::class)
class LocationProjectionBenchmark {
  @get:Rule val benchmarkRule = BenchmarkRule()

  private lateinit var longRoute: DetailedHike
  private lateinit var startPoint: LatLong
  private lateinit var midPoint: LatLong
  private lateinit var endPoint: LatLong
  private lateinit var farPoint: LatLong

  // @Before
  fun setup() {
    longRoute =
        DetailedHike(
            id = "benchmark",
            description = "Benchmark Hike",
            bounds = Bounds(45.0, 6.0, 47.0, 8.0),
            waypoints = generateRealisticWaypoints(100000),
            elevation = List(100000) { Random.nextDouble(0.0, 1000.0) },
            distance = 1000.0,
            estimatedTime = 10.0,
            elevationGain = 5000.0,
            difficulty = HikeDifficulty.DIFFICULT,
            color = 1,
            isSaved = true,
            plannedDate = Timestamp.now(),
            name = "unnamed")

    startPoint =
        LatLong(longRoute.waypoints.first().lat + 0.001, longRoute.waypoints.first().lon + 0.001)

    val midIndex = longRoute.waypoints.size / 2
    midPoint =
        LatLong(
            longRoute.waypoints[midIndex].lat + 0.001, longRoute.waypoints[midIndex].lon + 0.001)

    endPoint =
        LatLong(longRoute.waypoints.last().lat + 0.001, longRoute.waypoints.last().lon + 0.001)
    farPoint =
        LatLong(longRoute.waypoints[midIndex].lat + 1.0, longRoute.waypoints[midIndex].lon + 1.0)
  }

  // @Test
  fun benchmark_projectStart() =
      benchmarkRule.measureRepeated { projectLocationOnHike(startPoint, longRoute) }

  // @Test
  fun benchmark_projectMiddle() =
      benchmarkRule.measureRepeated { projectLocationOnHike(midPoint, longRoute) }

  // @Test
  fun benchmark_projectEnd() =
      benchmarkRule.measureRepeated { projectLocationOnHike(endPoint, longRoute) }

  // @Test
  fun benchmark_projectFar() =
      benchmarkRule.measureRepeated { projectLocationOnHike(farPoint, longRoute) }

  private fun generateRealisticWaypoints(count: Int): List<LatLong> {
    val waypoints = mutableListOf<LatLong>()
    var lat = 45.0
    var lon = 6.0

    repeat(count) {
      waypoints.add(LatLong(lat, lon))
      lat += Random.nextDouble(-0.001, 0.001)
      lon += Random.nextDouble(0.0005, 0.002)
    }
    return waypoints
  }
}
