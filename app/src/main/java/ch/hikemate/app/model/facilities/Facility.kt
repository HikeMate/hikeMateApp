package ch.hikemate.app.model.facilities

import ch.hikemate.app.model.route.LatLong

/**
 * Represents a facility or amenity point. Each facility has a specific type (e.g., toilet, parking)
 * and geographical coordinates.
 *
 * @property type The type of facility, defined in [FacilityType]
 * @property coordinates Geographic coordinates of the facility
 */
data class Facility(val type: FacilityType, val coordinates: LatLong)

/**
 * Enum of all supported facility types that can be found along hiking routes. Each facility type
 * corresponds to the amenity tag used in OpenStreetMap data.
 *
 * @property type The string representation of the facility type as used in OpenStreetMap/Overpass
 *   API @link: https://wiki.openstreetmap.org/wiki/Key:amenity
 */
enum class FacilityType(val type: String) {
  TOILETS("toilets"),
  PARKING("parking"),
  WASTE_BASKET("waste_basket"),
  SUPERMARKET("supermarket"),
  DRINKING_WATER("drinking_water"),
  RANGER_STATION("ranger_station"),
  BBQ("bbq"),
  BENCH("bench"),
  RESTAURANT("restaurant"),
  BIERGARTEN("biergarten");

  companion object {

    /**
     * Creates a pipe-separated string of all facility types for use in Overpass API queries.
     * Example output: "toilets|parking|waste_basket|..."
     *
     * @return String containing all facility types separated by pipe characters
     */
    fun listOfAmenitiesForOverpassRequest(): String {
      var string = ""
      for (facility in FacilityType.values()) {
        string += facility.type + "|"
      }
      return string
    }

    /**
     * Converts a string representation of a facility type to its corresponding enum value. Can be
     * used for example when parsing facility types from API responses.
     *
     * @param string The string representation of the facility type
     * @return The matching [FacilityType] enum value, or null if no match is found
     */
    fun fromString(string: String): FacilityType? {
      for (facility in FacilityType.values()) {
        if (facility.type == string) {
          return facility
        }
      }
      return null
    }
  }
}
