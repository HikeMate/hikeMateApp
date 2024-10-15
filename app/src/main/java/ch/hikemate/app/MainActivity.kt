package ch.hikemate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import ch.hikemate.app.ui.auth.SignInScreen
import ch.hikemate.app.ui.theme.HikeMateTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { HikeMateTheme { Surface(modifier = Modifier.fillMaxSize()) { SignInScreen() } } }
  }
}
