package ch.hikemate.app.model.authentication

import androidx.annotation.StringRes
import ch.hikemate.app.R
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

enum class AuthenticationError(@StringRes val messageResourceId: Int) {
  /** The email the user entered is empty. */
  EMPTY_EMAIL(R.string.authentication_error_empty_email),

  /** The password the user entered is empty. */
  EMPTY_PASSWORD(R.string.authentication_error_empty_password),

  /** The user does not exist or has been disabled. */
  INVALID_USER(R.string.authentication_error_invalid_user),

  /** The password is invalid, wrong credentials. */
  INVALID_CREDENTIALS(R.string.authentication_error_invalid_credentials),

  /** The email address is already in use by another account. */
  USER_COLLISION(R.string.authentication_error_user_already_exists),

  /** An unknown error that does not fall in the previous categories occurred. */
  UNKNOWN(R.string.authentication_error_unknown);

  companion object {
    /**
     * Converts a Firebase error to an [AuthenticationError]. If the error is not recognized,
     * [UNKNOWN] is returned.
     */
    fun fromFirebaseError(error: Exception): AuthenticationError {
      return when (error) {
        is FirebaseAuthInvalidUserException -> INVALID_USER
        is FirebaseAuthInvalidCredentialsException -> INVALID_CREDENTIALS
        is FirebaseAuthUserCollisionException -> USER_COLLISION
        else -> UNKNOWN
      }
    }
  }
}
