package ch.hikemate.app.model.authentication

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

  // MutableStateFlow is used for observing and updating the current user's state.
  private val _currentUser = MutableStateFlow(FirebaseAuth.getInstance().currentUser)

  // Public immutable version of _currentUser, exposed as StateFlow.
  // This allows other components to observe changes to the authentication state without modifying
  // it.
  val currentUser: StateFlow<FirebaseUser?>
    get() = _currentUser

  /**
   * Initiates the sign-in process using Google Sign-In.* Note: This function only changes the state
   * of currentUser, it has no return or onSuccess invocation.
   *
   * @param coroutineScope The CoroutineScope in which this operation will be executed.
   * @param context The context is used to start the Google Sign-In process.
   */
  fun signInWithGoogle(
      coroutineScope: CoroutineScope,
      context: Context,
      startAddAccountIntentLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>?,
  ) {

    repository.signInWithGoogle(
        onSuccess = { user: FirebaseUser? -> _currentUser.value = user },
        onErrorAction = {},
        context = context,
        coroutineScope = coroutineScope,
        startAddAccountIntentLauncher = startAddAccountIntentLauncher,
    )
  }

  /**
   * Initiates the account creation process using email and password.
   *
   * @param email The email address of the user.
   * @param password The password of the user.
   */
  fun createAccountWithEmailAndPassword(
      email: String,
      password: String,
      onSuccess: () -> Unit,
      onErrorAction: (Exception) -> Unit
  ) {
    if (email.isEmpty() || password.isEmpty()) {
      onErrorAction(Exception("Email and password must not be empty"))
      return
    }
    repository.createAccountWithEmailAndPassword(
        onSuccess = { user: FirebaseUser? ->
          _currentUser.value = user
          onSuccess()
        },
        onErrorAction = onErrorAction,
        email = email,
        password = password,
    )
  }

  /**
   * Initiates the sign-in process using email and password.
   *
   * @param email The email address of the user.
   * @param password The password of the user.
   */
  fun signInWithEmailAndPassword(
      email: String,
      password: String,
      onSuccess: () -> Unit,
      onErrorAction: (Exception) -> Unit
  ) {
    if (email.isEmpty() || password.isEmpty()) {
      onErrorAction(Exception("Email and password must not be empty"))
      return
    }

    repository.signInWithEmailAndPassword(
        onSuccess = { user: FirebaseUser? ->
          _currentUser.value = user
          onSuccess()
        },
        onErrorAction = onErrorAction,
        email = email,
        password = password,
    )
  }

  /** Signs out the current user. On successful sign-out, the _currentUser is set to null. */
  fun signOut() {
    repository.signOut(
        onSuccess = { _currentUser.value = null },
    )
  }
}
