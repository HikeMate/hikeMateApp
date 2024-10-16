package ch.hikemate.app.model.route

import org.osmdroid.util.BoundingBox

/** A class representing a bounding box */
data class Bounds(val minLat: Double, val minLon: Double, val maxLat: Double, val maxLon: Double) {

  init {
    require(!(minLat > maxLat || minLon > maxLon)) { "Invalid bounds" }
  }

  class Builder {
    private var minLat = 0.0
    private var minLon = 0.0
    private var maxLat = 0.0
    private var maxLon = 0.0

    fun setMinLat(minLat: Double): Builder {
      this.minLat = minLat
      return this
    }

    fun setMinLon(minLon: Double): Builder {
      this.minLon = minLon
      return this
    }

    fun setMaxLat(maxLat: Double): Builder {
      this.maxLat = maxLat
      return this
    }

    fun setMaxLon(maxLon: Double): Builder {
      this.maxLon = maxLon
      return this
    }

    fun build(): Bounds {
      return Bounds(minLat, minLon, maxLat, maxLon)
    }
  }
}

fun BoundingBox.toBounds(): Bounds {
  return Bounds(latSouth, lonWest, latNorth, lonEast)
}

fun Bounds.toBoundingBox(): BoundingBox {
  return BoundingBox(maxLat, maxLon, minLat, minLon)
}

/** A simple data class to represent a latitude and longitude */
data class LatLong(val lat: Double, val lon: Double)

/**
 * Represents a hike route
 *
 * @param id The id of the route, depending of the client
 * @param bounds The bounding box of the route
 * @param ways The points of the route
 */
data class HikeRoute(val id: String, val bounds: Bounds, val ways: List<LatLong>)
