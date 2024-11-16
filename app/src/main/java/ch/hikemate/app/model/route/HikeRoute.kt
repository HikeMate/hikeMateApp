package ch.hikemate.app.model.route

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import org.osmdroid.util.BoundingBox

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
    require(!(minLat > maxLat || minLon > maxLon)) {
      "Invalid bounds: minLat=$minLat, maxLat=$maxLat, minLon=$minLon, maxLong=$maxLon"
    }
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
data class LatLong(val lat: Double, val lon: Double) {
  /**
   * Calculate the distance between this point and another point, using the Haversine formula to
   * account for earth's curvature.
   *
   * @param other The other point
   * @return The distance in meters
   */
  fun distanceTo(other: LatLong): Double {
    val earthRadius = 6371000.0 // meters
    val dLat = Math.toRadians(other.lat - lat)
    val dLon = Math.toRadians(other.lon - lon)
    val a =
        sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat)) *
                cos(Math.toRadians(other.lat)) *
                sin(dLon / 2) *
                sin(dLon / 2)
    return 2 * earthRadius * asin(sqrt(a))
  }

  override fun equals(other: Any?): Boolean {
    return if (other is LatLong) {
      lat == other.lat && lon == other.lon
    } else {
      false
    }
  }

  override fun hashCode(): Int {
    return super.hashCode()
  }
}

/**
 * Represents a hike route
 *
 * @param id The id of the route, depending of the client
 * @param bounds The bounding box of the route
 * @param ways The points of the route
 * @param name The name of the route
 * @param description The description of the route
 */
data class HikeRoute(
    val id: String,
    val bounds: Bounds,
    val ways: List<LatLong>,
    val name: String? = null,
    val description: String? = null
) {

  private val colors =
      listOf(
          0xFF7E57C2.toInt(), // Purple
          0xFF1E88E5.toInt(), // Blue
          0xFFFFA726.toInt(), // Orange
          0xFF000000.toInt(), // Black
          0xFFEF5350.toInt(), // Red
          0xFF26A69A.toInt() // Teal
          )

  /** Get the color of the route from its id. The color should be the same for the same route id. */
  fun getColor(): Int {
    return colors[abs(id.hashCode()) % colors.size]
  }
}

typealias HikeWay = List<LatLong>
