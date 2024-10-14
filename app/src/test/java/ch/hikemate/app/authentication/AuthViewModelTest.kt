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
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any

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
    MockitoAnnotations.openMocks(this)

    staticFirebaseApp = mockStatic(FirebaseApp::class.java)
    staticFirebaseAuth = mockStatic(FirebaseAuth::class.java)

    `when`(FirebaseApp.initializeApp(any<Context>())).thenReturn(mock(FirebaseApp::class.java))
    `when`(FirebaseAuth.getInstance()).thenReturn(mockFirebaseAuth)
  }

  private fun setupSignedInUser() {
    `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
    viewModel = AuthViewModel(mockRepository)
  }

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

    doAnswer { arguments ->
          val onSuccess = arguments.getArgument<(FirebaseUser?) -> Unit>(0)
          onSuccess(mockFirebaseUser) // Call the success callback with a mock user
          null
        }
        .`when`(mockRepository)
        .signInWithGoogle(any(), any(), any(), any(), any())

    assertNull(viewModel.currentUser.first()) // Verify that currentUser is initially null

    viewModel.signInWithGoogle(this, mockContext)

    val currentUser = viewModel.currentUser.first() // Get the first (current) value of the flow
    assertEquals(
        mockFirebaseUser, currentUser) // Verify that currentUser is updated with the mock user
  }

  @Test
  fun signInWithGoogle_calls_repository_and_does_not_update_currentUser_on_error() = runTest {
    setupSignedOutUser()

    doAnswer { invocation ->
          val onErrorAction = invocation.getArgument<(Exception) -> Unit>(1)
          onErrorAction(Exception("Error"))
          null
        }
        .`when`(mockRepository)
        .signInWithGoogle(any(), any(), any(), any(), any())

    assertNull(viewModel.currentUser.first()) // Verify that currentUser is initially null

    viewModel.signInWithGoogle(this, mockContext)

    val currentUser = viewModel.currentUser.first()
    assertEquals(null, currentUser)
  }

  @Test
  fun signOut_calls_repository_signOut_and_updates_currentUser_to_null() = runTest {
    setupSignedInUser()

    doAnswer { arguments ->
          val onSuccess = arguments.getArgument<() -> Unit>(0)
          onSuccess()
          null
        }
        .`when`(mockRepository)
        .signOut(any())

    assertEquals(
        mockFirebaseUser,
        viewModel.currentUser.first()) // Verify that currentUser is initially mockFirebaseUser

    viewModel.signOut()

    verify(mockRepository).signOut(any()) // Verify that the repository's signOut was called
    assertEquals(null, viewModel.currentUser.value) // Verify that currentUser is updated to null
  }
}
