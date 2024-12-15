package ch.hikemate.app.utils

import android.content.Context
import android.location.Location
import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.DetailedHike
import ch.hikemate.app.model.route.HikeDifficulty
import ch.hikemate.app.model.route.LatLong
import ch.hikemate.app.utils.LocationUtils.projectLocationOnHike
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
  fun projectLocationOnHike_withInvalidRoute_returnsNull() {
    val emptyRoute =
        DetailedHike(
            id = "test",
            color = 0,
            isSaved = false,
            plannedDate = null,
            name = null,
            description = null,
            bounds = Bounds(0.0, 0.0, 1.0, 1.0),
            waypoints = listOf(),
            elevation = listOf(),
            distance = 0.0,
            estimatedTime = 0.0,
            elevationGain = 0.0,
            difficulty = HikeDifficulty.EASY)
    val location = LatLong(45.0, 7.0)
    assertNull(projectLocationOnHike(location, emptyRoute))

    val singlePointRoute =
        DetailedHike(
            id = "test",
            color = 0,
            isSaved = false,
            plannedDate = null,
            name = null,
            description = null,
            bounds = Bounds(44.0, 6.0, 46.0, 8.0),
            waypoints = listOf(LatLong(45.0, 7.0)),
            elevation = listOf(0.0),
            distance = 0.0,
            estimatedTime = 0.0,
            elevationGain = 0.0,
            difficulty = HikeDifficulty.EASY)
    assertNull(projectLocationOnHike(location, singlePointRoute))
  }

  @Test
  fun projectLocationOnHike_withSingleSegment_projectsCorrectly() {
    val route =
        DetailedHike(
            id = "test",
            color = 0,
            isSaved = false,
            plannedDate = null,
            name = null,
            description = null,
            bounds = Bounds(44.0, 6.0, 46.0, 8.0),
            waypoints = listOf(LatLong(45.0, 7.0), LatLong(45.0, 8.0)),
            elevation = listOf(150.0, 150.0),
            distance = 0.0,
            estimatedTime = 0.0,
            elevationGain = 0.0,
            difficulty = HikeDifficulty.EASY)

    val location = LatLong(45.1, 7.5) // Should project exactly at mid-segment

    val result = projectLocationOnHike(location, route)
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
    assertEquals(result.projectedLocationElevation, 150.0, EPSILON)
  }

  @Test
  fun projectLocationOnHike_selectsClosestSegmentFromThree() {
    val route =
        DetailedHike(
            id = "test",
            color = 0,
            isSaved = false,
            plannedDate = null,
            name = null,
            description = null,
            bounds = Bounds(44.0, 6.0, 46.0, 8.0),
            waypoints =
                listOf(
                    LatLong(45.0, 7.0), // Start
                    LatLong(45.0, 7.5), // End of first segment (horizontal)
                    LatLong(45.5, 7.5), // End of second segment (vertical)
                    LatLong(45.5, 8.0) // End of third segment (horizontal)
                    ),
            elevation = listOf(10.0, 20.0, 30.0, 40.0),
            distance = 0.0,
            estimatedTime = 0.0,
            elevationGain = 0.0,
            difficulty = HikeDifficulty.EASY)

    run {
      val point = LatLong(45.1, 7.2)
      val result = projectLocationOnHike(point, route)
      assertNotNull(result)

      assertEquals(0, result!!.indexToSegment)
      assertEquals(route.segments[0], result.segment)
      assertEquals(45.0, result.projectedLocation.lat, EPSILON)
      assertEquals(7.2, result.projectedLocation.lon, EPSILON)

      // Progress should only be distance from start of first segment
      val expectedProgress = result.projectedLocation.distanceTo(route.segments[0].start)
      assertEquals(expectedProgress, result.progressDistance, EPSILON)
      assertEquals(result.projectedLocationElevation, 10.0, EPSILON)
    }

    run {
      val point = LatLong(45.3, 7.5)
      val result = projectLocationOnHike(point, route)
      assertNotNull(result)

      assertEquals(1, result!!.indexToSegment)
      assertEquals(route.segments[1], result.segment)
      assertEquals(45.3, result.projectedLocation.lat, EPSILON)
      assertEquals(7.5, result.projectedLocation.lon, EPSILON)

      val expectedProgress =
          route.segments[0].length + result.projectedLocation.distanceTo(result.segment.start)
      assertEquals(expectedProgress, result.progressDistance, EPSILON)
      assertEquals(result.projectedLocationElevation, 20.0, EPSILON)
    }

    run {
      val point = LatLong(45.5, 7.8)
      val result = projectLocationOnHike(point, route)
      assertNotNull(result)

      assertEquals(2, result!!.indexToSegment)
      assertEquals(route.segments[2], result.segment)
      assertEquals(45.5, result.projectedLocation.lat, EPSILON)
      assertEquals(7.8, result.projectedLocation.lon, EPSILON)

      val expectedProgress =
          route.segments[0].length +
              route.segments[1].length +
              result.projectedLocation.distanceTo(result.segment.start)
      assertEquals(expectedProgress, result.progressDistance, EPSILON)
      assertEquals(result.projectedLocationElevation, 30.0, EPSILON)
    }
  }

  @After
  fun tearDown() {
    // Reset all mocks
    clearAllMocks()
    unmockkAll()
  }
}
