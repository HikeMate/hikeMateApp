package ch.hikemate.app.ui.components

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.res.stringResource

/**
 * A component that handles the state of an asynchronous operation and displays the appropriate
 * content based on the state. If an error occurred, an error message is displayed. If the value is
 * null, a loading animation is displayed. Otherwise, the content is displayed.
 *
 * @param errorMessageIdState The state that holds the error message id
 * @param actionContentDescriptionStringId The string id for the content description of the action
 * @param actionOnErrorAction The action to perform when the error action is clicked
 * @param valueState The state that holds the value to display
 * @param content The content to display
 */
@Composable
fun <T> AsyncStateHandler(
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

@Composable
fun AsyncStateHandler(
    errorMessageIdState: State<Int?>,
    actionContentDescriptionStringId: Int,
    actionOnErrorAction: () -> Unit,
    loadingState: State<Boolean>,
    content: @Composable () -> Unit
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
    loadingState.value -> {
      CenteredLoadingAnimation()
    }
    else -> {
      // Display the content if no error occurred
      content()
    }
  }
}
