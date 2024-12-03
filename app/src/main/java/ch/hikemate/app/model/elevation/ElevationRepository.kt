package ch.hikemate.app.model.elevation

import ch.hikemate.app.model.route.LatLong

/** A service for getting the elevation of a list of coordinates */
fun interface ElevationRepository {
  /**
   * Get the elevation of a list of coordinates
   *
   * @param coordinates The list of coordinates to get the elevation of
   * @param hikeID The ID of the hike
   * @param onSuccess The callback to be called when the request is successful
   * @param onFailure The callback to be called when the request fails
   */
  fun getElevation(
      coordinates: List<LatLong>,
      hikeID: String,
      onSuccess: (List<Double>) -> Unit,
      onFailure: (Exception) -> Unit
  )
}
