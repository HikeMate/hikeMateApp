package ch.hikemate.app.model.authentication

import android.content.Context
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope

interface AuthRepository {

  /**
   * Sign in with Google using Firebase Authentication.
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
  )

  /**
   * Signs out the current user from Firebase and invokes the success callback.
   *
   * @param onSuccess Callback to invoke after the user has successfully signed out.
   */
  fun signOut(onSuccess: () -> Unit = {})
}
