package ch.hikemate.app.model.authentication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import ch.hikemate.app.R
import ch.hikemate.app.model.profile.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel(
    private val repository: AuthRepository,
    // Auth view model takes the ProfileRepository for the creation of a Profile.
    private val profileRepository: ProfileRepository
) : ViewModel() {

  // MutableStateFlow is used for observing and updating the current user's state.
  private val _currentUser = MutableStateFlow(FirebaseAuth.getInstance().currentUser)

  // Public immutable version of _currentUser, exposed as StateFlow.
  // This allows other components to observe changes to the authentication state without modifying
  // it.
  val currentUser: StateFlow<FirebaseUser?>
    get() = _currentUser

  private val _loading = MutableStateFlow(false)
  /**
   * If an operation is in progress, the loading state will be set to true. This can be used to
   * display a loading animation.
   */
  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  private val _errorMessageId = MutableStateFlow<Int?>(null)
  /**
   * If an error occurs while performing an operation related to authentication, the resource ID of
   * an appropriate error message will be set in this state flow.
   */
  val errorMessageId: StateFlow<Int?> = _errorMessageId.asStateFlow()

  /** Checks if the user is currently logged in. */
  fun isUserLoggedIn(): Boolean {
    return _currentUser.value != null
  }

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
    _loading.value = true

    repository.signInWithGoogle(
        onSuccess = { user: FirebaseUser? ->
          profileRepository.createProfile(
              user,
              onSuccess = {
                _currentUser.value = user
                _loading.value = true
              },
              onFailure = {
                _loading.value = true
                _errorMessageId.value = R.string.an_error_occurred_while_creating_the_profile
              },
              context = context)
        },
        onErrorAction = {
          _loading.value = true
          _errorMessageId.value = it
        },
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
      name: String,
      email: String,
      password: String,
      onSuccess: () -> Unit,
      onErrorAction: (Int) -> Unit,
      context: Context
  ) {
    if (email.isEmpty() || password.isEmpty()) {
      onErrorAction(R.string.error_email_and_password_must_not_be_empty)
      return
    }

    repository.createAccountWithEmailAndPassword(
        onSuccess = { user: FirebaseUser? ->
          // This should never happen. Since the createProfile function checks whether
          // the user is null or not. So if the user is null the callback will not be called.
          user!!
              // Update the users display name since the user is created with the email address.
              .updateProfile(userProfileChangeRequest { displayName = name })
              .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                  profileRepository.createProfile(
                      user,
                      onSuccess = {
                        _currentUser.value = user
                        Log.d("AuthViewModel", "User profile created")
                        onSuccess()
                      },
                      onFailure = {
                        Log.e("AuthViewModel", "Error creating user profile", it)
                        onErrorAction(R.string.an_error_occurred_while_creating_the_profile)
                      },
                      context = context)
                } else {
                  Log.e("AuthViewModel", "Error updating user profile")
                  onErrorAction(R.string.an_error_occurred_while_updating_the_profile)
                }
              }
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
      onErrorAction: (Int) -> Unit
  ) {
    if (email.isEmpty() || password.isEmpty()) {
      onErrorAction(R.string.error_email_and_password_must_not_be_empty)
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
  fun signOut(onSuccess: () -> Unit) {
    repository.signOut(
        onSuccess = {
          _currentUser.value = null
          onSuccess()
        },
    )
  }

  /**
   * Deletes the current user's account. On successful deletion, the _currentUser is set to null.
   *
   * @param password The password of the user.
   * @param onSuccess The action to be executed on successful account deletion.
   * @param onErrorAction The action to be executed on error.
   */
  fun deleteAccount(
      password: String,
      activity: Activity,
      onSuccess: () -> Unit,
      onErrorAction: (Int) -> Unit
  ) {
    repository.deleteAccount(
        password = password,
        activity = activity,
        onSuccess = {
          _currentUser.value = null
          onSuccess()
        },
        onErrorAction = onErrorAction)
  }

  /**
   * Returns whether the current user is connect with an email provider which is the email and
   * password way of signing in to Firebase.
   */
  fun isEmailProvider(): Boolean {
    return if (_currentUser.value == null) {
      false
    } else {
      repository.isEmailProvider(_currentUser.value!!)
    }
  }
}
