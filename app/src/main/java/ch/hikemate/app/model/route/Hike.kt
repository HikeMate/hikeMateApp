package ch.hikemate.app.model.route

/**
 * Represents a hike route with associated information.
 *
 * Some data about the hike might not be available immediately, because they are costly to compute
 * and should only be obtained if absolutely necessary. For example [waypoints] or [elevation]. See
 * [DeferredData] for more information about how this works.
 *
 * @param id The unique ID of the hike.
 * @param name Human-readable name of the hike.
 * @param description Human-readable description of the hike.
 * @param bounds Minimum and maximum latitude/longitude of the hike on a map.
 * @param waypoints List of geographical points that form the line of the hike.
 * @param elevation List of [Double] values that give the elevation (in meters) along the hike.
 * @param distance Total length of the hike (in kilometers).
 * @param estimatedTime How much time the hike is estimated to take in order to complete it. The
 *   value is a [Double] that represents the number of hours it would take.
 * @param elevationGain The cumulative elevation gain of the hike (in meters).
 * @param difficulty Estimation of how difficult the hike is on a three-levels scale.
 * @see [ch.hikemate.app.utils.RouteUtils]
 * @see [HikeDifficulty]
 * @see [DeferredData]
 */
data class Hike(
    val id: String,
    val name: String,
    val description: String,
    val name: String?,
    val description: String?,
    val bounds: DeferredData<Bounds>,
    val waypoints: DeferredData<List<LatLong>>,
    val elevation: DeferredData<List<Double>>,
    val distance: DeferredData<Double>,
    val estimatedTime: DeferredData<Double>,
    val elevationGain: DeferredData<Double>,
    val difficulty: DeferredData<HikeDifficulty>
)
