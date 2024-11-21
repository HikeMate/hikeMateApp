package ch.hikemate.app.model.facilities

import okhttp3.OkHttpClient

class FacilitiesViewModelTest {

  val viewModel = FacilitiesViewModel(FacilitiesRepository(OkHttpClient()))
}
