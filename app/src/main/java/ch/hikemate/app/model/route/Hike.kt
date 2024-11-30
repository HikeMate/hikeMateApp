package ch.hikemate.app.model.route

import ch.hikemate.app.model.route.saved.SavedHike
import ch.hikemate.app.ui.theme.hikeColors
import com.google.firebase.Timestamp
import kotlin.math.abs

/**
 * Represents a hike route with associated information.
 *
 * Some data about the hike might not be available immediately, because they are costly to compute
 * and should only be obtained if absolutely necessary. For example [waypoints] or [elevation]. See
 * [DeferredData] for more information about how this works.
 *
 * See [ch.hikemate.app.utils.RouteUtils] for more information about how the hike's details
 * ([distance], [estimatedTime], [elevationGain], [difficulty]) are computed.
 *
 * @param id The unique ID of the hike.
 * @param isSaved Whether the hike has been saved by the user.
 * @param plannedDate The date at which the user plans to go on the hike. Null if the user did not
 *   save the date or saved it without a specific date.
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
    val isSaved: Boolean,
    val plannedDate: Timestamp?,
    val name: String?,
    val description: DeferredData<String?> = DeferredData.NotRequested,
    val bounds: DeferredData<Bounds> = DeferredData.NotRequested,
    val waypoints: DeferredData<List<LatLong>> = DeferredData.NotRequested,
    val elevation: DeferredData<List<Double>> = DeferredData.NotRequested,
    val distance: DeferredData<Double> = DeferredData.NotRequested,
    val estimatedTime: DeferredData<Double> = DeferredData.NotRequested,
    val elevationGain: DeferredData<Double> = DeferredData.NotRequested,
    val difficulty: DeferredData<HikeDifficulty> = DeferredData.NotRequested
) {
  /** Helper to convert this [Hike] to a [SavedHike] object. */
  fun toSavedHike() = SavedHike(id, name ?: "", plannedDate)

  /** Get the color of the route from its id. The color should be the same for the same route id. */
  fun getColor(): Int {
    return hikeColors[abs(id.hashCode()) % hikeColors.size]
  }

  /**
   * Indicates whether the hike has all of its OSM data loaded.
   *
   * This includes [description], [bounds], and [waypoints].
   *
   * @return True if all OSM data were obtained ([DeferredData.Obtained]) for the hike, false
   *   otherwise.
   */
  fun hasOsmData(): Boolean {
    return description.obtained() && bounds.obtained() && waypoints.obtained()
  }
}
