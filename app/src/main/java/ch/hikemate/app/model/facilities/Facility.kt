package ch.hikemate.app.model.facilities

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
    fun listOfAmenitiesForOverpassRequest(): String {
      var string = ""
      for (facility in FacilityType.values()) {
        string += facility.type + "|"
      }
      return string
    }

    fun fromString(string: String): FacilityType? {
      return when (string) {
        "toilets" -> TOILETS
        "parking" -> PARKING
        "waste_basket" -> WASTE_BASKET
        "supermarket" -> SUPERMARKET
        "drinking_water" -> DRINKING_WATER
        "ranger_station" -> RANGER_STATION
        "bbq" -> BBQ
        "bench" -> BENCH
        "restaurant" -> RESTAURANT
        "biergarten" -> BIERGARTEN
        else -> null
      }
    }
  }
}
