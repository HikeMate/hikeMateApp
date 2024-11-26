package ch.hikemate.app.utils

import android.content.Context
import android.location.Location
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.HikeRoute
import ch.hikemate.app.model.route.LatLong
import ch.hikemate.app.model.route.RouteProjectionResponse
import ch.hikemate.app.utils.LocationUtils.projectLocationOnStart
import ch.hikemate.app.utils.LocationUtils.projectLocationOngoing
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import io.mockk.CapturingSlot
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class LocationUtilsTest {
  private val EPSILON = 0.0001 // For double comparisons
  private lateinit var successCallbackSlot: CapturingSlot<OnSuccessListener<Location>>
  private lateinit var failureCallbackSlot: CapturingSlot<OnFailureListener>
  private lateinit var task: Task<Location>
  private lateinit var fusedClient: FusedLocationProviderClient
  private lateinit var context: Context
  private lateinit var location: Location

  @Before
  fun setUp() {
    successCallbackSlot = slot()
    failureCallbackSlot = slot()
    task = mockk()
    fusedClient = mockk()
    mockkStatic(LocationServices::class)
    context = mockk()
    every { LocationServices.getFusedLocationProviderClient(context) } returns fusedClient
    location = mockk()
  }

  @Test
  fun getUserLocationWorksOnSuccess() {
    // Given
    every { fusedClient.getCurrentLocation(any<Int>(), any()) } returns task
    every { task.addOnSuccessListener(capture(successCallbackSlot)) } returns task
    every { task.addOnFailureListener(any()) } returns task
    var successCallbackWasCalled = false

    // When
    LocationUtils.getUserLocation(context, { successCallbackWasCalled = true })
    successCallbackSlot.captured.onSuccess(location)

    // Then
    assert(successCallbackWasCalled)
  }

  @Test
  fun getUserLocationWorksOnFailure() {
    // Given
    every { fusedClient.getCurrentLocation(any<Int>(), any()) } returns task
    every { task.addOnSuccessListener(any()) } returns task
    every { task.addOnFailureListener(capture(failureCallbackSlot)) } returns task
    var failureCallbackWasCalled = false

    // When
    LocationUtils.getUserLocation(context, { failureCallbackWasCalled = it == null })
    failureCallbackSlot.captured.onFailure(Exception())

    // Then
    assert(failureCallbackWasCalled)
  }

  @Test
  fun getUserLocationWorksOnSecurityException() {
    // Given
    every { fusedClient.getCurrentLocation(any<Int>(), any()) } throws SecurityException()
    var failureCallbackWasCalled = false

    // When
    LocationUtils.getUserLocation(context, { failureCallbackWasCalled = it == null })

    // Then
    assert(failureCallbackWasCalled)
  }

  @Test
  fun startUserLocationUpdatesReturnsFalseOnSecurityException() {
    // Given
    every {
      fusedClient.requestLocationUpdates(any<LocationRequest>(), any<LocationCallback>(), any())
    } throws SecurityException()

    // When
    val result = LocationUtils.startUserLocationUpdates(context, mockk())

    // Then
    verify {
      fusedClient.requestLocationUpdates(any<LocationRequest>(), any<LocationCallback>(), any())
    }
    assert(!result)
  }

  @Test
  fun onUserLocationUpdateClearsPositionWhenLocationIsNull() {
    // Given
    val marker = mockk<Marker>()
    val mapView = mockk<MapView>()
    val locationResult = mockk<LocationResult>()
    every { locationResult.lastLocation } returns null
    mockkObject(MapUtils)
    every { MapUtils.clearUserPosition(any(), any(), any()) } returns Unit

    // When
    val result = LocationUtils.onUserLocationUpdate(locationResult, mapView, marker)

    // Then
    verify { MapUtils.clearUserPosition(marker, mapView, true) }
    assert(result == null)
  }

  @Test
  fun onUserLocationUpdateUpdatesPositionWhenLocationIsNotNull() {
    // Given
    val marker = mockk<Marker>()
    val mapView = mockk<MapView>()
    val locationResult = mockk<LocationResult>()
    val location = mockk<Location>()
    every { locationResult.lastLocation } returns location
    mockkObject(MapUtils)
    every { MapUtils.updateUserPosition(any(), any(), any()) } returns marker

    // When
    val result = LocationUtils.onUserLocationUpdate(locationResult, mapView, marker)

    // Then
    verify { MapUtils.updateUserPosition(marker, mapView, location) }
    assert(result == marker)
  }

  @Test
  fun projectLocationOnStart_withInvalidRoute_returnsNull() {
    val emptyRoute = HikeRoute(id = "test", bounds = Bounds(0.0, 0.0, 1.0, 1.0), ways = listOf())
    val location = LatLong(45.0, 7.0)
    assertNull(projectLocationOnStart(location, emptyRoute))

    val singlePointRoute =
        HikeRoute(
            id = "test", bounds = Bounds(44.0, 6.0, 46.0, 8.0), ways = listOf(LatLong(45.0, 7.0)))
    assertNull(projectLocationOnStart(location, singlePointRoute))
  }

  @Test
  fun projectLocationOnStart_withSingleSegment_projectsCorrectly() {
    val route =
        HikeRoute(
            id = "test",
            bounds = Bounds(44.0, 6.0, 46.0, 8.0),
            ways = listOf(LatLong(45.0, 7.0), LatLong(45.0, 8.0)))

    val location = LatLong(45.1, 7.5) // Should project exactly at mid-segment

    val result = projectLocationOnStart(location, route)
    assertNotNull(result)

    // Check projected location
    assertEquals(45.0, result!!.projectedLocation.lat, EPSILON)
    assertEquals(7.5, result.projectedLocation.lon, EPSILON)

    // Check segment selection
    assertEquals(0, result.indexToSegment)
    assertEquals(route.segments[0], result.segment)

    // Check distances
    val distanceToProjection = location.distanceTo(result.projectedLocation)
    val distanceFromSegmentStart = result.projectedLocation.distanceTo(result.segment.start)
    assertEquals(distanceToProjection, result.distanceFromRoute, EPSILON)

    // Progress should be distance from start to projection
    assertEquals(distanceFromSegmentStart, result.progressDistance, EPSILON)
  }

  @Test
  fun projectLocationOnStart_withMultipleSegments_findsClosestSegment() {
    val route =
        HikeRoute(
            id = "test",
            bounds = Bounds(44.0, 6.0, 46.0, 8.0),
            ways = listOf(LatLong(45.0, 7.0), LatLong(45.0, 7.5), LatLong(45.0, 8.0)))

    // Test point closer to second segment
    val location = LatLong(45.2, 7.7)

    val result = projectLocationOnStart(location, route)
    assertNotNull(result)

    // Check projected location
    assertEquals(45.0, result!!.projectedLocation.lat, EPSILON)
    assertEquals(7.7, result.projectedLocation.lon, EPSILON)

    // Should select middle segment
    assertEquals(1, result.indexToSegment)
    assertEquals(route.segments[1], result.segment)

    // Check distances
    val distanceToProjection = location.distanceTo(result.projectedLocation)
    assertEquals(distanceToProjection, result.distanceFromRoute, EPSILON)

    // Progress should include first segment length plus distance on current segment

    val distanceFromSegmentStart = result.projectedLocation.distanceTo(result.segment.start)
    val expectedProgress = route.segments[0].length + distanceFromSegmentStart
    assertEquals(expectedProgress, result.progressDistance, EPSILON)
  }

  @Test
  fun projectLocationOngoing_withinThreshold_maintainsSegment() {
    val route =
        HikeRoute(
            id = "test",
            bounds = Bounds(44.0, 6.0, 46.0, 8.0),
            ways = listOf(LatLong(45.0, 7.0), LatLong(45.0, 7.5), LatLong(45.0, 8.0)))

    val currentSegment = route.segments[1]
    val lastProjection = LatLong(45.0, 7.6)
    val location = LatLong(45.1, 7.65)
    val initialProgress = route.segments[0].length + lastProjection.distanceTo(currentSegment.start)
    val threshold = 1000.0
    val lastProjectionResponse =
        RouteProjectionResponse(
            projectedLocation = lastProjection,
            progressDistance = initialProgress,
            distanceFromRoute = lastProjection.distanceTo(currentSegment.start),
            segment = currentSegment,
            indexToSegment = 1)

    val result =
        projectLocationOngoing(
            location = location, route = route, lastProjectionResponse, threshold = threshold)

    // Check projected location
    assertEquals(45.0, result.projectedLocation.lat, EPSILON)
    assertEquals(7.65, result.projectedLocation.lon, EPSILON)

    // Should maintain same segment
    assertEquals(1, result.indexToSegment)
    assertEquals(currentSegment, result.segment)

    // Check distances
    val distanceToProjection = location.distanceTo(result.projectedLocation)
    val distanceFromSegmentStart = result.projectedLocation.distanceTo(result.segment.start)
    assertEquals(distanceToProjection, result.distanceFromRoute, EPSILON)

    // Should maintain progress
    assertEquals(
        route.segments[0].length + distanceFromSegmentStart, result.progressDistance, EPSILON)
  }

  @Test
  fun projectLocationOngoing_movingForward_projectsToNextSegment() {
    val route =
        HikeRoute(
            id = "test",
            bounds = Bounds(44.0, 6.0, 46.0, 8.0),
            ways = listOf(LatLong(45.0, 7.0), LatLong(45.0, 7.5), LatLong(45.0, 8.0)))

    val currentSegment = route.segments[0]
    val lastProjection = LatLong(45.0, 7.3)
    val location = LatLong(45.1, 7.6)
    val threshold = 100.0

    val lastProjectionResponse =
        RouteProjectionResponse(
            projectedLocation = lastProjection,
            progressDistance = lastProjection.distanceTo(currentSegment.start),
            distanceFromRoute = 0.0,
            segment = currentSegment,
            indexToSegment = 0)

    val result =
        projectLocationOngoing(
            location = location, route = route, lastProjectionResponse, threshold = threshold)

    // Check projected location
    assertEquals(45.0, result.projectedLocation.lat, EPSILON)
    assertEquals(7.6, result.projectedLocation.lon, EPSILON)

    // Should move to next segment
    assertEquals(1, result.indexToSegment)
    assertEquals(route.segments[1], result.segment)

    // Check distances
    val distanceToProjection = location.distanceTo(result.projectedLocation)
    val distanceFromSegmentStart = result.projectedLocation.distanceTo(result.segment.start)
    assertEquals(distanceToProjection, result.distanceFromRoute, EPSILON)

    // Progress should include first segment
    val expectedProgress = route.segments[0].length + distanceFromSegmentStart
    assertEquals(expectedProgress, result.progressDistance, EPSILON)
  }

  @Test
  fun projectLocationOngoing_movingBackward_projectsToPreviousSegment() {
    val route =
        HikeRoute(
            id = "test",
            bounds = Bounds(44.0, 6.0, 46.0, 8.0),
            ways = listOf(LatLong(45.0, 7.0), LatLong(45.0, 7.5), LatLong(45.0, 8.0)))

    val currentSegment = route.segments[1]
    val lastProjection = LatLong(45.0, 7.7)
    val location = LatLong(45.1, 7.3)
    val initialProgress = route.segments[0].length + lastProjection.distanceTo(currentSegment.start)
    val threshold = 100.0

    val lastProjectionResponse =
        RouteProjectionResponse(
            projectedLocation = lastProjection,
            progressDistance = initialProgress,
            distanceFromRoute = 0.0,
            segment = currentSegment,
            indexToSegment = 1)
    val result =
        projectLocationOngoing(
            location = location, route = route, lastProjectionResponse, threshold = threshold)

    // Check projected location
    assertEquals(45.0, result.projectedLocation.lat, EPSILON)
    assertEquals(7.3, result.projectedLocation.lon, EPSILON)

    // Should move to previous segment
    assertEquals(0, result.indexToSegment)
    assertEquals(route.segments[0], result.segment)

    // Check distances
    val distanceToProjection = location.distanceTo(result.projectedLocation)
    assertEquals(distanceToProjection, result.distanceFromRoute, EPSILON)
  }

  @Test
  fun projectLocationOngoing_atRouteEnds_maintainsEndpoints() {
    val route =
        HikeRoute(
            id = "test",
            bounds = Bounds(44.0, 6.0, 46.0, 8.0),
            ways = listOf(LatLong(45.0, 7.0), LatLong(45.0, 7.5)))

    // Test at start
    val startLocation = LatLong(45.0, 6.8)

    val lastProjectionResponse =
        RouteProjectionResponse(
            projectedLocation = LatLong(45.0, 7.0),
            progressDistance = 0.0,
            distanceFromRoute = 0.0,
            segment = route.segments[0],
            indexToSegment = 0)
    val startResult =
        projectLocationOngoing(
            location = startLocation, route = route, lastProjectionResponse, threshold = 100.0)

    assertEquals(0, startResult.indexToSegment)
    assertEquals(route.segments[0], startResult.segment)
    val startDistanceToProjection = startLocation.distanceTo(startResult.projectedLocation)
    val startDistanceFromSegmentStart =
        startResult.projectedLocation.distanceTo(startResult.segment.start)
    assertEquals(
        startDistanceToProjection + startDistanceFromSegmentStart,
        startResult.distanceFromRoute,
        EPSILON)

    // Test at end
    val endLocation = LatLong(45.0, 7.7)

    val lastProjectionResponse2 =
        RouteProjectionResponse(
            projectedLocation = LatLong(45.0, 7.5),
            progressDistance = route.segments[0].length,
            distanceFromRoute = 0.0,
            segment = route.segments.last(),
            indexToSegment = 0)
    val endResult =
        projectLocationOngoing(
            location = endLocation, route = route, lastProjectionResponse2, threshold = 100.0)

    assertEquals(route.segments.lastIndex, endResult.indexToSegment)
    assertEquals(route.segments.last(), endResult.segment)
    val endDistanceToProjection = endLocation.distanceTo(endResult.projectedLocation)
    val endDistanceFromSegmentStart =
        endResult.projectedLocation.distanceTo(endResult.segment.start)
    assertEquals(endDistanceToProjection, endResult.distanceFromRoute, EPSILON)
  }

  @After
  fun tearDown() {
    // Reset all mocks
    clearAllMocks()
    unmockkAll()
  }
}
