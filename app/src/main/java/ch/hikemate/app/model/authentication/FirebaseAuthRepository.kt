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
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class FirebaseAuthRepository : AuthRepository {

  override fun signInWithGoogle(
      onSuccess: (FirebaseUser?) -> Unit,
      onErrorAction: (Exception) -> Unit,
      context: Context,
      coroutineScope: CoroutineScope,
      credentialManager: CredentialManager,
      startAddAccountIntentLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>?,
  ) {
    // Initialize Firebase authentication and retrieve the web client ID from resources
    val auth = FirebaseAuth.getInstance()
    val token = context.getString(R.string.default_web_client_id)

    // Configure Google ID options to request credentials from authorized accounts and the server
    // client ID
    val googleIdOption: GetGoogleIdOption =
        GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // Allow all kinds of Google accounts
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
                    "com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")!!, // Non-null assertion because the token must exist if login is successful
                null // No access token needed
                )

        // Sign in with the Firebase credential (async task)
        auth.signInWithCredential(firebaseCredential).addOnCompleteListener { task ->
          if (task.isSuccessful) {
            Log.d("SignInButton", "signInWithCredential:success")
            onSuccess(auth.currentUser)
          } else {
            Log.d("SignInButton", "signInWithCredential:failure")
            onErrorAction(task.exception ?: Exception())
          }
        }
      } catch (e: NoCredentialException) {
        Log.e("SignInButton", "No credentials found: ${e.message}")
        // If there is no Google account connected to the device, prompt the user to add one
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

  override fun createAccountWithEmailAndPassword(
      onSuccess: (FirebaseUser?) -> Unit,
      onErrorAction: (Exception) -> Unit,
      email: String,
      password: String
  ) {
    val auth = FirebaseAuth.getInstance()
    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
      if (task.isSuccessful) {
        onSuccess(auth.currentUser)
      } else {
        onErrorAction(task.exception ?: Exception())
      }
    }
  }

  override fun signInWithEmailAndPassword(
      email: String,
      password: String,
      onSuccess: (FirebaseUser?) -> Unit,
      onErrorAction: (Exception) -> Unit,
  ) {
    val auth = FirebaseAuth.getInstance()
    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
      if (task.isSuccessful) {
        onSuccess(auth.currentUser)
      } else {
        onErrorAction(task.exception ?: Exception())
      }
    }
  }

  override fun signOut(onSuccess: () -> Unit) {
    Firebase.auth.signOut()
    onSuccess()
  }
}
