package ch.hikemate.app.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.location.Location
import android.widget.Toast
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.utils.MapUtils.calculateBestZoomLevel
import ch.hikemate.app.utils.MapUtils.getGeographicalCenter
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapUtilsTest {

  /*
   * Tests for the calculateBestZoomLevel function
   */
  @Test
  fun testCalculateBestZoomLevelWithSmallBounds() {
    val bounds = Bounds(minLat = 10.0, maxLat = 20.0, minLon = 10.0, maxLon = 20.0)
    val result = calculateBestZoomLevel(bounds)

    val expectedZoomLevel = 6
    assertEquals(expectedZoomLevel, result)
  }

  @Test
  fun testCalculateBestZoomLevelWithLargeBounds() {
    val bounds = Bounds(minLat = -80.0, maxLat = 80.0, minLon = -180.0, maxLon = 180.0)
    val result = calculateBestZoomLevel(bounds)

    // For very large bounds (covering the whole world), the zoom level should be very low (zoomed
    // out)
    val expectedZoomLevel = 0
    assertEquals(expectedZoomLevel, result)
  }

  @Test
  fun testCalculateBestZoomLevelWithAsymmetricBounds() {
    val bounds = Bounds(minLat = -30.0, maxLat = 30.0, minLon = -60.0, maxLon = 60.0)
    val result = calculateBestZoomLevel(bounds)

    val expectedZoomLevel = 2
    assertEquals(expectedZoomLevel, result)
  }

  @Test
  fun testCalculateBestZoomLevelWithSmallLongitudeDifference() {
    val bounds = Bounds(minLat = 10.0, maxLat = 20.0, minLon = 10.0, maxLon = 15.0)
    val result = calculateBestZoomLevel(bounds)

    val expectedZoomLevel = 6
    assertEquals(expectedZoomLevel, result)
  }

  /*
   * Tests for getGeoPointFromBounds function
   */

  @Test
  fun getGeographicalCenterBasicCase() {
    val bounds = Bounds(minLat = 0.0, maxLat = 0.0, minLon = -170.0, maxLon = 10.0)
    val expectedCenter = GeoPoint(0.0, -80.0)
    val actualCenter = getGeographicalCenter(bounds)
    assertEquals(expectedCenter, actualCenter)
  }

  // @Test
  fun getGeographicalCenterCrossesDateLinePositive() {
    val bounds = Bounds(minLat = -10.0, maxLat = 10.0, minLon = -170.0, maxLon = 170.0)
    val expectedCenter = GeoPoint(0.0, 180.0)
    val actualCenter = getGeographicalCenter(bounds)
    assertEquals(expectedCenter, actualCenter)
  }

  // @Test
  fun getGeographicalCenterCrossesDateLineNearZero() {
    val bounds = Bounds(minLat = -10.0, maxLat = 10.0, minLon = -179.5, maxLon = 179.5)
    val expectedCenter = GeoPoint(0.0, 180.0)
    val actualCenter = getGeographicalCenter(bounds)
    assertEquals(expectedCenter, actualCenter)
  }

  @Test
  fun getGeographicalCenterPolarRegion() {
    val bounds = Bounds(minLat = 85.0, maxLat = 90.0, minLon = -45.0, maxLon = 45.0)
    val expectedCenter = GeoPoint(87.5, 0.0) // High-latitude case, near the poles
    val actualCenter = getGeographicalCenter(bounds)
    assertEquals(expectedCenter, actualCenter)
  }

  @Test
  fun getGeographicalCenterAroundZeroMeridian() {
    val bounds = Bounds(minLat = -10.0, maxLat = 10.0, minLon = -5.0, maxLon = 5.0)
    val expectedCenter = GeoPoint(0.0, 0.0)
    val actualCenter = getGeographicalCenter(bounds)
    assertEquals(expectedCenter, actualCenter)
  }

  @Test
  fun updateUserPositionClearsOldPositionAndCreatesNewOne() {
    // Given
    // Spy on the MapUtils object so that only specific methods are mocked
    mockkObject(MapUtils)
    val dummyDrawable = mockk<Drawable>(relaxed = true)

    // Mock only `getUserLocationMarkerIcon`, while other methods remain as-is
    every { MapUtils.getUserLocationMarkerIcon(any()) } returns dummyDrawable

    // Mock or set up other test dependencies
    val map = mockk<MapView>(relaxed = true)
    val previous = mockk<Marker>(relaxed = true)
    map.overlays.add(previous)
    val location = mockk<Location>()
    every { location.latitude } returns 46.0
    every { location.longitude } returns 7.0
    val newOverlayCapture = slot<Marker>()

    // Capture the new overlay added to the map
    every { map.overlays.add(capture(newOverlayCapture)) } returns true

    // When the user position is udpated
    MapUtils.updateUserPosition(previous, map, location)

    // Then the previous marker is removed, map is invalidated, and a new marker is added
    verify { map.overlays.remove(previous) }
    verify { map.invalidate() }
    verify { map.overlays.add(any()) }
    val capturedMarker = newOverlayCapture.captured
    assertEquals(location.latitude, capturedMarker.position.latitude, 0.0)
    assertEquals(location.longitude, capturedMarker.position.longitude, 0.0)
  }

  @Test
  fun centerMapOnUserLocationCentersMapIfLocationIsReceived() {
    // Given
    mockkObject(MapUtils)
    every { MapUtils.centerMapOnUserLocation(any(), any()) } returns Unit
    mockkObject(LocationUtils)
    val callbackSlot = slot<(Location?) -> Unit>()
    every { LocationUtils.getUserLocation(any(), capture(callbackSlot), any(), any()) } returns Unit
    val context = mockk<Context>(relaxed = true)
    val map = mockk<MapView>(relaxed = true)
    val fakeLocation = mockk<Location>()
    every { fakeLocation.latitude } returns 46.0
    every { fakeLocation.longitude } returns 7.0

    // When
    MapUtils.centerMapOnUserLocation(context, map, null)

    // Then
    verify { LocationUtils.getUserLocation(context, any(), any(), any()) }

    // When
    callbackSlot.captured(fakeLocation)

    // Then
    verify { MapUtils.centerMapOnUserLocation(map, fakeLocation) }
  }

  @Test
  fun centerMapOnUserLocationClearsUserPositionIfLocationIsNull() {
    // Given
    mockkObject(MapUtils)
    every { MapUtils.centerMapOnUserLocation(any(), any()) } returns Unit
    mockkObject(LocationUtils)
    val callbackSlot = slot<(Location?) -> Unit>()
    every { LocationUtils.getUserLocation(any(), capture(callbackSlot), any(), any()) } returns Unit
    val context = mockk<Context>(relaxed = true)
    every { context.getString(any()) } returns "Test"
    val map = mockk<MapView>(relaxed = true)
    val fakeToast = mockk<Toast>(relaxed = true)
    mockkStatic(Toast::class)
    every { Toast.makeText(any<Context>(), any<CharSequence>(), any<Int>()) } returns fakeToast
    every { fakeToast.show() } returns Unit

    // When
    MapUtils.centerMapOnUserLocation(context, map, null)

    // Then
    verify { LocationUtils.getUserLocation(context, any(), any(), any()) }

    // When
    callbackSlot.captured(null)

    // Then
    verify { MapUtils.clearUserPosition(null, map, true) }
    verify { fakeToast.show() }
  }

  @Test
  fun centerMapOnUserLocationWithLocationAnimatesMap() {
    // Given
    val map = mockk<MapView>(relaxed = true)
    val location = mockk<Location>()
    every { location.latitude } returns 46.0
    every { location.longitude } returns 7.0

    // When
    MapUtils.centerMapOnUserLocation(map, location)
    Thread.sleep(500)

    // Then
    verify { map.controller.animateTo(any(), any(), any()) }
  }

  @After
  fun tearDown() {
    // Clears all mocks and resets the global state to avoid interference between tests
    clearAllMocks()
    unmockkAll()
  }
}
