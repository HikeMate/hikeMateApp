package ch.hikemate.app.model.profile

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
          hikingLevel = HikingLevel.INTERMEDIATE,
          joinedDate = Timestamp(1609459200, 0))

  @Mock private lateinit var repository: ProfileRepository
  @Mock private lateinit var firebaseAuth: com.google.firebase.auth.FirebaseAuth
  @Mock private lateinit var firebaseUser: FirebaseUser
  private lateinit var profileViewModel: ProfileViewModel

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    FirebaseApp.initializeApp(context)
    MockitoAnnotations.openMocks(this)

    firebaseAuth = mock(com.google.firebase.auth.FirebaseAuth::class.java)
    firebaseUser = mock(FirebaseUser::class.java)

    `when`(firebaseAuth.currentUser).thenReturn(firebaseUser)
    `when`(firebaseUser.uid).thenReturn("1")

    `when`(firebaseAuth.addAuthStateListener(any())).thenAnswer {
      val listener = it.getArgument<FirebaseAuth.AuthStateListener>(0)
      listener.onAuthStateChanged(firebaseAuth)
      null
    }

    `when`(repository.init(any())).thenAnswer {
      val initAction = it.getArgument<() -> Unit>(0)
      initAction()
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
  fun addProfileCallsRepository() {
    `when`(repository.addProfile(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<() -> Unit>(1)
      onSuccess()
    }
    `when`(repository.getProfileById(any(), any(), any())).thenAnswer {
      val onSuccess = it.getArgument<(Profile) -> Unit>(1)
      onSuccess(profile)
    }

    profileViewModel.addProfile(profile)

    verify(repository).addProfile(eq(profile), any(), any())

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
}
