package ch.hikemate.app.model.facilities

import ch.hikemate.app.model.route.Bounds
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.osmdroid.util.GeoPoint

class FacilitiesViewModelTest {

  private lateinit var mockFacilitiesRepository: FacilitiesRepository
  private lateinit var facilitiesViewModel: FacilitiesViewModel

  private val testBounds = Bounds(46.5, 6.6, 46.51, 6.62)

  @Before
  fun setUp() {
    mockFacilitiesRepository = mock()
    facilitiesViewModel = FacilitiesViewModel(mockFacilitiesRepository)
  }

  @Test
  fun testGetFacilities_onSuccess() {
    val mockFacility = Facility(FacilityType.TOILETS, GeoPoint(46.51, 6.61))
    val mockFacilities = listOf(mockFacility)
    val onSuccess: (List<Facility>) -> Unit = { facilities ->
      assert(facilities.contains(mockFacility))
    }
    val onFailure: (Exception) -> Unit = { fail() }

    `when`(mockFacilitiesRepository.getFacilities(any(), any(), any())).then {
      val onSuccess = it.getArgument<(List<Facility>) -> Unit>(1)
      onSuccess(mockFacilities)
    }

    facilitiesViewModel.getFacilities(testBounds, onSuccess, onFailure)
  }
}
