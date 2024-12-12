package ch.hikemate.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import ch.hikemate.app.R
import ch.hikemate.app.utils.PermissionUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState

object LocationPermissionAlertDialog {
  const val TEST_TAG_LOCATION_PERMISSION_ALERT =
      "LocationPermissionAlertDialogLocationPermissionAlert"
  const val TEST_TAG_NO_THANKS_ALERT_BUTTON = "LocationPermissionAlertDialogNoThanksAlertButton"
  const val TEST_TAG_GRANT_ALERT_BUTTON = "LocationPermissionAlertDialogGrantAlertButton"
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionAlertDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    simpleMessage: Boolean,
    locationPermissionState: MultiplePermissionsState,
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current
) {
  if (!show) return

  AlertDialog(
      modifier = modifier.testTag(LocationPermissionAlertDialog.TEST_TAG_LOCATION_PERMISSION_ALERT),
      icon = {
        Icon(painter = painterResource(id = R.drawable.my_location), contentDescription = null)
      },
      title = { Text(text = stringResource(R.string.map_screen_location_rationale_title)) },
      text = {
        Text(
            text =
                stringResource(
                    if (simpleMessage) R.string.map_screen_location_rationale_simple
                    else R.string.map_screen_location_rationale))
      },
      onDismissRequest = onDismiss,
      confirmButton = {
        Button(
            modifier = modifier.testTag(LocationPermissionAlertDialog.TEST_TAG_GRANT_ALERT_BUTTON),
            onClick = {
              onConfirm()
              // If should show rationale is true, it is safe to launch permission requests
              if (locationPermissionState.shouldShowRationale) {
                locationPermissionState.launchMultiplePermissionRequest()
              }

              // If the user is asked for the first time, it is safe to launch permission requests
              else if (PermissionUtils.firstTimeAskingPermission(
                  context, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                PermissionUtils.setFirstTimeAskingPermission(
                    context, android.Manifest.permission.ACCESS_FINE_LOCATION, false)
                PermissionUtils.setFirstTimeAskingPermission(
                    context, android.Manifest.permission.ACCESS_COARSE_LOCATION, false)
                locationPermissionState.launchMultiplePermissionRequest()
              }

              // Otherwise, the user should be brought to the settings page
              else {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)))
              }
            }) {
              Text(text = stringResource(R.string.map_screen_location_rationale_grant_button))
            }
      },
      dismissButton = {
        Button(
            modifier =
                modifier.testTag(LocationPermissionAlertDialog.TEST_TAG_NO_THANKS_ALERT_BUTTON),
            onClick = onDismiss) {
              Text(text = stringResource(R.string.map_screen_location_rationale_cancel_button))
            }
      })
}
