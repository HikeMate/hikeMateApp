package ch.hikemate.app.model.authentication

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import ch.hikemate.app.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
      } catch (e: Exception) {
        Log.d("SignInButton", "Login error: ${e.message}")
        onErrorAction(e)
      }
    }
  }

  fun googleSignIn(context: Context): Flow<Result<AuthResult>> {
    val firebaseAuth = FirebaseAuth.getInstance()
    return callbackFlow {
      try {
        // Initialize Credential Manager
        val credentialManager: CredentialManager = CredentialManager.create(context)

        // Generate a nonce (a random number used once)
        val ranNonce: String = UUID.randomUUID().toString()
        val bytes: ByteArray = ranNonce.toByteArray()
        val md: MessageDigest = MessageDigest.getInstance("SHA-256")
        val digest: ByteArray = md.digest(bytes)
        val hashedNonce: String = digest.fold("") { str, it -> str + "%02x".format(it) }

        // Set up Google ID option
        val googleIdOption: GetGoogleIdOption =
            GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("YOUR_WEB_CLIENT_ID")
                .setNonce(hashedNonce)
                .build()

        // Request credentials
        val request: GetCredentialRequest =
            GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

        // Get the credential result
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential

        // Check if the received credential is a valid Google ID Token
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
          val authCredential =
              GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
          val authResult = firebaseAuth.signInWithCredential(authCredential).await()
          trySend(Result.success(authResult))
        } else {
          throw RuntimeException("Received an invalid credential type")
        }
      } catch (e: GetCredentialCancellationException) {
        trySend(Result.failure(Exception("Sign-in was canceled. Please try again.")))
      } catch (e: Exception) {
        trySend(Result.failure(e))
      }
      awaitClose {}
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
