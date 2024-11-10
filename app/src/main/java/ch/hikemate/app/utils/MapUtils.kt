package ch.hikemate.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import ch.hikemate.app.R
import ch.hikemate.app.ui.map.MapScreen
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

object MapUtils {
  private const val LOG_TAG = "MapUtils"

  /**
   * Clears the marker representing the user's position from the map.
   *
   * The map is NOT invalidated. If needed, use [org.osmdroid.views.MapView.invalidate].
   *
   * To update the user's position to a new location, use [updateUserPosition].
   *
   * @param previous The previous marker representing the user's position
   * @param mapView The map view where the marker is displayed
   */
  fun clearUserPosition(previous: Marker?, mapView: MapView, invalidate: Boolean = false) {
    previous?.let { mapView.overlays.remove(it) }
    if (invalidate) {
      mapView.invalidate()
    }
  }

  /**
   * Get the icon to use for the user's location marker.
   *
   * @param context The context where the icon will be used
   * @return The icon to use for the user's location marker
   */
  fun getUserLocationMarkerIcon(context: Context): Drawable {
    // Retrieve the actual drawable resource
    val originalDrawable = AppCompatResources.getDrawable(context, R.drawable.user_location)

    // Resize the vector resource to look good on the map
    val bitmap =
        Bitmap.createBitmap(
            MapScreen.USER_LOCATION_MARKER_ICON_SIZE,
            MapScreen.USER_LOCATION_MARKER_ICON_SIZE,
            Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    originalDrawable?.setBounds(0, 0, canvas.width, canvas.height)
    originalDrawable?.draw(canvas)

    return BitmapDrawable(context.resources, bitmap)
  }

  /**
   * Draws a new marker on the map representing the user's position. The previous marker is cleared
   * to avoid duplicates. The map is invalidated to redraw the map with the new marker.
   *
   * @param previous The previous marker representing the user's position
   * @param mapView The map view where the marker will be displayed
   * @param location The new location of the user
   * @return The new marker representing the user's position
   */
  fun updateUserPosition(previous: Marker?, mapView: MapView, location: Location): Marker {
    // Clear the previous marker to avoid duplicates
    clearUserPosition(previous, mapView)

    // Create a new marker with the new position
    val newMarker =
        Marker(mapView).apply {
          icon = getUserLocationMarkerIcon(mapView.context)
          position = GeoPoint(location.latitude, location.longitude)
          setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
          setOnMarkerClickListener { marker, mapView ->
            mapView.controller.animateTo(
                marker.position,
                mapView.zoomLevelDouble,
                MapScreen.CENTER_MAP_ON_MARKER_ANIMATION_TIME)
            true
          }
        }

    // Add the new marker to the map
    mapView.overlays.add(newMarker)
    mapView.invalidate()

    return newMarker
  }

  /**
   * Retrieves the user's current location and centers the map on it.
   *
   * If the location is not available, the user is notified that they must enable location services.
   *
   * @param context The context where the location is requested
   * @param mapView The map view to center
   * @param userLocationMarker The marker representing the user's location on the map
   * @see [LocationUtils.getUserLocation]
   */
  fun centerMapOnUserLocation(context: Context, mapView: MapView, userLocationMarker: Marker?) {
    LocationUtils.getUserLocation(
        context = context,
        onLocation = { location ->
          if (location == null) {
            Log.e(LOG_TAG, "User location is null, cannot center map")

            // Clear the user's position from the map
            clearUserPosition(userLocationMarker, mapView, invalidate = true)

            // Notify the user that they must enable location services
            Toast.makeText(
                    context,
                    context.getString(R.string.map_screen_location_not_available),
                    Toast.LENGTH_LONG)
                .show()
          } else {
            // Center the map on the user's location
            centerMapOnUserLocation(mapView, location)
          }
        })
  }

  /**
   * Centers the map on the user's location.
   *
   * Animates the map so that the transition is smooth and not instant.
   *
   * @param mapView The map view to center
   * @param location The user's location to center the map on
   */
  fun centerMapOnUserLocation(mapView: MapView, location: Location) {
    mapView.controller.animateTo(
        GeoPoint(location.latitude, location.longitude),
        mapView.zoomLevelDouble,
        MapScreen.CENTER_MAP_ANIMATION_TIME)
  }
}
