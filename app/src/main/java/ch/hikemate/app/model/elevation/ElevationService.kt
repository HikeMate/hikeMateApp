package ch.hikemate.app.model.elevation

import ch.hikemate.app.model.route.LatLong

/** A service for getting the elevation of a list of coordinates */
interface ElevationService {
  /** Get the elevation of a list of coordinates */
  fun getElevation(
      coordinates: List<LatLong>,
      onSuccess: (List<Double>) -> Unit,
      onFailure: (Exception) -> Unit
  )
}
