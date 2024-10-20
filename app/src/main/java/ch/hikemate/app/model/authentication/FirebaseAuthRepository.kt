package ch.hikemate.app.model.authentication

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import ch.hikemate.app.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
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
      startAddAccountIntentLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>?,
  ) {
    // Initialize Firebase authentication and retrieve the web client ID from resources
    val auth = FirebaseAuth.getInstance()
    val token = context.getString(R.string.default_web_client_id)

    // Configure Google ID options to request credentials from authorized accounts and the server
    // client ID
    val googleIdOption: GetSignInWithGoogleOption = GetSignInWithGoogleOption.Builder(token).build()

    // Build the credential request with the Google ID option
    val request: GetCredentialRequest =
        GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    // Launch a coroutine for the login process to avoid blocking the main thread
    coroutineScope.launch {
      try {
        Log.d("SignInButton", "Trying to get credential")
        // Request credentials from the credential manager
        val result =
            credentialManager.getCredential(
                request = request, // Send the request we built
                context = context // Provide the context for the request
                )

        Log.d("SignInButton", "${result}")

        // Extract the ID token from the result and create a Firebase credential
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
        val firebaseCredential =
            GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

        Log.d("SignInButton", "$firebaseCredential")

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
      } catch (e: NoCredentialException) {
        startAddAccountIntentLauncher?.launch(getAddGoogleAccountIntent())
      } catch (e: Exception) {
        Log.d("SignInButton", "Login error: ${e.message}")
        onErrorAction(e)
      }
    }
  }

  private fun getAddGoogleAccountIntent(): Intent {
    val intent = Intent(Settings.ACTION_ADD_ACCOUNT)
    intent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
    return intent
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
