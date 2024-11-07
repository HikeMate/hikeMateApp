import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.utils.calculateBestZoomLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class MapUtilsTest {

  @Test
  fun testCalculateBestZoomLevelWithSmallBounds() {
    val bounds = Bounds(minLat = 10.0, maxLat = 20.0, minLon = 10.0, maxLon = 20.0)
    val result = calculateBestZoomLevel(bounds)

    val expectedZoomLevel = 6
    assertEquals(expectedZoomLevel, result)
  }

  @Test
  fun testCalculateBestZoomLevelWithLargeBounds() {
    val bounds = Bounds(minLat = -80.0, maxLat = 80.0, minLon = -180.0, maxLon = 180.0)
    val result = calculateBestZoomLevel(bounds)

    // For very large bounds (covering the whole world), the zoom level should be very low (zoomed
    // out)
    val expectedZoomLevel = 0
    assertEquals(expectedZoomLevel, result)
  }

  @Test
  fun testCalculateBestZoomLevelWithAsymmetricBounds() {
    val bounds = Bounds(minLat = -30.0, maxLat = 30.0, minLon = -60.0, maxLon = 60.0)
    val result = calculateBestZoomLevel(bounds)

    val expectedZoomLevel = 2
    assertEquals(expectedZoomLevel, result)
  }

  @Test
  fun testCalculateBestZoomLevelWithSmallLongitudeDifference() {
    val bounds = Bounds(minLat = 10.0, maxLat = 20.0, minLon = 10.0, maxLon = 15.0)
    val result = calculateBestZoomLevel(bounds)

    val expectedZoomLevel = 6
    assertEquals(expectedZoomLevel, result)
  }
}
