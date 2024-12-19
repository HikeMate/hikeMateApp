package ch.hikemate.app.model.profile

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.hikemate.app.R
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ProfileViewModelTest {

  private lateinit var context: Context

  private val profile =
      Profile(
          id = "1",
          name = "John Doe",
          email = "john.doe@gmail.com",
          hikingLevel = HikingLevel.AMATEUR,
          joinedDate = Timestamp(1609459200, 0))

  @Mock private lateinit var repository: ProfileRepository
  @Mock private lateinit var firebaseAuth: FirebaseAuth
  @Mock private lateinit var firebaseUser: FirebaseUser
  private lateinit var profileViewModel: ProfileViewModel

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    FirebaseApp.initializeApp(context)
    MockitoAnnotations.openMocks(this)

    firebaseAuth = mock(FirebaseAuth::class.java)
    firebaseUser = mock(FirebaseUser::class.java)

    `when`(firebaseAuth.currentUser).thenReturn(firebaseUser)
    `when`(firebaseUser.uid).thenReturn("1")

    `when`(firebaseAuth.addAuthStateListener(any())).thenAnswer {
      val listener = it.getArgument<FirebaseAuth.AuthStateListener>(0)
      listener.onAuthStateChanged(firebaseAuth)
      null
    }

    profileViewModel = ProfileViewModel(repository)
  }

  @Test
  fun canBeCreatedAsFactory() {
    val factory = ProfileViewModel.Factory
    val viewModel = factory.create(ProfileViewModel::class.java)
    assertNotNull(viewModel)
  }

  @Test
  fun getProfileByIdCallsRepository() {
    `when`(repository.getProfileById(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(Profile) -> Unit>(1)
      onSuccess(profile)
    }

    profileViewModel.getProfileById("1")

    verify(repository).getProfileById(eq("1"), any(), any())

    assert(profileViewModel.profile.value == profile)
  }

  @Test
  fun updateProfileCallsRepository() {
    `when`(repository.updateProfile(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<() -> Unit>(1)
      onSuccess()
    }

    profileViewModel.updateProfile(profile)

    verify(repository).updateProfile(eq(profile), any(), any())

    assert(profileViewModel.profile.value == profile)
  }

  @Test
  fun deleteProfileByIdCallsRepository() {
    `when`(repository.deleteProfileById(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<() -> Unit>(1)
      onSuccess()
    }

    profileViewModel.deleteProfileById("1")

    verify(repository).deleteProfileById(eq("1"), any(), any())

    assert(profileViewModel.profile.value == null)
  }

  @Test
  fun getProfileById_errorThrownByRepositoryIsHandled() {
    `when`(repository.getProfileById(any(), any(), any())).thenAnswer {
      val onFailure = it.getArgument<(Exception) -> Unit>(2)
      onFailure(Exception(context.getString(R.string.an_error_occurred_while_fetching_the_profile)))
    }

    profileViewModel.getProfileById("1")

    assertEquals(
        profileViewModel.errorMessageId.value,
        R.string.an_error_occurred_while_fetching_the_profile)
  }

  @Test
  fun updateProfile_errorThrownByRepositoryIsHandled() {
    `when`(repository.updateProfile(any(), any(), any())).thenAnswer {
      val onFailure = it.getArgument<(Exception) -> Unit>(2)
      onFailure(Exception(context.getString(R.string.an_error_occurred_while_updating_the_profile)))
    }

    profileViewModel.updateProfile(profile)

    assertEquals(
        profileViewModel.errorMessageId.value,
        R.string.an_error_occurred_while_updating_the_profile)
  }

  @Test
  fun deleteProfileById_errorThrownByRepositoryIsHandled() {
    `when`(repository.deleteProfileById(any(), any(), any())).thenAnswer {
      val onFailure = it.getArgument<(Exception) -> Unit>(2)
      onFailure(Exception(context.getString(R.string.an_error_occurred_while_deleting_the_profile)))
    }

    profileViewModel.deleteProfileById("1")

    assertEquals(
        profileViewModel.errorMessageId.value,
        R.string.an_error_occurred_while_deleting_the_profile)
  }
}
