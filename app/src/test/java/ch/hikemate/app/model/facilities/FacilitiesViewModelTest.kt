package ch.hikemate.app.model.facilities

import ch.hikemate.app.model.route.Bounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.osmdroid.util.GeoPoint

class FacilitiesViewModelTest {

  private lateinit var mockFacilitiesRepository: FacilitiesRepository
  private lateinit var testDispatcher: TestDispatcher

  private lateinit var facilitiesViewModel: FacilitiesViewModel // SUT

  private val testBounds = Bounds(46.5, 6.6, 46.51, 6.62)
  private val testFacilities = listOf(Facility(FacilityType.TOILETS, GeoPoint(46.51, 6.61)))

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    testDispatcher = StandardTestDispatcher()
    Dispatchers.setMain(testDispatcher)

    mockFacilitiesRepository = mock()
    facilitiesViewModel = FacilitiesViewModel(mockFacilitiesRepository, testDispatcher)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun testGetFacilities_onSuccess() {
    val facilities = listOf(Facility(FacilityType.TOILETS, GeoPoint(46.51, 6.61)))

    `when`(mockFacilitiesRepository.getFacilities(any(), any(), any())).then {
      val onSuccess = it.getArgument<(List<Facility>) -> Unit>(1)
      onSuccess(facilities)
    }

    facilitiesViewModel.getFacilities(
        bounds = testBounds,
        onSuccess = { result -> assertEquals(facilities, result) },
        onFailure = { fail("Should not be called") })
  }

  @Test
  fun testGetFacilities_onFailure() {
    val exception = Exception("Test exception")

    `when`(mockFacilitiesRepository.getFacilities(any(), any(), any())).then {
      val onFailure = it.getArgument<(Exception) -> Unit>(2)
      onFailure(exception)
    }

    facilitiesViewModel.getFacilities(
        bounds = testBounds,
        onSuccess = { fail("Should not be called") },
        onFailure = { ex -> assertEquals(exception, ex) })
  }

  @Test
  fun testGetFacilities_usesCache_sameBounds() = runTest {
    val facilities = listOf(Facility(FacilityType.TOILETS, GeoPoint(46.51, 6.61)))

    var onSuccessCallCount = 0 // Used to make sure the first onSuccess callback is only called once

    `when`(mockFacilitiesRepository.getFacilities(any(), any(), any())).then {
      val onSuccess = it.getArgument<(List<Facility>) -> Unit>(1)
      onSuccess(facilities)
    }

    facilitiesViewModel.getFacilities(
        bounds = testBounds,
        onSuccess = { result ->
          assertEquals(facilities, result)
          onSuccessCallCount++
        },
        onFailure = { fail("Should not be called") })

    testDispatcher.scheduler.advanceUntilIdle()

    // Second call to getFacilities should use the cache
    facilitiesViewModel.getFacilities(
        bounds = testBounds,
        onSuccess = { result -> assertEquals(facilities, result) },
        onFailure = { fail("Should not be called") })

    testDispatcher.scheduler.advanceUntilIdle()

    verify(mockFacilitiesRepository, times(1)).getFacilities(any(), any(), any())
    assertEquals(1, onSuccessCallCount)
  }

  @Test
  fun testGetFacilities_usesCache_containedBounds() = runTest {
    val facilities = listOf(Facility(FacilityType.TOILETS, GeoPoint(46.51, 6.61)))

    var onSuccessCallCount = 0 // Used to make sure the first onSuccess callback is only called once

    `when`(mockFacilitiesRepository.getFacilities(any(), any(), any())).then {
      val onSuccess = it.getArgument<(List<Facility>) -> Unit>(1)
      onSuccess(facilities)
    }

    facilitiesViewModel.getFacilities(
        bounds = testBounds,
        onSuccess = { result ->
          assertEquals(facilities, result)
          onSuccessCallCount++
        },
        onFailure = { fail("Should not be called") })

    testDispatcher.scheduler.advanceUntilIdle()

    // Second call to getFacilities should use the cache. The bounds here are within the previous
    // calls bounds
    facilitiesViewModel.getFacilities(
        bounds = Bounds(46.501, 6.601, 46.509, 6.619),
        onSuccess = { result -> assertEquals(facilities, result) },
        onFailure = { fail("Should not be called") })

    testDispatcher.scheduler.advanceUntilIdle()

    verify(mockFacilitiesRepository, times(1)).getFacilities(any(), any(), any())
    assertEquals(1, onSuccessCallCount)
  }

  @Test
  fun testGetFacilities_doesNotUseCache_onDifferentBounds() = runTest {
    val otherBounds = Bounds(41.0, 7.0, 42.0, 8.0)
    val otherFacilities =
        listOf(
            Facility(FacilityType.BENCH, GeoPoint(42.0, 7.5)),
            Facility(FacilityType.DRINKING_WATER, GeoPoint(41.5, 7.1)))

    `when`(mockFacilitiesRepository.getFacilities(eq(testBounds), any(), any())).then {
      val onSuccess = it.getArgument<(List<Facility>) -> Unit>(1)
      onSuccess(testFacilities)
    }

    `when`(mockFacilitiesRepository.getFacilities(eq(otherBounds), any(), any())).then {
      val onSuccess = it.getArgument<(List<Facility>) -> Unit>(1)
      onSuccess(testFacilities)
    }

    facilitiesViewModel.getFacilities(
        bounds = testBounds,
        onSuccess = { result -> assertEquals(testFacilities, result) },
        onFailure = { fail("Should not be called") })

    testDispatcher.scheduler.advanceUntilIdle()

    // Second call to getFacilities should not use the cache, since the bounds do not overlap
    facilitiesViewModel.getFacilities(
        bounds = otherBounds,
        onSuccess = { result -> assertEquals(otherFacilities, result) },
        onFailure = { fail("Should not be called") })

    testDispatcher.scheduler.advanceUntilIdle()

    verify(mockFacilitiesRepository, times(2)).getFacilities(any(), any(), any())
  }
}
