package ch.hikemate.app.model.authentication

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import ch.hikemate.app.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class FirebaseAuthRepository {

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
  ) {

    try {

      // Initialize Firebase authentication and retrieve the web client ID from resources
      val auth = Firebase.auth
      val token = context.getString(R.string.default_web_client_id)

      // Configure Google ID options to request credentials from authorized accounts and the server
      // client ID
      val googleIdOption: GetGoogleIdOption =
        GetGoogleIdOption.Builder()
          .setFilterByAuthorizedAccounts(
            true
          ) // Only allow accounts already signed in on the device
          .setServerClientId(token) // Server client ID for OAuth
          .setAutoSelectEnabled(true) // Auto-select if only one account is available
          .build()

      // Build the credential request with the Google ID option
      val request: GetCredentialRequest =
        GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

      // Launch a coroutine for the login process to avoid blocking the main thread
      coroutineScope.launch {
        try {
          // Request credentials from the credential manager
          val result =
            credentialManager.getCredential(
              request = request, // Send the request we built
              context = context // Provide the context for the request
            )

          // Extract the ID token from the result and create a Firebase credential
          val firebaseCredential =
            GoogleAuthProvider.getCredential(
              result.credential.data.getString(
                "com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN"
              )!!, // Non-null assertion because the token must exist if login is successful
              null // No access token needed
            )

          // Sign in with the Firebase credential (async task)
          auth.signInWithCredential(firebaseCredential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
              Log.d("SignInButton", "signInWithCredential:success")
              onSuccess(auth.currentUser)
            } else {
              Log.d("SignInButton", "signInWithCredential:failure")
              onErrorAction(task.exception ?: Exception("Unknown error"))
            }
          }
        } catch (e: Exception) {
          Log.d("SignInButton", "Login error: ${e.message}")
          onErrorAction(e)
        }
      }
    } catch (e: Exception) {
      Log.d("SignInButton", "Login error: ${e.message}")
      onErrorAction(e)
    }
  }

  /**
   * Signs out the current user from Firebase and invokes the success callback.
   *
   * @param onSuccess Callback to invoke after the user has successfully signed out.
   */
  fun signOut(onSuccess: () -> Unit) {
    Firebase.auth.signOut()
    onSuccess()
  }
}
