package ch.hikemate.app.model.facilities

import ch.hikemate.app.model.route.Bounds
import ch.hikemate.app.model.route.LatLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class FacilitiesViewModelTest {

  private lateinit var mockFacilitiesRepository: FacilitiesRepositoryOverpass
  private lateinit var testDispatcher: TestDispatcher

  private lateinit var facilitiesViewModel: FacilitiesViewModel // SUT

  private val testBounds = Bounds(46.5, 6.6, 46.51, 6.62)
  private val testFacilities = listOf(Facility(FacilityType.TOILETS, LatLong(46.51, 6.61)))

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
    val facilities = listOf(Facility(FacilityType.TOILETS, LatLong(46.51, 6.61)))

    `when`(mockFacilitiesRepository.getFacilities(any(), any(), any())).then {
      val onSuccess = it.getArgument<(List<Facility>) -> Unit>(1)
      onSuccess(facilities)
    }

    var onSuccessCalled = false

    facilitiesViewModel.getFacilities(
        bounds = testBounds,
        onSuccess = { result ->
          assertEquals(facilities, result)
          onSuccessCalled = true
        },
        onFailure = { fail("Should not be called") })

    assertTrue(onSuccessCalled)
  }

  @Test
  fun testGetFacilities_onFailure() {
    val exception = Exception("Test exception")

    `when`(mockFacilitiesRepository.getFacilities(any(), any(), any())).then {
      val onFailure = it.getArgument<(Exception) -> Unit>(2)
      onFailure(exception)
    }

    var onFailureCalled = false

    facilitiesViewModel.getFacilities(
        bounds = testBounds,
        onSuccess = { fail("Should not be called") },
        onFailure = { ex ->
          assertEquals(exception, ex)
          onFailureCalled = true
        })

    assertTrue(onFailureCalled)
  }

  @Test
  fun testGetFacilities_usesCache_sameBounds() = runTest {
    val facilities = listOf(Facility(FacilityType.TOILETS, LatLong(46.51, 6.61)))

    // makes sure each onSuccess is called exactly once
    var onSuccessCalledFirstCall = 0
    var onSuccessCalledSecondCall = 0

    `when`(mockFacilitiesRepository.getFacilities(any(), any(), any())).then {
      val onSuccess = it.getArgument<(List<Facility>) -> Unit>(1)
      onSuccess(facilities)
    }

    facilitiesViewModel.getFacilities(
        bounds = testBounds,
        onSuccess = { result ->
          assertEquals(facilities, result)
          onSuccessCalledFirstCall++
        },
        onFailure = { fail("Should not be called") })

    testDispatcher.scheduler.advanceUntilIdle()

    // Second call to getFacilities should use the cache
    facilitiesViewModel.getFacilities(
        bounds = testBounds,
        onSuccess = { result ->
          assertEquals(facilities, result)
          onSuccessCalledSecondCall++
        },
        onFailure = { fail("Should not be called") })

    testDispatcher.scheduler.advanceUntilIdle()

    verify(mockFacilitiesRepository, times(1)).getFacilities(any(), any(), any())
    assertEquals(1, onSuccessCalledFirstCall)
    assertEquals(1, onSuccessCalledSecondCall)
  }

  @Test
  fun testGetFacilities_usesCache_containedBounds() = runTest {
    val facilities = listOf(Facility(FacilityType.TOILETS, LatLong(46.505, 6.605)))

    var onSuccessCallCountFirstCall = 0
    var onSuccessCallCountSecondCall = 0

    `when`(mockFacilitiesRepository.getFacilities(any(), any(), any())).then {
      val onSuccess = it.getArgument<(List<Facility>) -> Unit>(1)
      onSuccess(facilities)
    }

    facilitiesViewModel.getFacilities(
        bounds = testBounds,
        onSuccess = { result ->
          assertEquals(facilities, result)
          onSuccessCallCountFirstCall++
        },
        onFailure = { fail("Should not be called") })

    testDispatcher.scheduler.advanceUntilIdle()

    // Second call to getFacilities should use the cache. The bounds here are within the previous
    // calls bounds
    facilitiesViewModel.getFacilities(
        bounds = Bounds(46.501, 6.601, 46.509, 6.619),
        onSuccess = { result ->
          assertEquals(facilities, result)
          onSuccessCallCountSecondCall++
        },
        onFailure = { fail("Should not be called") })

    testDispatcher.scheduler.advanceUntilIdle()

    verify(mockFacilitiesRepository, times(1)).getFacilities(any(), any(), any())
    assertEquals(1, onSuccessCallCountFirstCall)
    assertEquals(1, onSuccessCallCountSecondCall)
  }

  @Test
  fun testGetFacilities_doesNotUseCache_onDifferentBounds() = runTest {
    // otherBounds does not overlap with testBounds
    val otherBounds = Bounds(41.0, 7.0, 42.0, 8.0)
    val otherFacilities = // two facilities contained within "otherBounds"
        listOf(
            Facility(FacilityType.BENCH, LatLong(41.1, 7.51)),
            Facility(FacilityType.DRINKING_WATER, LatLong(41.2, 7.2)))

    `when`(mockFacilitiesRepository.getFacilities(eq(testBounds), any(), any())).then {
      val onSuccess = it.getArgument<(List<Facility>) -> Unit>(1)
      onSuccess(testFacilities)
    }

    `when`(mockFacilitiesRepository.getFacilities(eq(otherBounds), any(), any())).then {
      val onSuccess = it.getArgument<(List<Facility>) -> Unit>(1)
      onSuccess(otherFacilities)
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
