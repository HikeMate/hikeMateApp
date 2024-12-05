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

  /**
   * Get the color of the route from its id. The color should be the same for the same route id.
   *
   * @return The color as an integer. Use [androidx.compose.ui.graphics.Color] to convert it to an
   *   actual color.
   */
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

  /**
   * Indicates whether the hike has all of its data obtained.
   *
   * See [DeferredData] for more information about what it means for the data to be obtained.
   *
   * If this returns, true, you can safely call [withDetailsOrThrow] to get a [DetailedHike]
   * instance
   *
   * @return True if all data were obtained for the hike, false otherwise.
   */
  fun isFullyLoaded(): Boolean =
      description.obtained() &&
          bounds.obtained() &&
          waypoints.obtained() &&
          elevation.obtained() &&
          distance.obtained() &&
          estimatedTime.obtained() &&
          elevationGain.obtained() &&
          difficulty.obtained()

  /**
   * If all attributes of the hike are computed, casts everything to their respective data type and
   * returns a [DetailedHike] with the values to work with them directly without needing to perform
   * null checks.
   *
   * To make sure all details are loaded before calling this method, use [isFullyLoaded].
   *
   * If one or more attribute is missing, throws an [IllegalStateException].
   *
   * @throws IllegalStateException If one or more attribute of the hike has not been computed yet.
   */
  fun withDetailsOrThrow(): DetailedHike {
    check(isFullyLoaded())
    return DetailedHike(
        id,
        getColor(),
        isSaved,
        plannedDate,
        name,
        description.getOrThrow(),
        bounds.getOrThrow(),
        waypoints.getOrThrow(),
        elevation.getOrThrow(),
        distance.getOrThrow(),
        estimatedTime.getOrThrow(),
        elevationGain.getOrThrow(),
        difficulty.getOrThrow())
  }
}

/**
 * A [Hike] equivalent where all data are guaranteed to be available.
 *
 * See [Hike]'s documentation for more information about the fields.
 *
 * Used in [Hike.withDetailsOrThrow] to provide a [Hike] instance with all its details.
 */
data class DetailedHike(
    val id: String,
    val color: Int,
    val isSaved: Boolean,
    val plannedDate: Timestamp?,
    val name: String?,
    val description: String?,
    val bounds: Bounds,
    val waypoints: List<LatLong>,
    val elevation: List<Double>,
    val distance: Double,
    val estimatedTime: Double,
    val elevationGain: Double,
    val difficulty: HikeDifficulty
)
