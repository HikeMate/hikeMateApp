package ch.hikemate.app.model.route

/**
 * Represents the state of some data attribute that is not immediately available.
 *
 * Here is an example of how to handle the possible states of a deferred data value in code:
 * ```kotlin
 * val deferredData: DeferredData = ...
 * when (deferredData) {
 *   is DeferredData.NotRequested -> println("Ask the view model to retrieve the data")
 *   is DeferredData.Requested -> println("Display some loading animation")
 *   is DeferredData.Obtained -> println("Display the ${deferredData.data}")
 * }
 * ```
 *
 * An example of such data can be the elevation data of a hike route, which is costly to get and
 * thus not retrieved immediately when the hike route is loaded. Instead, it is loaded when (and if)
 * needed, and stored in this DeferredData object.
 *
 * @param T The type of the data that is being deferred.
 */
sealed class DeferredData<out T> {
  /**
   * The deferred data is not available and has not yet been requested.
   *
   * In a typical UI scenario, if these data are required, you would ask the view model to retrieve
   * the data. This would switch the state to [Requested].
   */
  object NotRequested : DeferredData<Nothing>()

  /**
   * The deferred data has been requested but is not yet available.
   *
   * In a typical UI scenario, if these data are required, a loading animation or label should be
   * displayed to notify the user this data is being queried. Once the data arrives, the state will
   * be switched to [Obtained] by the view model making the request.
   */
  object Requested : DeferredData<Nothing>()

  /**
   * The deferred data has been requested and is available.
   *
   * Note that the contained data might be invalid or empty, but the request has been performed and
   * [data] represents the received response or result. Further processing of the result is not the
   * job of [DeferredData].
   */
  data class Obtained<out T>(val data: T) : DeferredData<T>()

  /** Returns `true` if the data has been obtained, `false` otherwise. */
  fun obtained(): Boolean = this is Obtained
}
