package ch.hikemate.app.model.route

/** A class representing a bounding box */
data class Bounds(val minLat: Double, val minLon: Double, val maxLat: Double, val maxLon: Double) {

  /**
   * Check if this bounds intersects another bounds
   *
   * @param other The other bounds
   * @return True if this bounds intersects the other bounds, false otherwise
   */
  fun intersectsBounds(other: Bounds): Boolean {
    return !(this.minLat > other.maxLat ||
        this.maxLat < other.minLat ||
        this.minLon > other.maxLon ||
        this.maxLon < other.minLon)
  }
  /**
   * Check if this bounds contains another bounds
   *
   * @param other The other bounds
   * @return True if this bounds contains the other bounds, false otherwise
   */
  fun containsBounds(other: Bounds): Boolean {
    return minLat <= other.minLat &&
        minLon <= other.minLon &&
        maxLat >= other.maxLat &&
        maxLon >= other.maxLon
  }

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
