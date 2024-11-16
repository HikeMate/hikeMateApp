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
}
