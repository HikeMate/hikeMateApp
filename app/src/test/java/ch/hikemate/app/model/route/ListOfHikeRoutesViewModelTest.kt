package ch.hikemate.app.model.route

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.osmdroid.util.BoundingBox

/** Testing the ListOfRoutesViewModel class */
class ListOfHikeRoutesViewModelTest {
  private lateinit var listOfHikeRoutesViewModel: ListOfHikeRoutesViewModel

  @Before
  fun setUp() {
    listOfHikeRoutesViewModel = ListOfHikeRoutesViewModel()
  }

  @Test
  fun canBeCreatedAsFactory() {
    val factory = ListOfHikeRoutesViewModel.Factory
    val viewModel = factory.create(ListOfHikeRoutesViewModel::class.java)
    assertNotNull(viewModel)
  }

  @Test
  fun canGetRoutes() {
    listOfHikeRoutesViewModel.getRoutes()
    // Wait for the coroutine to finish
    Thread.sleep(500)
    assertNotEquals(listOfHikeRoutesViewModel.hikeRoutes.value.size, 0)
  }

  @Test
  fun canSelectRoute() {
    listOfHikeRoutesViewModel.selectRoute("Route 1")
    assertEquals(listOfHikeRoutesViewModel.selectedHikeRoute.value, "Route 1")
  }

  @Test
  fun canSetArea() {
    listOfHikeRoutesViewModel.setArea(BoundingBox(0.0, 0.0, 0.0, 0.0))
    // Wait for the coroutine to finish
    Thread.sleep(500)
    assertNotEquals(listOfHikeRoutesViewModel.hikeRoutes.value.size, 0)
  }
}
