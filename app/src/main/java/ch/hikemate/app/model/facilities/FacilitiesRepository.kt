package ch.hikemate.app.model.facilities

import ch.hikemate.app.model.route.Bounds

/** Interface for the facilities provider repository. */
fun interface FacilitiesRepository {

  /**
   * Asynchronously fetches facilities within the bounds. Makes a request to the Overpass API to
   * retrieve amenities like toilets, parking areas, and waste baskets. These can be specified in
   * the FacilityType enum.
   *
   * @param bounds Geographical bounds within which to search for facilities
   * @param onSuccess Callback to handle the resulting list of facilities when the operation
   *   succeeds
   * @param onFailure Callback invoked when the operation fails
   */
  fun getFacilities(
      bounds: Bounds,
      onSuccess: (List<Facility>) -> Unit,
      onFailure: (Exception) -> Unit
  )
}
