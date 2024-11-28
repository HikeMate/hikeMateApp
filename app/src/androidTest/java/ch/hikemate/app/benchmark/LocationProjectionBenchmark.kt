package ch.hikemate.app.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.LatLong
import ch.hikemate.app.utils.LocationUtils.projectLocationOnStart
import kotlin.random.Random
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationProjectionBenchmark {
  @get:Rule val benchmarkRule = BenchmarkRule()

  private lateinit var longRoute: HikeRoute
  private lateinit var startPoint: LatLong
  private lateinit var midPoint: LatLong
  private lateinit var endPoint: LatLong
  private lateinit var farPoint: LatLong

  @Before
  fun setup() {
    longRoute =
        HikeRoute(
            id = "benchmark",
            bounds = Bounds(45.0, 6.0, 47.0, 8.0),
            ways = generateRealisticWaypoints(100000))

    startPoint = LatLong(longRoute.ways.first().lat + 0.001, longRoute.ways.first().lon + 0.001)

    val midIndex = longRoute.ways.size / 2
    midPoint = LatLong(longRoute.ways[midIndex].lat + 0.001, longRoute.ways[midIndex].lon + 0.001)

    endPoint = LatLong(longRoute.ways.last().lat + 0.001, longRoute.ways.last().lon + 0.001)
    farPoint = LatLong(longRoute.ways[midIndex].lat + 1.0, longRoute.ways[midIndex].lon + 1.0)
  }

  @Test
  fun benchmark_projectStart() =
      benchmarkRule.measureRepeated { projectLocationOnStart(startPoint, longRoute) }

  @Test
  fun benchmark_projectMiddle() =
      benchmarkRule.measureRepeated { projectLocationOnStart(midPoint, longRoute) }

  @Test
  fun benchmark_projectEnd() =
      benchmarkRule.measureRepeated { projectLocationOnStart(endPoint, longRoute) }

  @Test
  fun benchmark_projectFar() =
      benchmarkRule.measureRepeated { projectLocationOnStart(farPoint, longRoute) }

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
