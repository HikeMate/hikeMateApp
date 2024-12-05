package ch.hikemate.app.model.route

import ch.hikemate.app.model.elevation.ElevationRepository
import ch.hikemate.app.model.extensions.toBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.osmdroid.util.BoundingBox

/** Testing the ListOfRoutesViewModel class */
class ListOfHikeRoutesViewModelTest {
  private lateinit var hikesRepository: HikeRoutesRepository
  private lateinit var elevationRepository: ElevationRepository
  private lateinit var listOfHikeRoutesViewModel: ListOfHikeRoutesViewModel

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())

    hikesRepository = mock(HikeRoutesRepository::class.java)
    elevationRepository = mock(ElevationRepository::class.java)
    listOfHikeRoutesViewModel =
        ListOfHikeRoutesViewModel(hikesRepository, elevationRepository, UnconfinedTestDispatcher())
  }

  @Test
  fun canBeCreatedAsFactory() {
    val factory = ListOfHikeRoutesViewModel.Factory
    val viewModel = factory.create(ListOfHikeRoutesViewModel::class.java)
    assertNotNull(viewModel)
  }

  @Test
  fun getRoutesWithoutBoundingBoxDoesNotCallRepository() {
    // Since we use UnconfinedTestDispatcher, we don't need to wait for the coroutine to finish
    listOfHikeRoutesViewModel.getRoutes()
    verify(hikesRepository, times(0)).getRoutes(any(), any(), any())
  }

  @Test
  fun getRoutesWithBoundingBoxCallsRepositoryWithCorrectBounds() {
    // Calling getRoutes() without setting a bounding box won't call repo, as tested above
    val providedBounds = BoundingBox(0.0, 0.0, 0.0, 0.0)
    listOfHikeRoutesViewModel.setArea(providedBounds)

    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val bounds = it.getArgument<Bounds>(0)
      assertEquals(bounds, providedBounds.toBounds())
    }

    // Since we use UnconfinedTestDispatcher, we don't need to wait for the coroutine to finish
    listOfHikeRoutesViewModel.getRoutes()

    verify(hikesRepository, times(2)).getRoutes(eq(providedBounds.toBounds()), any(), any())
  }

  @Test
  fun getRoutesUpdatesHikeRoutes() {
    // Calling getRoutes() without setting a bounding box won't call repo, as tested above
    listOfHikeRoutesViewModel.setArea(BoundingBox(0.0, 0.0, 0.0, 0.0))

    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
      onSuccess(listOf(HikeRoute("Route 1", Bounds(0.0, 0.0, 0.0, 0.0), emptyList())))
    }

    // Since we use UnconfinedTestDispatcher, we don't need to wait for the coroutine to finish
    listOfHikeRoutesViewModel.getRoutes()

    assertEquals(1, listOfHikeRoutesViewModel.hikeRoutes.value.size)
  }

  @Test
  fun getRouteElevationCallsElevationRepository() {
    val route = HikeRoute("Route 1", Bounds(0.0, 0.0, 0.0, 0.0), emptyList())
    listOfHikeRoutesViewModel.getRoutesElevation(route)
    verify(elevationRepository, times(1)).getElevation(any(), any(), any())
  }

  @Test
  fun getRouteElevationReturnsCorrectElevation() {
    val route = HikeRoute("Route 1", Bounds(0.0, 0.0, 0.0, 0.0), emptyList())
    `when`(elevationRepository.getElevation(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<Double>) -> Unit>(1)
      onSuccess(listOf(1.0, 2.0, 3.0))
    }

    listOfHikeRoutesViewModel.getRoutesElevation(
        route,
        {
          val elevationData = it
          assertEquals(3, elevationData.size)
          assertEquals(1.0, elevationData[0], 0.0)
          assertEquals(2.0, elevationData[1], 0.0)
          assertEquals(3.0, elevationData[2], 0.0)
        },
        { fail("Should not have failed") })
  }

  @Test
  fun getRouteElevationCallsOnFailure() {
    val route = HikeRoute("Route 1", Bounds(0.0, 0.0, 0.0, 0.0), emptyList())
    `when`(elevationRepository.getElevation(any(), any(), any())).thenAnswer {
      val onFailure = it.getArgument<(Exception) -> Unit>(2)
      onFailure(Exception("Test exception"))
    }

    listOfHikeRoutesViewModel.getRoutesElevation(
        route,
        { fail("Should not have succeeded") },
        { // Should be called
        })

    // Verify that the onFailure function was called
    verify(elevationRepository, times(1)).getElevation(any(), any(), any())
  }

  @Test
  fun canSelectRoute() {
    listOfHikeRoutesViewModel.selectRoute(
        HikeRoute("Route 1", Bounds(0.0, 0.0, 0.0, 0.0), emptyList()))
    val route = listOfHikeRoutesViewModel.selectedHikeRoute.value
    assertNotNull(route)
    assertEquals(route!!.id, "Route 1")
    assertEquals(route.bounds, Bounds(0.0, 0.0, 0.0, 0.0))
    assertEquals(route.ways.size, 0)
    assertEquals(route.ways, emptyList<LatLong>())
  }

  @Test
  fun setAreaCallsRepository() {
    // Since we use UnconfinedTestDispatcher, we don't need to wait for the coroutine to finish
    listOfHikeRoutesViewModel.setArea(BoundingBox(0.0, 0.0, 0.0, 0.0))

    verify(hikesRepository, times(1)).getRoutes(eq(Bounds(0.0, 0.0, 0.0, 0.0)), any(), any())
  }

  @Test
  fun setAreaCrossingDateLineCallsRepositoryTwice() {
    // When the hike repository calls the getRoutes method, return on success
    `when`(hikesRepository.getRoutes(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
      onSuccess(emptyList())
    }

    // Since we use UnconfinedTestDispatcher, we don't need to wait for the coroutine to finish
    listOfHikeRoutesViewModel.setArea(BoundingBox(50.0, -170.0, 40.0, 170.0))

    verify(hikesRepository, times(2)).getRoutes(any(), any(), any())
  }

  @Test
  fun selectRouteByIdCallsRepoAndSelectsHike() {
    // Given
    val hike =
        HikeRoute(
            id = "Route 1",
            bounds = Bounds(0.0, 0.0, 0.0, 0.0),
            ways = emptyList(),
            name = "Name of Route 1",
            description = "Description of Route 1")

    `when`(hikesRepository.getRouteById(eq(hike.id), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(HikeRoute) -> Unit>(1)
      onSuccess(hike)
    }

    // When
    // Since we use UnconfinedTestDispatcher, we don't need to wait for the coroutine to finish
    listOfHikeRoutesViewModel.selectRouteById(hike.id)

    // Then
    verify(hikesRepository, times(1)).getRouteById(eq(hike.id), any(), any())
    assertEquals(hike, listOfHikeRoutesViewModel.selectedHikeRoute.value)
  }

  @Test
  fun getRoutesByIdsCallsRepo() {
    val hikesIds = listOf("Route 1", "Route 2")

    // Since we use UnconfinedTestDispatcher, we don't need to wait for the coroutine to finish
    listOfHikeRoutesViewModel.getRoutesByIds(hikesIds)

    verify(hikesRepository, times(1)).getRoutesByIds(eq(hikesIds), any(), any())
  }

  @Test
  fun getRoutesByIdsUpdatesHikeRoutes() {
    val hikes =
        listOf(
            HikeRoute("Route 1", Bounds(0.0, 0.0, 0.0, 0.0), emptyList()),
            HikeRoute("Route 2", Bounds(0.0, 0.0, 0.0, 0.0), emptyList()))

    val hikesIds = hikes.map { it.id }

    `when`(hikesRepository.getRoutesByIds(eq(hikesIds), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(List<HikeRoute>) -> Unit>(1)
      onSuccess(hikes)
    }

    // Since we use UnconfinedTestDispatcher, we don't need to wait for the coroutine to finish
    listOfHikeRoutesViewModel.getRoutesByIds(hikesIds)

    assertEquals(2, listOfHikeRoutesViewModel.hikeRoutes.value.size)
  }

  @Test
  fun getRoutesByIdsCallsOnFailure() {
    val hikesIds = listOf("Route 1", "Route 2")

    `when`(hikesRepository.getRoutesByIds(eq(hikesIds), any(), any())).thenAnswer {
      val onFailure = it.getArgument<(Exception) -> Unit>(2)
      onFailure(Exception("Test exception"))
    }

    var onFailedCalled = false
    // Since we use UnconfinedTestDispatcher, we don't need to wait for the coroutine to finish
    listOfHikeRoutesViewModel.getRoutesByIds(
        hikesIds,
        { fail("Should not have succeeded") },
        {
          // Should be called
          onFailedCalled = true
        })

    assertTrue(onFailedCalled)
    // Verify that the onFailure function was called
    verify(hikesRepository, times(1)).getRoutesByIds(eq(hikesIds), any(), any())
  }
}
