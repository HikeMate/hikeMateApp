package ch.hikemate.app.model.route

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
  private lateinit var listOfHikeRoutesViewModel: ListOfHikeRoutesViewModel

  @Before
  fun setUp() {
    hikesRepository = mock(HikeRoutesRepository::class.java)
    listOfHikeRoutesViewModel = ListOfHikeRoutesViewModel(hikesRepository)
  }

  @Test
  fun canBeCreatedAsFactory() {
    val factory = ListOfHikeRoutesViewModel.Factory
    val viewModel = factory.create(ListOfHikeRoutesViewModel::class.java)
    assertNotNull(viewModel)
  }

  @Test
  fun getRoutesWithoutBoundingBoxDoesNotCallRepository() {
    listOfHikeRoutesViewModel.getRoutes()
    // Wait for the coroutine to finish
    // TODO : Try to remove the hardcoded dispatchers and see if it allows to remove the sleep
    Thread.sleep(500)
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

    listOfHikeRoutesViewModel.getRoutes()
    // Wait for the coroutine to finish
    // TODO : Try to remove the hardcoded dispatchers and see if it allows to remove the sleep
    Thread.sleep(500)

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

    listOfHikeRoutesViewModel.getRoutes()
    // Wait for the coroutine to finish
    // TODO : Try to remove the hardcoded dispatchers and see if it allows to remove the sleep
    Thread.sleep(500)
    assertEquals(1, listOfHikeRoutesViewModel.hikeRoutes.value.size)
  }

  @Test
  fun canSelectRoute() {
    listOfHikeRoutesViewModel.selectRoute(HikeRoute("Route 1", Bounds(0.0, 0.0, 0.0, 0.0), emptyList()))
    // TODO : Is the StateFlow immediately updated? If not, how to test this?
    val route = listOfHikeRoutesViewModel.selectedHikeRoute.value
    assertNotNull(route)
    assertEquals(route!!.id, "Route 1")
    assertEquals(route.bounds, Bounds(0.0, 0.0, 0.0, 0.0))
    assertEquals(route.ways.size, 0)
    assertEquals(route.ways, emptyList<LatLong>())
  }

  @Test
  fun setAreaCallsRepository() {
    listOfHikeRoutesViewModel.setArea(BoundingBox(0.0, 0.0, 0.0, 0.0))
    // Wait for the coroutine to finish
    // TODO : Try to remove the hardcoded dispatchers and see if it allows to remove the sleep
    Thread.sleep(500)
    verify(hikesRepository, times(1)).getRoutes(eq(Bounds(0.0, 0.0, 0.0, 0.0)), any(), any())
  }
}
