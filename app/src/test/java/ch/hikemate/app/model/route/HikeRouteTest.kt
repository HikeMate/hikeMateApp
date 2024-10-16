package ch.hikemate.app.model.route

import org.junit.Assert.*
import org.junit.Test
import org.osmdroid.util.BoundingBox

class HikeRouteTest {
  object HikeRouteTest {
    const val DELTA = 0.0001
  }

  @Test
  fun conversionFromBoundingBoxToBounds() {
    val boundingBox = BoundingBox(40.0, 20.0, -40.0, -20.0)
    val bounds = boundingBox.toBounds()
    assertEquals(bounds.minLat, boundingBox.latSouth, HikeRouteTest.DELTA)
    assertEquals(bounds.maxLat, boundingBox.latNorth, HikeRouteTest.DELTA)
    assertEquals(bounds.minLon, boundingBox.lonWest, HikeRouteTest.DELTA)
    assertEquals(bounds.maxLon, boundingBox.lonEast, HikeRouteTest.DELTA)
  }

  @Test
  fun conversionFromBoundsToBoundingBox() {
    val bounds = Bounds(-40.0, -20.0, 40.0, 20.0)
    val boundingBox = bounds.toBoundingBox()
    assertEquals(boundingBox.latSouth, bounds.minLat, HikeRouteTest.DELTA)
    assertEquals(boundingBox.latNorth, bounds.maxLat, HikeRouteTest.DELTA)
    assertEquals(boundingBox.lonWest, bounds.minLon, HikeRouteTest.DELTA)
    assertEquals(boundingBox.lonEast, bounds.maxLon, HikeRouteTest.DELTA)
  }
}
