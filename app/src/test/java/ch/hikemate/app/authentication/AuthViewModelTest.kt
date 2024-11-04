package ch.hikemate.app.authentication

import android.content.Context
import ch.hikemate.app.model.authentication.AuthViewModel
import ch.hikemate.app.model.authentication.FirebaseAuthRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull

class AuthViewModelTest {
  @Mock private lateinit var mockRepository: FirebaseAuthRepository

  @Mock private lateinit var mockContext: Context

  @Mock private lateinit var mockFirebaseAuth: FirebaseAuth

  @Mock private lateinit var mockFirebaseUser: FirebaseUser

  private lateinit var staticFirebaseApp: MockedStatic<FirebaseApp>

  private lateinit var staticFirebaseAuth: MockedStatic<FirebaseAuth>

  // The ViewModel under test
  private lateinit var viewModel: AuthViewModel

  @Before
  fun setup() {
    mockRepository = mock()
    mockContext = mock()
    mockFirebaseAuth = mock()
    mockFirebaseUser = mock()

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
    viewModel = AuthViewModel(mockRepository)
  }

  // Helper functions to setup the ViewModel with a signed-in or signed-out user
  private fun setupSignedOutUser() {
    `when`(mockFirebaseAuth.currentUser).thenReturn(null)
    viewModel = AuthViewModel(mockRepository)
  }

  @After
  fun tearDown() {
    staticFirebaseApp.close()
    staticFirebaseAuth.close()
  }

  @Test
  fun signInWithGoogle_calls_repository_and_updates_currentUser_on_success() = runTest {
    setupSignedOutUser()

    // Simulate a successful Google sign-in by invoking the onSuccess callback
    doAnswer { arguments ->
          val onSuccess = arguments.getArgument<(FirebaseUser?) -> Unit>(0)
          onSuccess(mockFirebaseUser) // Call the success callback with a mock user
          null
        }
        .`when`(mockRepository)
        .signInWithGoogle(any(), any(), any(), any(), any(), anyOrNull())

    // Verify that currentUser is initially null
    assertNull(viewModel.currentUser.first())

    viewModel.signInWithGoogle(this, mockContext, null)

    // Verify call to the repository
    verify(mockRepository).signInWithGoogle(any(), any(), any(), any(), any(), anyOrNull())

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
          val onSuccess = invocation.getArgument<(FirebaseUser?) -> Unit>(0)
          onSuccess(mockFirebaseUser) // Call the success callback with a mock user
          null
        }
        .`when`(mockRepository)
        .signInWithEmailAndPassword(any(), any(), any(), any())

    // Verify that currentUser is initially null
    assertNull(viewModel.currentUser.first())

    viewModel.signInWithEmailAndPassword("mock@example.com", "password")

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
              val onErrorAction = invocation.getArgument<(Exception) -> Unit>(1)
              onErrorAction(Exception("Error"))
              null
            }
            .`when`(mockRepository)
            .signInWithEmailAndPassword(any(), any(), any(), any())

        // Verify that currentUser is initially null
        assertNull(viewModel.currentUser.first())

        viewModel.signInWithEmailAndPassword("mock@example.com", "password")

        val currentUser = viewModel.currentUser.first()

        // Verify call to the repository
        verify(mockRepository).signInWithEmailAndPassword(any(), any(), any(), any())

        // Confirm that currentUser is still null
        assertNull(currentUser)
      }

  @Test
  fun signOut_calls_repository_signOut_and_updates_currentUser_to_null() = runTest {
    setupSignedInUser()

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

    viewModel.signOut()

    // Verify that the repository's signOut was called
    verify(mockRepository).signOut(any())
    // Verify that currentUser is updated to null
    assertEquals(null, viewModel.currentUser.value)
  }
}
