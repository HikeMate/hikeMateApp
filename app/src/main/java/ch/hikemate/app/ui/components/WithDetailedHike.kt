package ch.hikemate.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.hikemate.app.R
import ch.hikemate.app.model.route.DetailedHike
import ch.hikemate.app.model.route.Hike
import ch.hikemate.app.model.route.HikesViewModel

private const val LOG_TAG = "WithDetailedHike"

/** Acts as a wrapper. */
@Composable
fun WithDetailedHike(
    hike: Hike,
    hikesViewModel: HikesViewModel,
    withDetailedHike: @Composable (DetailedHike) -> Unit,
    whenError: @Composable () -> Unit
) {
  when {
    // All the details of the hike have been computed, pass them on to the content callback
    hike.isFullyLoaded() -> {
      val detailedHike =
          try {
            hike.withDetailsOrThrow()
          } catch (e: Exception) {
            null
          }

      if (detailedHike != null) {
        withDetailedHike(detailedHike)
      } else {
        whenError()
      }
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

    // Waypoints are not available, retrieve them
    else -> {
      hikesViewModel.retrieveLoadedHikesOsmData()
      CenteredLoadingAnimation(stringResource(R.string.loading_hike_osm_data))
    }
  }
}
