package ch.hikemate.app.model.route

import ch.hikemate.app.ui.theme.hikeColors
import java.math.RoundingMode
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

  /**
   * Checks if a given coordinate point falls within these bounds. Uses inclusive range checks for
   * both latitude and longitude.
   *
   * @param lat The latitude to check, in decimal degrees
   * @param lon The longitude to check, in decimal degrees
   * @return true if the coordinate is within the bounds, false otherwise
   */
  fun containsCoordinate(lat: Double, lon: Double): Boolean {
    return (lat in minLat..maxLat) && (lon in minLon..maxLon)
  }

  /**
   * Formats the bounds as a comma-separated string for use in Overpass API queries. Returns
   * coordinates in the format required by Overpass API: "south,west,north,east"
   * (minLat,minLon,maxLat,maxLon)
   *
   * @return String representation of the bounds in Overpass API format
   */
  fun toStringForOverpassAPI(): String {
    return "$minLat,$minLon,$maxLat,$maxLon"
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

    fun isCrossingDateLine(): Boolean {
      return minLon > maxLon
    }

    fun build(): Bounds {
      return Bounds(minLat, minLon, maxLat, maxLon)
    }
  }
}

fun Bounds.toBoundingBox(): BoundingBox {
  return BoundingBox(maxLat, maxLon, minLat, minLon)
}

/** A simple data class to represent a latitude and longitude */
class LatLong(lat: Double, lon: Double) {

  companion object {
    /** The precision of the coordinates */
    const val PRECISION = 6
  }

  val lat: Double = lat.toBigDecimal().setScale(PRECISION, RoundingMode.HALF_UP).toDouble()
  val lon: Double = lon.toBigDecimal().setScale(PRECISION, RoundingMode.HALF_UP).toDouble()

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

  /**
   * Projects this point onto a line segment between two geographical coordinates. Assumes routes
   * don't wrap around the world map edges. Accounts for longitude distance scaling based on
   * latitude.
   *
   * @param start Starting point of the line segment
   * @param end Ending point of the line segment
   * @return Projected point as LatLong
   */
  fun projectPointOntoLine(start: LatLong, end: LatLong): LatLong {
    // Scale longitude differences based on latitude (at middle latitude of segment)
    // This is an acceptable approximation in our use case since longitude is not expected to change
    // much on a single segment,
    // plus we do not need perfect accuracy.
    val midLat = (start.lat + end.lat) / 2.0
    val lonScaleFactor = cos(Math.toRadians(midLat))

    // Calculate scaled vectors
    val dx = (end.lon - start.lon) * lonScaleFactor
    val dy = end.lat - start.lat

    val l2 = dx * dx + dy * dy

    if (l2 == 0.0) return start

    // Calculate projection with scaled coordinates
    val t = ((this.lon - start.lon) * lonScaleFactor * dx + (this.lat - start.lat) * dy) / l2
    val tClamped = t.coerceIn(0.0, 1.0)

    return LatLong(
        lat = start.lat + tClamped * dy, lon = start.lon + tClamped * (end.lon - start.lon))
  }

  override fun equals(other: Any?): Boolean {
    return if (other is LatLong) {
      lat == other.lat && lon == other.lon
    } else {
      false
    }
  }

  override fun hashCode(): Int {
    return lat.hashCode() * 31 + lon.hashCode()
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

  /** Get the color of the route from its id. The color should be the same for the same route id. */
  fun getColor(): Int {
    return hikeColors[abs(id.hashCode()) % hikeColors.size]
  }

  /** The list of segments of the Route */
  val segments: List<RouteSegment> by lazy {
    return@lazy this.ways.zipWithNext { p1, p2 -> RouteSegment(p1, p2, p1.distanceTo(p2)) }
  }
}

typealias HikeWay = List<LatLong>
