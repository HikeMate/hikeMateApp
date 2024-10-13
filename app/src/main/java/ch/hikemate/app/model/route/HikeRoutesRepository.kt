package ch.hikemate.app.model.route

/** Interface for the hiking route provider repository. */
fun interface HikeRoutesRepository {
  /**
   * Returns the routes inside the given bounding box and zoom level.
   *
   * @param bounds The bounds in which to search for routes.
   * @param onSuccess The callback to be called when the routes are successfully fetched.
   * @param onFailure The callback to be called when the routes could not be fetched.
   */
  fun getRoutes(
      bounds: Bounds,
      onSuccess: (List<HikeRoute>) -> Unit,
      onFailure: (Exception) -> Unit
  )
}
