package ch.hikemate.app.model.profile

import androidx.lifecycle.ViewModelProvider
import com.google.firebase.Timestamp
import junit.framework.TestCase.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any

class ProfileViewModelTest {

  private val profile =
      Profile(
          id = "1",
          name = "John Doe",
          email = "john.doe@gmail.com",
          fitnessLevel = FitnessLevel(8, "Very fit"),
          joinedDate = Timestamp(1609459200, 0))

  @Mock private lateinit var repository: ProfileRepository
  @Mock private lateinit var mockFactory: ViewModelProvider.Factory
  private lateinit var profileViewModel: ProfileViewModel

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    profileViewModel = ProfileViewModel(repository)

    `when`(repository.init(any())).thenAnswer {
      val init = it.getArgument<() -> Unit>(0)
      init()
    }

    `when`(mockFactory.create(ProfileViewModel::class.java)).thenReturn(profileViewModel)
  }

  @Test
  fun canBeCreatedAsFactory() {
    val factory = mockFactory
    val viewModel = factory.create(ProfileViewModel::class.java)
    assertNotNull(viewModel)
  }

  @Test
  fun getProfileById() {
    `when`(repository.getProfileById(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(Profile) -> Unit>(1)
      onSuccess(profile)
    }

    profileViewModel.getProfileById("1")

    assert(profileViewModel.profile.value == profile)
  }

  @Test
  fun addProfile() {
    `when`(repository.addProfile(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<() -> Unit>(1)
      onSuccess()
    }
    `when`(repository.getProfileById(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(Profile) -> Unit>(1)
      onSuccess(profile)
    }

    profileViewModel.addProfile(profile)

    assert(profileViewModel.profile.value == profile)
  }

  @Test
  fun updateProfile() {
    `when`(repository.updateProfile(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<() -> Unit>(1)
      onSuccess()
    }

    profileViewModel.updateProfile(profile)

    assert(profileViewModel.profile.value == profile)
  }

  @Test
  fun deleteProfileById() {
    `when`(repository.deleteProfileById(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<() -> Unit>(1)
      onSuccess()
    }

    profileViewModel.deleteProfileById("1")

    assert(profileViewModel.profile.value == null)
  }
}
