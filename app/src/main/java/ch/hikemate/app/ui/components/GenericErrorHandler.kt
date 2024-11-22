package ch.hikemate.app.ui.components

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.res.stringResource

@Composable
fun <T> GenericErrorHandler(
    errorMessageIdState: State<Int?>,
    actionContentDescriptionStringId: Int,
    actionOnErrorAction: () -> Unit,
    valueState: State<T?>,
    content: @Composable (T) -> Unit
) {
  when {
    errorMessageIdState.value != null -> {
      Log.e("Error handler", "Error occurred: ${stringResource(id = errorMessageIdState.value!!)}")
      // Display an error message if an error occurred
      CenteredErrorAction(
          errorMessageId = errorMessageIdState.value!!,
          actionIcon = Icons.Outlined.Home,
          actionContentDescriptionStringId = actionContentDescriptionStringId,
          onAction = actionOnErrorAction)
    }
    valueState.value == null -> {
      CenteredLoadingAnimation()
    }
    else -> {
      // Display the content if no error occurred
      content(valueState.value!!)
    }
  }
}
