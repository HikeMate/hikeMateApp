package ch.hikemate.app.utils

import android.content.Context
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState

/**
 * Utility class for handling permissions.
 *
 * Strongly inspired (if not copy-pasted) from this Stack Overflow answer:
 * https://stackoverflow.com/a/40639430
 */
object PermissionUtils {
  /** The name of the shared preferences file that stores persistent data about permissions. */
  private const val PERMISSIONS_SHARED_PREFERENCES = "permissions"

  /**
   * Sets whether the user has been asked for a permission before.
   *
   * @param context The context to use.
   * @param permission The permission to set the first time asking status for.
   * @param isFirstTime Whether the user has been asked for the permission before.
   * @see firstTimeAskingPermission
   */
  fun setFirstTimeAskingPermission(context: Context, permission: String, isFirstTime: Boolean) {
    val sharedPreferences =
        context.getSharedPreferences(PERMISSIONS_SHARED_PREFERENCES, Context.MODE_PRIVATE)
    sharedPreferences.edit().putBoolean(permission, isFirstTime).apply()
  }

  /**
   * Returns whether the user has been asked for a permission before.
   *
   * Default value is true, meaning the user has not been asked for the permission before. This is
   * because the shared preferences entry might never have been set if this is the first time.
   *
   * @param context The context to use.
   * @param permission The permission to check the first time asking status for.
   * @return Whether the user has been asked for the permission before.
   * @see setFirstTimeAskingPermission
   */
  fun firstTimeAskingPermission(context: Context, permission: String): Boolean {
    return context
        .getSharedPreferences(PERMISSIONS_SHARED_PREFERENCES, Context.MODE_PRIVATE)
        .getBoolean(permission, true)
  }

  @OptIn(ExperimentalPermissionsApi::class)
  fun anyPermissionGranted(state: MultiplePermissionsState): Boolean {
    return state.revokedPermissions.size != state.permissions.size
  }
}
