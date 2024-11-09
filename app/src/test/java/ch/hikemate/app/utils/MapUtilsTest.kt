import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.utils.calculateBestZoomLevel
import ch.hikemate.app.utils.getGeographicalCenter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.osmdroid.util.GeoPoint

class MapUtilsTest {

  /*
   * Tests for the calculateBestZoomLevel function
   */
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


  /*
   * Tests for getGeoPointFromBounds function
   */

  @Test
  fun getGeographicalCenterBasicCase() {
    val bounds = Bounds(minLat = 0.0, maxLat = 10.0, minLon = 0.0, maxLon = 10.0)
    val expectedCenter = GeoPoint(5.0, 5.0)
    val actualCenter = getGeographicalCenter(bounds)
    assertEquals(expectedCenter, actualCenter)
  }

  @Test
  fun getGeographicalCenterCrossesDateLinePositive() {
    val bounds = Bounds(minLat = -10.0, maxLat = 10.0, minLon = 170.0, maxLon = -170.0)
    val expectedCenter = GeoPoint(0.0, 180.0) // Corrected longitude crossing the Date Line
    val actualCenter = getGeographicalCenter(bounds)
    assertEquals(expectedCenter, actualCenter)
  }

  @Test
  fun getGeographicalCenterCrossesDateLineNegative() {
    val bounds = Bounds(minLat = -10.0, maxLat = 10.0, minLon = -170.0, maxLon = 170.0)
    val expectedCenter = GeoPoint(0.0, -180.0) // Corrected longitude crossing the Date Line
    val actualCenter = getGeographicalCenter(bounds)
    assertEquals(expectedCenter, actualCenter)
  }

  @Test
  fun getGeographicalCenterCrossesDateLineNearZero() {
    val bounds = Bounds(minLat = -10.0, maxLat = 10.0, minLon = 179.5, maxLon = -179.5)
    val expectedCenter = GeoPoint(0.0, 180.0) // Adjusted to account for Date Line crossing
    val actualCenter = getGeographicalCenter(bounds)
    assertEquals(expectedCenter, actualCenter)
  }

  @Test
  fun getGeographicalCenterLongitudeNormalizationOver180() {
    val bounds = Bounds(minLat = -10.0, maxLat = 10.0, minLon = 170.0, maxLon = 200.0)
    val expectedCenter = GeoPoint(0.0, -175.0) // Normalized to within -180 to 180
    val actualCenter = getGeographicalCenter(bounds)
    assertEquals(expectedCenter, actualCenter)
  }

  @Test
  fun getGeographicalCenterLongitudeNormalizationUnderNegative180() {
    val bounds = Bounds(minLat = -10.0, maxLat = 10.0, minLon = -190.0, maxLon = -170.0)
    val expectedCenter = GeoPoint(0.0, 175.0) // Normalized to within -180 to 180
    val actualCenter = getGeographicalCenter(bounds)
    assertEquals(expectedCenter, actualCenter)
  }

  @Test
  fun getGeographicalCenterPolarRegion() {
    val bounds = Bounds(minLat = 85.0, maxLat = 90.0, minLon = -45.0, maxLon = 45.0)
    val expectedCenter = GeoPoint(87.5, 0.0) // High-latitude case, near the poles
    val actualCenter = getGeographicalCenter(bounds)
    assertEquals(expectedCenter, actualCenter)
  }

  @Test
  fun getGeographicalCenterAroundZeroMeridian() {
    val bounds = Bounds(minLat = -10.0, maxLat = 10.0, minLon = -5.0, maxLon = 5.0)
    val expectedCenter = GeoPoint(0.0, 0.0) // Centered on the prime meridian
    val actualCenter = getGeographicalCenter(bounds)
    assertEquals(expectedCenter, actualCenter)
  }
}
