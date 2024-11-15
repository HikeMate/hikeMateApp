package ch.hikemate.app.authentication

import android.content.Context
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.model.authentication.FirebaseAuthRepository
import ch.hikemate.app.model.profile.Profile
import ch.hikemate.app.model.profile.ProfileRepository
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class AuthViewModelTest {
  @Mock private lateinit var mockAuthResult: Task<AuthResult>
  @Mock private lateinit var mockRepository: FirebaseAuthRepository

  @Mock private lateinit var mockProfile: ProfileRepository

  @Mock private lateinit var mockContext: Context

  @Mock private lateinit var mockFirebaseAuth: FirebaseAuth

  @Mock private lateinit var mockFirebaseUser: FirebaseUser

  @Mock private lateinit var mockProfileUser: Profile

  @Mock private lateinit var mockTask: Task<Void>

  private lateinit var staticFirebaseApp: MockedStatic<FirebaseApp>

  private lateinit var staticFirebaseAuth: MockedStatic<FirebaseAuth>

  // The ViewModel under test
  private lateinit var viewModel: AuthViewModel

  @Before
  fun setup() {
    mockRepository = mock()
    mockAuthResult = mock()
    mockContext = mock()
    mockFirebaseAuth = mock()
    mockProfileUser = mock()
    mockTask = mock()
    mockFirebaseUser = mock()
    mockProfile = mock()

    staticFirebaseApp = mockStatic(FirebaseApp::class.java)
    staticFirebaseAuth = mockStatic(FirebaseAuth::class.java)

    // Define the behavior of FirebaseApp and FirebaseAuth when called.
    // This is necessary because the AuthViewModel calls and uses on FirebaseAuth.
    `when`(FirebaseApp.initializeApp(any<Context>())).thenReturn(mock(FirebaseApp::class.java))
    `when`(FirebaseAuth.getInstance()).thenReturn(mockFirebaseAuth)
  }

  // Helper functions to setup the ViewModel with a signed-in or signed-out user
  private fun setupSignedInUser() {
    `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
    viewModel = AuthViewModel(mockRepository, mockProfile)
  }

  // Helper functions to setup the ViewModel with a signed-in or signed-out user
  private fun setupSignedOutUser() {
    `when`(mockFirebaseAuth.currentUser).thenReturn(null)
    viewModel = AuthViewModel(mockRepository, mockProfile)
  }

  @After
  fun tearDown() {
    staticFirebaseApp.close()
    staticFirebaseAuth.close()
  }

  @Test
  fun signInWithGoogle_calls_repository_and_updates_currentUser_on_success() = runTest {
    setupSignedOutUser()

    // Mock the sign-in process to immediately invoke the onSuccess callback with a mock user
    doAnswer { invocation ->
          val onSuccess = invocation.getArgument<(FirebaseUser?) -> Unit>(0)
          onSuccess(mockFirebaseUser) // Trigger success callback with mock user
          null
        }
        .`when`(mockRepository)
        .signInWithGoogle(any(), any(), any(), any(), any(), anyOrNull())

    // Mock the profile creation to immediately invoke the onSuccess callback
    doAnswer { invocation ->
          val onSuccess = invocation.getArgument<(Profile) -> Unit>(1)
          onSuccess(mockProfileUser) // Trigger success callback with mock profile
          null
        }
        .`when`(mockProfile)
        .createProfile(any(), any(), any(), any())

    // Verify that `currentUser` is initially null
    assertNull(viewModel.currentUser.first())

    // Call the function being tested
    viewModel.signInWithGoogle(this, mockContext, null)

    // Verify interactions with the repository and profile repository
    verify(mockRepository).signInWithGoogle(any(), any(), any(), any(), any(), anyOrNull())
    verify(mockProfile).createProfile(any(), any(), any(), any())
    val currentUser = viewModel.currentUser.first() // Get the first (current) value of the flow

    // Confirm that currentUser is now logged in
    assertEquals(mockFirebaseUser, currentUser)
  }

  @Test
  fun signInWithGoogle_calls_repository_and_does_not_update_currentUser_on_error() = runTest {
    setupSignedOutUser()

    // Simulate an unsuccessful Google sign-in by invoking the onError callback
    doAnswer { invocation ->
          val onErrorAction = invocation.getArgument<(Exception) -> Unit>(1)
          onErrorAction(Exception("Error"))
          null
        }
        .`when`(mockRepository)
        .signInWithGoogle(any(), any(), any(), any(), any(), anyOrNull())

    // Verify that currentUser is initially null
    assertNull(viewModel.currentUser.first())

    viewModel.signInWithGoogle(this, mockContext, null)

    val currentUser = viewModel.currentUser.first()

    // Verify call to the repository
    verify(mockRepository).signInWithGoogle(any(), any(), any(), any(), any(), anyOrNull())

    // Confirm that currentUser is still null
    assertEquals(null, currentUser)
  }

  @Test
  fun signInWithEmailAndPassword_calls_repository_and_updates_currentUser_on_success() = runTest {
    setupSignedOutUser()

    // Simulate a successful email and password sign-in by invoking the onSuccess callback
    doAnswer { invocation ->
          val onSuccess = invocation.getArgument<(FirebaseUser?) -> Unit>(2)
          onSuccess(mockFirebaseUser) // Call the success callback with a mock user
          null
        }
        .`when`(mockRepository)
        .signInWithEmailAndPassword(any(), any(), any(), any())

    // Verify that currentUser is initially null
    assertNull(viewModel.currentUser.first())

    viewModel.signInWithEmailAndPassword(
        "mock@example.com", "password", {}, { fail("Error callback should not be called") })

    val currentUser = viewModel.currentUser.first() // Get the first (current) value of the flow

    // Verify call to the repository
    verify(mockRepository).signInWithEmailAndPassword(any(), any(), any(), any())

    assertEquals(mockFirebaseUser, currentUser)
  }

  @Test
  fun signInWithEmailAndPassword_calls_repository_and_does_not_update_currentUser_on_error() =
      runTest {
        setupSignedOutUser()

        // Simulate an unsuccessful email and password sign-in by invoking the onError callback
        doAnswer { invocation ->
              val onErrorAction = invocation.getArgument<(Exception) -> Unit>(3)
              onErrorAction(Exception("Error"))
              null
            }
            .`when`(mockRepository)
            .signInWithEmailAndPassword(any(), any(), any(), any())

        // Verify that currentUser is initially null
        assertNull(viewModel.currentUser.first())

        viewModel.signInWithEmailAndPassword(
            "mock@example.com", "password", { fail("Success callback should not be called") }, {})

        val currentUser = viewModel.currentUser.first()

        // Verify call to the repository
        verify(mockRepository).signInWithEmailAndPassword(any(), any(), any(), any())

        // Confirm that currentUser is still null
        assertNull(currentUser)
      }

  @Test
  fun createAccountWithEmailAndPassword_calls_repository_and_updates_currentUser_on_success() =
      runTest {
        setupSignedOutUser()

        // Simulate successful creation of account
        doAnswer { invocation ->
              val onSuccess = invocation.getArgument<(FirebaseUser?) -> Unit>(0)
              onSuccess(mockFirebaseUser)
              null
            }
            .whenever(mockRepository)
            .createAccountWithEmailAndPassword(any(), any(), any(), any())

        doAnswer { invocation ->
              val onSuccess = invocation.getArgument<(Profile) -> Unit>(1)
              onSuccess(mockProfileUser) // Trigger success callback with mock profile
              null
            }
            .whenever(mockProfile)
            .createProfile(any(), any(), any(), any())

        // Create ArgumentCaptor for profile update request
        val profileUpdateRequest = ArgumentCaptor.forClass(UserProfileChangeRequest::class.java)

        // Mock user profile update to complete successfully

        `when`(mockFirebaseAuth.createUserWithEmailAndPassword(any(), any()))
            .thenReturn(mockAuthResult)
        `when`(mockFirebaseUser.updateProfile(profileUpdateRequest.capture())).thenReturn(mockTask)
        `when`(mockTask.isSuccessful).thenReturn(true)

        // Mock task completion
        `when`(mockTask.addOnCompleteListener(any())).thenAnswer { invocation ->
          val listener = invocation.getArgument<OnCompleteListener<Void>>(0)
          listener.onComplete(mockTask)
          mockTask
        }
        `when`(mockAuthResult.addOnCompleteListener(any())).thenAnswer { invocation ->
          val listener = invocation.getArgument<OnCompleteListener<Void>>(0)
          listener.onComplete(mockTask)
          mockTask
        }

        // Simulate profile creation success
        doAnswer { invocation ->
              val onSuccess = invocation.getArgument<(Profile) -> Unit>(1)
              onSuccess.invoke(mockProfileUser) // And this line
              null
            }
            .whenever(mockProfile)
            .createProfile(any(), any(), any(), any())

        // Verify initial state
        assertNull(viewModel.currentUser.value)

        // Execute the function being tested
        viewModel.createAccountWithEmailAndPassword(
            name = "Test User",
            email = "test@example.com",
            password = "password123",
            context = mockContext,
            onSuccess = {},
            onErrorAction = { fail("Error callback should not be called") })

        // Verify the sequence of operations
        verify(mockRepository)
            .createAccountWithEmailAndPassword(
                any(),
                any(),
                eq("test@example.com"),
                eq("password123"),
            )

        // Verify profile update was called with correct name
        verify(mockFirebaseUser).updateProfile(any())
        assertEquals("Test User", profileUpdateRequest.value.displayName)

        // Verify profile creation was called with correct user
        verify(mockProfile).createProfile(eq(mockFirebaseUser), any(), any(), any())

        // Verify final state
        assertEquals(mockFirebaseUser, viewModel.currentUser.value)
      }

  @Test
  fun createAccountWithEmailAndPassword_calls_repository_and_does_not_update_currentUser_on_error() =
      runTest {
        setupSignedOutUser()

        // Simulate an unsuccessful email and password account creation by invoking the onError
        // callback
        doAnswer { invocation ->
              val onErrorAction = invocation.getArgument<(Exception) -> Unit>(1)
              onErrorAction(Exception("Error"))
              null
            }
            .`when`(mockRepository)
            .createAccountWithEmailAndPassword(any(), any(), any(), any())

        // Verify that currentUser is initially null
        assertNull(viewModel.currentUser.first())

        viewModel.createAccountWithEmailAndPassword(
            "",
            "mock@example.com",
            "password",
            { fail("Success callback should not be called") },
            {},
            context = mockContext)

        val currentUser = viewModel.currentUser.first()

        // Verify call to the repository
        verify(mockRepository).createAccountWithEmailAndPassword(any(), any(), any(), any())

        // Confirm that currentUser is still null
        assertNull(currentUser)
      }

  @Test
  fun signOut_calls_repository_signOut_and_updates_currentUser_to_null() = runTest {
    setupSignedInUser()

    val mockOnSuccess: (() -> Unit) = mock()

    // Simulate a successful sign-out by invoking the onSuccess callback
    doAnswer { arguments ->
          val onSuccess = arguments.getArgument<() -> Unit>(0)
          onSuccess()
          null
        }
        .`when`(mockRepository)
        .signOut(any())

    // Verify that currentUser is initially mockFirebaseUser
    assertEquals(mockFirebaseUser, viewModel.currentUser.first())

    viewModel.signOut { mockOnSuccess() }

    // Verify that the repository's signOut was called
    verify(mockRepository).signOut(any())
    // Verify that currentUser is updated to null
    assertEquals(null, viewModel.currentUser.value)
    // Verify that the onSuccess callback was called
    verify(mockOnSuccess).invoke()
  }
}
