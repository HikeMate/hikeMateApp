package ch.hikemate.app.ui

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import ch.hikemate.app.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SignInButton(
    onAuthComplete: () -> Unit,
    onErrorAction: (GetCredentialException) -> Unit
) {

    var user by remember { mutableStateOf(Firebase.auth.currentUser) }
    val token = stringResource(R.string.default_web_client_id)
    val context = LocalContext.current
    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    if (user == null) {
        Button(
            modifier = Modifier.testTag("signInButton"),
            onClick = {

                val credentialManager = CredentialManager.create(context)
                val auth = Firebase.auth
                val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true)
                    .setServerClientId(token)
                    .setAutoSelectEnabled(true)
                    .build()
                val request: androidx.credentials.GetCredentialRequest =
                    androidx.credentials.GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                coroutineScope.launch {
                    try {
                        val result = credentialManager.getCredential(
                            request = request,
                            context = context,
                        )

                        val firebaseCredential = GoogleAuthProvider.getCredential(
                            result.credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")!!,
                            null
                        )

                        auth.signInWithCredential(firebaseCredential)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d("SignInButton", "signInWithCredential:success")
                                    user = auth.currentUser
                                } else {
                                    Log.w(
                                        "SignInButton",
                                        "signInWithCredential:failure",
                                        task.exception
                                    )
                                }
                            }
                    } catch (e: GetCredentialException) {
                        onErrorAction(e)
                        Log.d(
                            "SignInButton", "Login error: ${e.message}"
                        )
                    }
                }
            }) { Text("Sign in with Google") }
    } else {
        onAuthComplete()
    }
}



@Composable
fun SignOutButton(
    onSignOutComplete: () -> Unit
) {

    var user by remember { mutableStateOf(Firebase.auth.currentUser) }

    if (user != null) {
        Button(
            onClick = {
                // Call Firebase sign out
                Firebase.auth.signOut()
                user = null // Update the state to reflect the logout

            },
            modifier = Modifier
                .testTag("signOutButton")
        ) {
            Text("Sign out")
        }
    } else {
        onSignOutComplete()
    }
}