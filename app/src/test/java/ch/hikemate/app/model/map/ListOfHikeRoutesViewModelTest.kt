package ch.hikemate.app.model.map

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

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
    assertNotEquals(listOfHikeRoutesViewModel.hikeRoutes.value.size, 0)
  }

  @Test
  fun canSelectRoute() {
    listOfHikeRoutesViewModel.selectRoute("Route 1")
    assertEquals(listOfHikeRoutesViewModel.selectedHikeRoute.value, "Route 1")
  }
}
