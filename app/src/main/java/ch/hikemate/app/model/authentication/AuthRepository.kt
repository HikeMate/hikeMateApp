package ch.hikemate.app.model.authentication

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope

interface AuthRepository {

  /**
   * This function should be called to initiate the sign-in process using Google Sign-In.
   *
   * @param onSuccess Callback to invoke when login is successful. Passes the FirebaseUser if
   *   successful.
   * @param onErrorAction Callback to invoke when an error occurs during login. Passes the Throwable
   *   error.
   * @param context The Android Context, used for accessing resources and system services.
   * @param coroutineScope The CoroutineScope to launch the login task in a non-blocking way.
   * @param credentialManager (Optional) CredentialManager for handling credential requests. Pass
   *   explicitly when testing with mocks.
   */
  fun signInWithGoogle(
      onSuccess: (FirebaseUser?) -> Unit,
      onErrorAction: (Exception) -> Unit,
      context: Context,
      coroutineScope: CoroutineScope,
      credentialManager: CredentialManager = CredentialManager.create(context),
      startAddAccountIntentLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>?,
  )

  /**
   * This function should be called to initiate the create account process using email and password.
   *
   * @param onSuccess Callback to invoke when creating the account is successful. Passes the
   *   FirebaseUser if successful.
   * @param onErrorAction Callback to invoke when an error occurs during the account creation.
   *   Passes the Throwable error.
   * @param email The email address of the user.
   * @param password The password of the user.
   */
  fun createAccountWithEmailAndPassword(
      onSuccess: (FirebaseUser?) -> Unit,
      onErrorAction: (Exception) -> Unit,
      email: String,
      password: String,
  )

  /**
   * This function should be called to initiate the sign-in process using email and password.
   *
   * @param onSuccess Callback to invoke when login is successful. Passes the FirebaseUser if
   *   successful.
   * @param onErrorAction Callback to invoke when an error occurs during login. Passes the Throwable
   *   error.
   * @param email The email address of the user.
   * @param password The password of the user.
   */
  fun signInWithEmailAndPassword(
      onSuccess: (FirebaseUser?) -> Unit,
      onErrorAction: (Exception) -> Unit,
      email: String,
      password: String,
  )

  /**
   * Signs out the current user and invokes the success callback.
   *
   * @param onSuccess Callback to invoke after the user has successfully signed out.
   */
  fun signOut(onSuccess: () -> Unit = {})
}
