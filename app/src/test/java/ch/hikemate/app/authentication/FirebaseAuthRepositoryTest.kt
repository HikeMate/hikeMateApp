package ch.hikemate.app.authentication

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.hikemate.app.model.authentication.FirebaseAuthRepository
import ch.hikemate.app.model.profile.ProfileRepository
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import io.mockk.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class FirebaseAuthRepositoryTest {

  private lateinit var context: Context

  // Repository under Test
  private lateinit var repository: FirebaseAuthRepository

  // Mock objects
  @Mock private lateinit var profileRepository: ProfileRepository

  private lateinit var mockCredentialManager: CredentialManager
  private lateinit var mockFirebaseAuth: FirebaseAuth
  private lateinit var mockFirebaseUser: FirebaseUser
  private lateinit var mockFirebaseApp: FirebaseApp
  private lateinit var mockAuthCredential: AuthCredential
  private lateinit var mockCredential: Credential
  private lateinit var mockCredentialResponse: GetCredentialResponse
  private lateinit var mockTask: Task<AuthResult>
  private lateinit var mockOnSuccess: (FirebaseUser?) -> Unit
  private lateinit var mockOnError: (Throwable) -> Unit

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()

    // Initialize mocks
    mockCredentialManager = mockk(relaxed = true)
    mockFirebaseAuth = mockk(relaxed = true)
    mockFirebaseUser = mockk(relaxed = true)
    mockFirebaseApp = mockk(relaxed = true)
    mockAuthCredential = mockk(relaxed = true)
    mockCredential = mockk(relaxed = true)
    mockCredentialResponse = mockk(relaxed = true)
    mockTask = mockk(relaxed = true)
    profileRepository = mockk(relaxed = true)

    mockOnSuccess = mockk(relaxed = true)
    mockOnError = mockk(relaxed = true)

    // Mock FirebaseApp initialization
    mockkStatic(FirebaseApp::class)
    every { FirebaseApp.initializeApp(any()) } returns mockFirebaseApp

    // Mock Firebase object
    mockkObject(Firebase)
    every { FirebaseAuth.getInstance() } returns mockFirebaseAuth
    every { Firebase.auth } returns mockFirebaseAuth

    // Mock GoogleAuthProvider
    mockkStatic(GoogleAuthProvider::class)
    every { GoogleAuthProvider.getCredential(any(), null) } returns mockAuthCredential

    // Mock behavior for the CredentialManager
    every { mockCredentialResponse.credential } returns mockCredential
    coEvery { mockCredentialManager.getCredential(any(), any<GetCredentialRequest>()) } returns
        mockCredentialResponse

    // Mock behavior authentication with Firebase
    every { mockFirebaseAuth.signInWithCredential(any()) } returns mockTask
    every { mockFirebaseAuth.signInWithEmailAndPassword(any(), any()) } returns mockTask
    every { mockFirebaseAuth.createUserWithEmailAndPassword(any(), any()) } returns mockTask
    every { mockTask.addOnCompleteListener(any()) } answers
        {
          val listener = firstArg<OnCompleteListener<AuthResult>>()
          listener.onComplete(mockTask)
          mockTask
        }

    // Initialize the Firebase and the repository
    FirebaseApp.initializeApp(context)
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun testCreateAccountWithEmailAndPassword_successful() =
      runTest(timeout = 5.seconds) {
        every { mockTask.isSuccessful } returns true

        /* We hardcode that FirebaseAuth.currentUser is a valid user even before the login process is successful, since we are mocking Firebase's
        functionality almost completely, so we don't have access Firebase's backend, and can therefore not
        organically sign in a "real" (or fake ;) ) user. This however does not compromise the tests,
        since we assume for this test that FirebaseAuth.signInWithCredential works and will successfully sign in our user once it is called with the correct parameters. This method is provided by
        Firebase and therefore outside of the scope of the tests.*/
        every { mockFirebaseAuth.currentUser } returns mockFirebaseUser

        repository.createAccountWithEmailAndPassword(
            onSuccess = mockOnSuccess,
            onErrorAction = mockOnError,
            email = "mock@example.com",
            password = "password")

        advanceUntilIdle()

        verify { mockFirebaseAuth.createUserWithEmailAndPassword(any(), any()) }
        verify { mockOnSuccess(mockFirebaseUser) }
        verify(exactly = 0) { mockOnError(any()) }
      }

  @Test
  fun testCreateAccountWithEmailAndPassword_unsuccessful() =
      runTest(timeout = 5.seconds) {
        every { mockTask.isSuccessful } returns false
        every { mockTask.exception } returns Exception("Test Error")

        repository.createAccountWithEmailAndPassword(
            onSuccess = mockOnSuccess,
            onErrorAction = mockOnError,
            email = "mock@example.com",
            password = "password")

        advanceUntilIdle()

        verify { mockFirebaseAuth.createUserWithEmailAndPassword(any(), any()) }
        verify(exactly = 0) { mockOnSuccess(any()) }
        verify { mockOnError(any<Throwable>()) }
      }

  @Test
  fun testSignInWithEmailAndPassword_successful() =
      runTest(timeout = 5.seconds) {
        every { mockTask.isSuccessful } returns true
        every { mockFirebaseAuth.currentUser } returns mockFirebaseUser

        repository.signInWithEmailAndPassword(
            onSuccess = mockOnSuccess,
            onErrorAction = mockOnError,
            email = "mock@example.com",
            password = "password")

        advanceUntilIdle()

        verify { mockFirebaseAuth.signInWithEmailAndPassword(any(), any()) }
        verify { mockOnSuccess(mockFirebaseUser) }
        verify(exactly = 0) { mockOnError(any()) }
      }

  @Test
  fun testSignInWithEmailAndPassword_unsuccessful() =
      runTest(timeout = 5.seconds) {
        every { mockTask.isSuccessful } returns false
        every { mockTask.exception } returns Exception("Test Error")

        repository.signInWithEmailAndPassword(
            onSuccess = mockOnSuccess,
            onErrorAction = mockOnError,
            email = "mock@example.com",
            password = "password")

        advanceUntilIdle()

        verify { mockFirebaseAuth.signInWithEmailAndPassword(any(), any()) }
        verify(exactly = 0) { mockOnSuccess(any()) }
        verify { mockOnError(any<Throwable>()) }
      }

  @Test
  fun testSignInWithGoogle_successful() =
      runTest(timeout = 5.seconds) {
        // Mocks a successful call to FirebaseAuth.signInWithCredential
        every { mockTask.isSuccessful } returns true
        every { mockFirebaseAuth.currentUser } returns mockFirebaseUser

        repository.signInWithGoogle(
            onSuccess = mockOnSuccess,
            onErrorAction = mockOnError,
            context = context,
            credentialManager = mockCredentialManager,
            coroutineScope = this,
            startAddAccountIntentLauncher = null)

        advanceUntilIdle()

        coVerify { mockCredentialManager.getCredential(any(), any<GetCredentialRequest>()) }
        verify { mockFirebaseAuth.signInWithCredential(mockAuthCredential) }
        verify { mockOnSuccess(mockFirebaseUser) }
        verify(exactly = 0) { mockOnError(any()) }
        verify { profileRepository.createProfile(any(), any(), any()) }
      }

  @Test
  fun testSignInWithGoogle_unsuccessful() =
      runTest(timeout = 5.seconds) {
        every { mockTask.isSuccessful } returns false
        every { mockTask.exception } returns Exception("Test Error")

        repository.signInWithGoogle(
            onSuccess = mockOnSuccess,
            onErrorAction = mockOnError,
            context = context,
            credentialManager = mockCredentialManager,
            coroutineScope = this,
            startAddAccountIntentLauncher = null)

        advanceUntilIdle()

        coVerify { mockCredentialManager.getCredential(any(), any<GetCredentialRequest>()) }
        verify { mockFirebaseAuth.signInWithCredential(mockAuthCredential) }
        verify(exactly = 0) { mockOnSuccess(any()) }
        verify { mockOnError(any<Throwable>()) }
      }

  @Test
  fun testSignInWithGoogle_Error() =
      runTest(timeout = 5.seconds) {
        val testException = GetCredentialUnknownException("Test Error")
        coEvery { mockCredentialManager.getCredential(any(), any<GetCredentialRequest>()) } throws
            testException

        repository.signInWithGoogle(
            onSuccess = mockOnSuccess,
            onErrorAction = mockOnError,
            context = context,
            credentialManager = mockCredentialManager,
            coroutineScope = this,
            startAddAccountIntentLauncher = null)

        advanceUntilIdle()

        coVerify { mockCredentialManager.getCredential(any(), any<GetCredentialRequest>()) }
        verify(exactly = 0) { mockFirebaseAuth.signInWithCredential(mockAuthCredential) }
        verify(exactly = 0) { mockOnSuccess(any()) }
        verify { mockOnError(any<GetCredentialUnknownException>()) }
      }

  @Test
  fun testSignInWithGoogle_NoCredentialException() =
      runTest(timeout = 5.seconds) {
        val noCredentialException = NoCredentialException()
        coEvery { mockCredentialManager.getCredential(any(), any<GetCredentialRequest>()) }
            .coAnswers { throw noCredentialException }

        val mockLauncher: ManagedActivityResultLauncher<Intent, ActivityResult> =
            mockk(relaxed = true)

        // Call signInWithGoogle, expecting it to handle the NoCredentialException
        repository.signInWithGoogle(
            onSuccess = mockOnSuccess,
            onErrorAction = mockOnError,
            context = context,
            credentialManager = mockCredentialManager,
            coroutineScope = this,
            startAddAccountIntentLauncher = mockLauncher)

        advanceUntilIdle()

        // Verify that getCredential() was called
        coVerify { mockCredentialManager.getCredential(any(), any<GetCredentialRequest>()) }

        // Verify that startAddAccountIntentLauncher.launch() was called
        verify { mockLauncher.launch(any()) }

        // Verify no success or error callbacks were called
        verify(exactly = 0) { mockOnSuccess(any()) }
        verify(exactly = 0) { mockOnError(any()) }
      }

  @Test
  fun testSignOut_successful() =
      runTest(timeout = 5.seconds) {
        every { mockFirebaseAuth.currentUser } returns mockFirebaseUser

        val onSuccess: () -> Unit = mockk(relaxed = true)

        repository.signOut(onSuccess)

        verify { mockFirebaseAuth.signOut() }
        verify { onSuccess() }
      }

}
