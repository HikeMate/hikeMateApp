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
import ch.hikemate.app.model.profile.ProfileRepositoryFirestore
import ch.hikemate.app.model.route.saved.SavedHikesRepositoryFirestore
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class FirebaseAuthRepository : AuthRepository {

  override fun signInWithGoogle(
      onSuccess: (FirebaseUser?) -> Unit,
      onErrorAction: (Int) -> Unit,
      context: Context,
      coroutineScope: CoroutineScope,
      credentialManager: CredentialManager,
      startAddAccountIntentLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>?,
  ) {
    // Initialize Firebase authentication and retrieve the web client ID from resources
    val auth = FirebaseAuth.getInstance()

    getGoogleAuthCredential(
        context,
        coroutineScope,
        credentialManager,
        onSuccess = { firebaseCredential ->
          // Sign in with the Firebase credential (async task)
          auth.signInWithCredential(firebaseCredential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
              Log.d("FirebaseAuthRepository", "signInWithCredential:success")
              onSuccess(auth.currentUser)
            } else {
              Log.d("FirebaseAuthRepository", "signInWithCredential:failure", task.exception)
              onErrorAction(R.string.error_occurred_while_signing_in_with_google)
            }
          }
        },
        onFailure = { e ->
          if (e is NoCredentialException) {
            Log.e("SignInButton", "No credentials found: ${e.message}")
            // If there is no Google account connected to the device, prompt the user to add one
            startAddAccountIntentLauncher?.launch(getAddGoogleAccountIntent())
          } else {
            Log.d("SignInButton", "Login error: ${e.message}")
            onErrorAction(R.string.error_occurred_while_signing_in_with_google)
          }
        })
  }

  private fun getAddGoogleAccountIntent(): Intent {
    val intent = Intent(Settings.ACTION_ADD_ACCOUNT)
    intent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
    return intent
  }

  override fun createAccountWithEmailAndPassword(
      onSuccess: (FirebaseUser?) -> Unit,
      onErrorAction: (Int) -> Unit,
      email: String,
      password: String
  ) {
    val auth = FirebaseAuth.getInstance()
    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
      if (task.isSuccessful) {
        Log.d("FirebaseAuthRepository", "createAccountWithEmailAndPassword:success")
        onSuccess(auth.currentUser)
      } else {
        Log.e("FirebaseAuthRepository", "createAccountWithEmailAndPassword:failure", task.exception)
        onErrorAction(R.string.error_occurred_while_creating_account)
      }
    }
  }

  override fun signInWithEmailAndPassword(
      email: String,
      password: String,
      onSuccess: (FirebaseUser?) -> Unit,
      onErrorAction: (Int) -> Unit,
  ) {
    val auth = FirebaseAuth.getInstance()
    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
      if (task.isSuccessful) {
        Log.d("FirebaseAuthRepository", "signInWithEmailAndPassword:success")
        onSuccess(auth.currentUser)
      } else {
        Log.e("FirebaseAuthRepository", "signInWithEmailAndPassword:failure", task.exception)
        onErrorAction(R.string.error_occurred_while_signing_in_with_email)
      }
    }
  }

  override fun signOut(onSuccess: () -> Unit) {
    Firebase.auth.signOut()
    onSuccess()
  }

  override fun deleteAccount(
      password: String,
      context: Context,
      coroutineScope: CoroutineScope,
      onSuccess: () -> Unit,
      onErrorAction: (Int) -> Unit
  ) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user != null && user.email != null) {
      val email = user.email
      Log.d("DeleteAccount", "User email: '$email', password: '$password'")

      // Re-authenticate the user
      reauthenticate(
          user,
          password,
          context,
          coroutineScope,
          {
            Log.d("DeleteAccount", "User re-authenticated")

            // Delete the user profile and saved hikes
            val deleteProfileTask =
                FirebaseFirestore.getInstance()
                    .collection(ProfileRepositoryFirestore.PROFILES_COLLECTION)
                    .document(user.uid)
                    .delete()

            val deleteSavedHikesTask =
                FirebaseFirestore.getInstance()
                    .collection(SavedHikesRepositoryFirestore.SAVED_HIKES_COLLECTION)
                    .document(user.uid)
                    .delete()

            Tasks.whenAll(deleteProfileTask, deleteSavedHikesTask).addOnCompleteListener {
                deleteDataTask ->
              if (deleteDataTask.isSuccessful) {
                // Both deletions were successful
                // Delete the user account
                user.delete().addOnCompleteListener { task ->
                  if (task.isSuccessful) {
                    Log.d("DeleteAccount", "User account deleted")
                    onSuccess()
                  } else {
                    onErrorAction(R.string.error_deleting_the_user)
                  }
                }
              } else {
                onErrorAction(R.string.error_deleting_user_profile_and_saved_hikes)
              }
            }
          },
          { onErrorAction(R.string.error_re_authenticating_the_user) })
    } else {
      onErrorAction(R.string.no_user_is_currently_signed_in)
    }
  }

  override fun isEmailProvider(user: FirebaseUser): Boolean {
    return user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
  }

  /**
   * Re-authenticate the user with a given passwords. If the user is signed in with an email
   * provider, re-authenticate with email and password. If the user is signed in with Google, use
   * Google Sign-In to re-authenticate.
   *
   * @param user The user to re-authenticate.
   * @param password The password to use for re-authentication.
   * @param context The context to use for Google Sign-In.
   * @param coroutineScope The coroutine scope to use for Google Sign-In.
   * @param onSuccess The action to perform if re-authentication is successful.
   * @param onFailure The action to perform if re-authentication fails.
   */
  private fun reauthenticate(
      user: FirebaseUser,
      password: String,
      context: Context,
      coroutineScope: CoroutineScope,
      onSuccess: () -> Unit,
      onFailure: () -> Unit
  ) {
    val isEmail = isEmailProvider(user)

    if (isEmail) {
      reauthenticateWithEmailAndPassword(user, password, onSuccess, onFailure)
    } else {
      reauthenticateWithGoogleSignIn(user, context, coroutineScope, onSuccess, onFailure)
    }
  }

  /**
   * Re-authenticate the user with email and password.
   *
   * @param user The user to re-authenticate.
   * @param password The password to use for re-authentication.
   * @param onSuccess The action to perform if re-authentication is successful.
   * @param onFailure The action to perform if re-authentication fails.
   */
  private fun reauthenticateWithEmailAndPassword(
      user: FirebaseUser,
      password: String,
      onSuccess: () -> Unit,
      onFailure: () -> Unit
  ) {
    val email = user.email ?: return onFailure()

    val credential: AuthCredential = EmailAuthProvider.getCredential(email, password)
    user.reauthenticate(credential).addOnCompleteListener { task ->
      if (task.isSuccessful) {
        Log.d("FirebaseAuthRepository", "reauthenticate(email):success")
        onSuccess()
      } else {
        Log.d("FirebaseAuthRepository", "reauthenticate(email):failure", task.exception)
        onFailure()
      }
    }
  }

  /**
   * Re-authenticate the user with Google Sign-In.
   *
   * @param user The user to re-authenticate.
   * @param context The context to use for Google Sign-In.
   * @param coroutineScope The coroutine scope to use for Google Sign-In.
   * @param onSuccess The action to perform if re-authentication is successful.
   * @param onFailure The action to perform if re-authentication fails.
   */
  private fun reauthenticateWithGoogleSignIn(
      user: FirebaseUser,
      context: Context,
      coroutineScope: CoroutineScope,
      onSuccess: () -> Unit,
      onFailure: () -> Unit
  ) {
    getGoogleAuthCredential(
        context,
        coroutineScope,
        onSuccess = { firebaseCredential ->
          user.reauthenticate(firebaseCredential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
              Log.d("FirebaseAuthRepository", "reauthenticate(google):success")
              onSuccess()
            } else {
              Log.d("FirebaseAuthRepository", "reauthenticate(google):failure", task.exception)
              onFailure()
            }
          }
        },
        onFailure = {
          Log.d("GetGoogleCredential", "Error getting Google credential: ${it.message}")
          onFailure()
        })
  }

  /**
   * Get a Google credential for Firebase authentication. Useful for Google Sign-In and
   * re-authentication.
   *
   * @param context The context to use for Google Sign-In.
   * @param coroutineScope The coroutine scope to use for Google Sign-In.
   * @param credentialManager The credential manager to use for Google Sign-In.
   * @param onSuccess The action to perform if the credential is successfully retrieved.
   * @param onFailure The action to perform if the credential retrieval fails.
   */
  private fun getGoogleAuthCredential(
      context: Context,
      coroutineScope: CoroutineScope,
      credentialManager: CredentialManager = CredentialManager.create(context),
      onSuccess: (AuthCredential) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
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

        onSuccess(firebaseCredential)
      } catch (e: Exception) {
        onFailure(e)
      }
    }
  }
}
