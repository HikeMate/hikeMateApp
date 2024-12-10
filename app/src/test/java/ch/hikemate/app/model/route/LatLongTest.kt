package ch.hikemate.app.model.route

import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LatLongTest {
  private val latLong = LatLong(46.94809, 7.44744)
  private val latLong2 = LatLong(46.94809, 7.44744)
  private val latLong3 = LatLong(46.94809, 7.44745)
  private val latLong4 = LatLong(0.0, 0.0)

  @Test
  fun latLong_distanceToCorrect() {
    val distance = latLong.distanceTo(latLong2)
    assertEquals(0.0, distance, 0.0)
  }

  @Test
  fun latLong_equalsCorrect() {
    assertEquals(latLong, latLong2)
    assertNotEquals(latLong, latLong3)
  }

  @Test
  fun latLong_hashCodeCorrect() {
    assertEquals(latLong.hashCode(), latLong2.hashCode())
    assertNotEquals(latLong.hashCode(), latLong3.hashCode())
    assertNotEquals(latLong.hashCode(), latLong4.hashCode())
    assertNotEquals(latLong2.hashCode(), latLong4.hashCode())
    assertNotEquals(latLong3.hashCode(), latLong4.hashCode())
  }
}
