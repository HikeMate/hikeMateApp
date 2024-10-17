package ch.hikemate.app.model.authentication

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel(private val repository: FirebaseAuthRepository) : ViewModel() {

  // MutableStateFlow is used for observing and updating the current user's state.
  private val _currentUser = MutableStateFlow(FirebaseAuth.getInstance().currentUser)

  // Public immutable version of _currentUser, exposed as StateFlow.
  // This allows other components to observe changes to the authentication state without modifying
  // it.
  val currentUser: StateFlow<FirebaseUser?>
    get() = _currentUser

  /**
   * Initiates the sign-in process using Google Sign-In..
   *
   * @param coroutineScope The CoroutineScope in which this operation will be executed.
   * @param context The context is used to start the Google Sign-In process.
   */
  fun signInWithGoogle(
      coroutineScope: CoroutineScope,
      context: Context,
      onSuccess: () -> Unit,
      onError: (Exception) -> Unit
  ) {
    repository.signInWithGoogle(
        onSuccess = { user: FirebaseUser? ->
          _currentUser.value = user
          onSuccess()
        },
        onErrorAction = onError,
        context = context,
        coroutineScope = coroutineScope)
  }

  /** Signs out the current user. On successful sign-out, the _currentUser is set to null. */
  fun signOut() {
    repository.signOut(
        onSuccess = { _currentUser.value = null },
    )
  }

  // create factory
  companion object {
    val Factory: ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(FirebaseAuthRepository()) as T
          }
        }
  }
}
