package ch.hikemate.app.model.route

interface ElevationService {
  fun getElevation(
      coordinates: List<LatLong>,
      onSuccess: (List<Double>) -> Unit,
      onFailure: (Exception) -> Unit
  )
}
