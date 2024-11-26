package ch.hikemate.app.model.facilities

import org.junit.Test

class FacilityTest {

  @Test
  fun testListOfAmenitiesForOverpassRequest() {
    val list = FacilityType.listOfAmenitiesForOverpassRequest()
    for (facility in FacilityType.values()) {
      assert(list.contains(facility.type))
    }
  }

  @Test
  fun testFromString() {
    for (facility in FacilityType.values()) {
      assert(FacilityType.fromString(facility.type) == facility)
    }
  }
}
