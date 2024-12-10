package ch.hikemate.app.ui.components

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import ch.hikemate.app.R
import ch.hikemate.app.model.route.DeferredData
import ch.hikemate.app.model.route.DetailedHike
import ch.hikemate.app.model.route.Hike
import ch.hikemate.app.model.route.HikesViewModel

object WithDetailedHike {
  const val LOG_TAG = "WithDetailedHike"
}

/**
 * Utility composable that acts as a wrapper.
 *
 * If a section of the interface needs a [DetailedHike] object, this composable will perform the
 * loading and error handling logic, then provide a callback for when the [DetailedHike] instance is
 * ready.
 *
 * It will also display a loading screen while the data is being fetched.
 *
 * @param hike The hike for which the details are needed
 * @param hikesViewModel The view model that will be used to fetch the data
 * @param withDetailedHike The callback that will be called when the hike details are ready
 * @param whenError The callback that will be called when an error occurs
 * @see DetailedHike
 */
@Composable
fun WithDetailedHike(
    hike: Hike,
    hikesViewModel: HikesViewModel,
    withDetailedHike: @Composable (DetailedHike) -> Unit,
    whenError: @Composable () -> Unit
) {
  val loadingErrorMessageId by hikesViewModel.loadingErrorMessageId.collectAsState()

  when {
    // All the details of the hike have been computed, pass them on to the content callback
    hike.isFullyLoaded() -> {
      val detailedHike =
          try {
            hike.withDetailsOrThrow()
          } catch (e: Exception) {
            Log.d(WithDetailedHike.LOG_TAG, "Error while getting detailed hike: $e")
            null
          }

      if (detailedHike != null) {
        withDetailedHike(detailedHike)
      } else {
        whenError()
      }
    }

    // Elevation data has been retrieved, but an error occurred
    hike.elevation is DeferredData.Error -> {
      CenteredErrorAction(
          errorMessageId = R.string.loading_hike_elevation_retrieval_error,
          actionIcon = Icons.Default.Refresh,
          actionContentDescriptionStringId = R.string.retry,
          onAction = { hikesViewModel.retrieveElevationDataFor(hike.id) })
    }

    // Details have not been computed yet, but the elevation has been retrieved
    hike.elevation.obtained() -> {
      hikesViewModel.computeDetailsFor(hike.id)
      CenteredLoadingAnimation(stringResource(R.string.loading_hike_details))
    }

    // Waypoints are available, but elevation hasn't been retrieved yet. Retrieve it
    hikesViewModel.canElevationDataBeRetrievedFor(hike) -> {
      hikesViewModel.retrieveElevationDataFor(hike.id)
      CenteredLoadingAnimation(stringResource(R.string.loading_hike_elevation))
    }

    // An error occurred while loading the hike's OSM data or while refreshing the saved hikes list
    loadingErrorMessageId != null -> {
      whenError()
    }

    // Waypoints are not available, retrieve them
    else -> {
      hikesViewModel.retrieveLoadedHikesOsmData()
      CenteredLoadingAnimation(stringResource(R.string.loading_hike_osm_data))
    }
  }
}
