package ch.hikemate.app.model.route

import org.junit.Assert.*
import org.junit.Test
import org.osmdroid.util.BoundingBox

class HikeRouteTest {
  object HikeRouteTest {
    const val DELTA_BOUNDS = 0.0001
    // Test data is precise up to 0.01
    const val DELTA_LATLONG = 0.01
  }

  @Test
  fun conversionFromBoundingBoxToBounds() {
    val boundingBox = BoundingBox(40.0, 20.0, -40.0, -20.0)
    val bounds = boundingBox.toBounds()
    assertEquals(bounds.minLat, boundingBox.latSouth, HikeRouteTest.DELTA_BOUNDS)
    assertEquals(bounds.maxLat, boundingBox.latNorth, HikeRouteTest.DELTA_BOUNDS)
    assertEquals(bounds.minLon, boundingBox.lonWest, HikeRouteTest.DELTA_BOUNDS)
    assertEquals(bounds.maxLon, boundingBox.lonEast, HikeRouteTest.DELTA_BOUNDS)
  }

  @Test
  fun conversionFromBoundsToBoundingBox() {
    val bounds = Bounds(-40.0, -20.0, 40.0, 20.0)
    val boundingBox = bounds.toBoundingBox()
    assertEquals(boundingBox.latSouth, bounds.minLat, HikeRouteTest.DELTA_BOUNDS)
    assertEquals(boundingBox.latNorth, bounds.maxLat, HikeRouteTest.DELTA_BOUNDS)
    assertEquals(boundingBox.lonWest, bounds.minLon, HikeRouteTest.DELTA_BOUNDS)
    assertEquals(boundingBox.lonEast, bounds.maxLon, HikeRouteTest.DELTA_BOUNDS)
  }

  @Test
  fun latLong_distanceToCorrect() {
    val pos1 = LatLong(0.0, 0.0)
    val pos2 = LatLong(0.0, 1.0)
    val pos3 = LatLong(1.0, 0.0)
    val pos4 = LatLong(1.0, 1.0)
    val distance12 = pos1.distanceTo(pos2)
    val distance13 = pos1.distanceTo(pos3)
    val distance14 = pos1.distanceTo(pos4)
    val distance23 = pos2.distanceTo(pos3)
    val distance24 = pos2.distanceTo(pos4)
    val distance34 = pos3.distanceTo(pos4)
    assertEquals(111194.93, distance12, HikeRouteTest.DELTA_LATLONG)
    assertEquals(111194.93, distance13, HikeRouteTest.DELTA_LATLONG)
    assertEquals(157249.38, distance14, HikeRouteTest.DELTA_LATLONG)
    assertEquals(157249.38, distance23, HikeRouteTest.DELTA_LATLONG)
    assertEquals(111194.93, distance24, HikeRouteTest.DELTA_LATLONG)
    assertEquals(111177.99, distance34, HikeRouteTest.DELTA_LATLONG)
  }

  @Test
  fun getColorsIsDeterministic() {
    val bounds = Bounds(0.0, 0.0, 0.0, 0.0)
    val hikes =
        listOf(
            HikeRoute("1", bounds, emptyList(), "name", "desc"),
            HikeRoute("2", bounds, emptyList(), "name", "desc"),
            HikeRoute("3", bounds, emptyList(), "name", "desc"),
            HikeRoute("4", bounds, emptyList(), "name", "desc"),
            HikeRoute("5", bounds, emptyList(), "name", "desc"),
        )

    for (hike in hikes) {
      for (i in 0..100) {
        val color = hike.getColor()
        assertEquals(color, hike.getColor())
      }
    }
  }

  @Test
  fun test_projection_of_point_onto_horizontal_line() {
    val start = LatLong(0.0, 0.0)
    val end = LatLong(0.0, 10.0)
    val point = LatLong(1.0, 5.0)

    val result = point.projectPointIntoLine(start, end)

    // The projected point should be at lat=0.0 (on the line) and lon=5.0 (halfway)
    assertLatLongEquals(LatLong(0.0, 5.0), result)
  }

  @Test
  fun test_projection_of_point_onto_vertical_line() {
    val start = LatLong(0.0, 0.0)
    val end = LatLong(10.0, 0.0)
    val point = LatLong(5.0, 1.0)

    val result = point.projectPointIntoLine(start, end)

    // The projected point should be at lon=0.0 (on the line) and lat=5.0 (halfway)
    assertLatLongEquals(LatLong(5.0, 0.0), result)
  }

  @Test
  fun test_projection_beyond_start_of_line_segment_is_clamped_to_start() {
    val start = LatLong(0.0, 0.0)
    val end = LatLong(10.0, 10.0)
    val point = LatLong(-5.0, -5.0)

    val result = point.projectPointIntoLine(start, end)

    // The point should be clamped to the start of the line
    assertLatLongEquals(start, result)
  }

  @Test
  fun test_projection_beyond_end_of_line_segment_is_clamped_to_end() {
    val start = LatLong(0.0, 0.0)
    val end = LatLong(10.0, 10.0)
    val point = LatLong(15.0, 15.0)

    val result = point.projectPointIntoLine(start, end)

    // The point should be clamped to the end of the line
    assertLatLongEquals(end, result)
  }

  @Test
  fun test_projection_onto_zero_length_line_returns_start_point() {
    val start = LatLong(5.0, 5.0)
    val end = LatLong(5.0, 5.0)
    val point = LatLong(10.0, 10.0)

    val result = point.projectPointIntoLine(start, end)

    // Should return the start point when line has zero length
    assertLatLongEquals(start, result)
  }

  @Test
  fun test_projection_accounts_for_latitude_scaling() {
    // Test at higher latitude where longitude distances are compressed
    val start = LatLong(60.0, 0.0)
    val end = LatLong(60.0, 10.0)
    val point = LatLong(61.0, 5.0)

    val result = point.projectPointIntoLine(start, end)

    // The projected point should be at lat=60.0 (on the line) and lon=5.0 (halfway)
    // but the exact position should account for the latitude scaling
    assertLatLongEquals(LatLong(60.0, 5.0), result)
  }

  @Test
  fun test_projection_with_exact_point_on_line() {
    val start = LatLong(0.0, 0.0)
    val end = LatLong(10.0, 10.0)
    val point = LatLong(5.0, 5.0)

    val result = point.projectPointIntoLine(start, end)

    // Point should project to itself since it's already on the line
    assertLatLongEquals(point, result)
  }

  @Test
  fun test_projection_near_equator() {
    val start = LatLong(0.0, 0.0)
    val end = LatLong(0.0, 10.0)
    val point = LatLong(1.0, 5.0)

    val result = point.projectPointIntoLine(start, end)

    // Near equator, no significant latitude scaling should occur
    assertLatLongEquals(LatLong(0.0, 5.0), result)
  }

  @Test
  fun test_projection_near_poles() {
    val start = LatLong(85.0, 0.0)
    val end = LatLong(85.0, 10.0)
    val point = LatLong(86.0, 5.0)

    val result = point.projectPointIntoLine(start, end)

    // Near poles, longitude differences should be heavily compressed
    assertEquals(85.0, result.lat, 0.0001)
    assertEquals(5.0, result.lon, 0.0001)
  }

  private fun assertLatLongEquals(expected: LatLong, actual: LatLong, delta: Double = 0.0001) {
    assertEquals("Latitude should match", expected.lat, actual.lat, delta)
    assertEquals("Longitude should match", expected.lon, actual.lon, delta)
  }
}
