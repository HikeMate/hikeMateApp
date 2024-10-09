package ch.hikemate.app

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialUnknownException
import ch.hikemate.app.ui.SignInButton
import ch.hikemate.app.ui.SignOutButton
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.initialize
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AuthTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockCredentialManager: CredentialManager
  private lateinit var mockFirebaseAuth: FirebaseAuth
  private lateinit var mockFirebaseUser: FirebaseUser
  private lateinit var mockAuthCredential: AuthCredential
  private lateinit var mockCredentialResponse: GetCredentialResponse

  @Before
  fun setUp() {

    mockCredentialManager = mockk()
    mockCredentialResponse = mockk()
    mockFirebaseAuth = mockk()
    mockFirebaseUser = mockk()
    mockAuthCredential = mockk()

    Firebase.initialize(mockk<Context>())
    mockkStatic(Firebase::class)
    mockkStatic(FirebaseAuth::class)

    every { Firebase.auth } returns mockFirebaseAuth
    every { mockFirebaseAuth.signInWithCredential(any<AuthCredential>()) } returns
        Tasks.forResult(mockk<AuthResult>())
    every { mockCredentialResponse.credential.data } returns
        mockk { every { getString(any()) } returns "mocked_id_token" }
    coEvery { mockCredentialManager.getCredential(any(), any<GetCredentialRequest>()) } returns
        mockCredentialResponse
  }

  @Test
  fun testSignInButton_whenUserIsNull() {
    every { mockFirebaseAuth.currentUser } returns
        null andThen
        mockFirebaseUser // After sign-in, the user becomes non-null

    val onAuthComplete: () -> Unit = mockk(relaxed = true)

    composeTestRule.setContent {
      SignInButton(
          onAuthComplete = onAuthComplete,
          onErrorAction = { fail() },
          credentialManager = mockCredentialManager)
    }

    runBlocking {
      composeTestRule.onNodeWithTag("signInButton").assertExists().performClick()
      composeTestRule.waitForIdle() // Wait for composable to finish processing
    }

    verify { mockFirebaseAuth.signInWithCredential(any<AuthCredential>()) }
    coVerify { mockCredentialManager.getCredential(any(), any<GetCredentialRequest>()) }
    verify { onAuthComplete() }
  }

  @Test
  fun testSignInButton_whenUserIsNotNull() {
    every { mockFirebaseAuth.currentUser } returns mockFirebaseUser

    composeTestRule.setContent {
      SignInButton(
          onAuthComplete = {},
          onErrorAction = { fail() },
          credentialManager = mockCredentialManager)
    }

    composeTestRule.onNodeWithTag("signInButton").assertDoesNotExist()
  }

  @Test
  fun testSignInButton_onErrorActionCalledOnFailure() {
    every { mockFirebaseAuth.currentUser } returns null

    coEvery { mockCredentialManager.getCredential(any(), any<GetCredentialRequest>()) } throws
        GetCredentialUnknownException("mocked error")

    val onErrorAction: (Throwable) -> Unit = mockk(relaxed = true)

    composeTestRule.setContent {
      SignInButton(
          onAuthComplete = { fail("onAuthComplete should not be called on error") },
          onErrorAction = onErrorAction,
          credentialManager = mockCredentialManager)
    }

    runBlocking {
      composeTestRule.onNodeWithTag("signInButton").assertExists().performClick()
      composeTestRule.waitForIdle() // Wait for composable to finish processing
    }

    // Verify that onErrorAction was called with the exception
    coVerify { onErrorAction(any()) }
  }

  @Test
  fun testSignOutButton_whenUserIsNotNull() {
    every { mockFirebaseAuth.currentUser } returns mockFirebaseUser andThen null
    every { mockFirebaseAuth.signOut() } just Runs

    val onSignOutComplete: () -> Unit = mockk(relaxed = true)

    composeTestRule.setContent { SignOutButton(onSignOutComplete) }

    runBlocking {
      composeTestRule.onNodeWithText("Sign out").assertExists().performClick()
      composeTestRule.waitForIdle()
    }

    verify { mockFirebaseAuth.signOut() }
    verify { onSignOutComplete() }
  }

  @Test
  fun testSignOutButton_whenUserIsNull() {
    every { mockFirebaseAuth.currentUser } returns null

    composeTestRule.setContent { SignOutButton {} }

    composeTestRule.onNodeWithText("Sign out").assertDoesNotExist()
  }
}
