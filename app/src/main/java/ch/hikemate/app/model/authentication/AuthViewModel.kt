package ch.hikemate.app.model.authentication

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel(private val repository: FirebaseAuthRepository) {

  // MutableStateFlow is used for observing and updating the current user's state.
  private val _currentUser = MutableStateFlow(FirebaseAuth.getInstance().currentUser)

  // Public immutable version of _currentUser, exposed as StateFlow.
  // This allows other components to observe changes to the authentication state without modifying it.
  val currentUser: StateFlow<FirebaseUser?>
    get() = _currentUser

  /**
   * Initiates the sign-in process using Google Sign-In.*
   * Note: This function only changes the state of currentUser, it has no return or onSuccess invocation.
   *
   * @param coroutineScope The CoroutineScope in which this operation will be executed.
   * @param context The context is used to start the Google Sign-In process.
   */
  fun signInWithGoogle(coroutineScope: CoroutineScope, context: Context) {
    repository.signInWithGoogle(
        onSuccess = { user: FirebaseUser? -> _currentUser.value = user },
        onErrorAction = {},
        context = context,
        coroutineScope = coroutineScope)
  }

  /**
   * Signs out the current user.
   * On successful sign-out, the _currentUser is set to null.
   */
  fun signOut() {
    repository.signOut(
        onSuccess = { _currentUser.value = null },
    )
  }
}
