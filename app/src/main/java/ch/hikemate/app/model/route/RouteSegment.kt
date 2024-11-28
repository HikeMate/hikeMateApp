package ch.hikemate.app.model.route

/**
 * Represents the line defined by two points in a hike route
 *
 * @param start The start point of the segment
 * @param end The end point of the segment
 * @param length
 */
data class RouteSegment(val start: LatLong, val end: LatLong, val length: Double)
