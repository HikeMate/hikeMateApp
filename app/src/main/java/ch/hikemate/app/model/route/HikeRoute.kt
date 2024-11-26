package ch.hikemate.app.model.route

import ch.hikemate.app.ui.theme.hikeColors
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

  /**
   * Projects this point onto a line segment between two geographical coordinates.
   * Assumes routes don't wrap around the world map edges.
   * Accounts for longitude distance scaling based on latitude.
   *
   * @param start Starting point of the line segment
   * @param end Ending point of the line segment
   * @return Projected point as LatLong
   */
  fun projectPointIntoLine(start: LatLong, end: LatLong): LatLong {
    // Scale longitude differences based on latitude (at middle latitude of segment)
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
      lat = start.lat + tClamped * dy,
      lon = start.lon + tClamped * (end.lon - start.lon)
    )
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
  val length: Double
    get() = getLength()

  val segments: List<RouteSegment>
    get() = toSegments()

  /** Get the color of the route from its id. The color should be the same for the same route id. */
  fun getColor(): Int {
    return hikeColors[abs(id.hashCode()) % hikeColors.size]
  }

  private val segmentLengths: List<Double> by lazy {
    ways.windowed(2).map { (p1, p2) -> p1.distanceTo(p2) }
  }

  fun toSegments(): List<RouteSegment> {
    return this.ways.zip(this.segmentLengths).windowed(2).map { (first, second) ->
      RouteSegment(first.first, second.first, first.second)
    }
  }

  /** Get the length of the hike */
  private fun getLength(): Double {
    return segmentLengths.sum()
  }
}

/**
 * Represents two points in a hike route
 *
 * @param start The start point of the segment
 * @param end The end point of the segment
 * @param length
 */
data class RouteSegment(val start: LatLong, val end: LatLong, val length: Double)

/**
 * Data class used for projections from a location to the hike route this gives every necessary
 * information for the UI to handle the projection.
 *
 * @param projectedLocation the projected point in the route
 * @param progressDistance the distance up to the nearest start point of a RouteSegment.
 * @param distanceFromRoute the distance from the Location to the projectedLocation
 * @param segment the RouteSegment the Location is projected in
 * @param indexToSegment the index of the segment in the list of segments
 */
data class RouteProjectionResponse(
    val projectedLocation: LatLong,
    val progressDistance: Double,
    val distanceFromRoute: Double,
    val segment: RouteSegment,
    val indexToSegment: Int
)

typealias HikeWay = List<LatLong>
