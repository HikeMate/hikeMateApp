package ch.hikemate.app.model.authentication

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel(private val repository: FirebaseAuthRepository) {

  private val _currentUser = MutableStateFlow(FirebaseAuth.getInstance().currentUser)
  val currentUser: StateFlow<FirebaseUser?> get() = _currentUser

  /**
   *
   */
  fun signInWithGoogle(
    coroutineScope: CoroutineScope,
    context: Context
  ) {
    repository.signInWithGoogle(
      onSuccess = { user: FirebaseUser? -> _currentUser.value = user },
      onErrorAction = { },
      context = context,
      coroutineScope = coroutineScope
    )
  }

  fun signOut() {
    repository.signOut(
      onSuccess = { _currentUser.value = null },
    )
  }
}
