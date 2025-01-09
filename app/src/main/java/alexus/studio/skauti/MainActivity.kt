package alexus.studio.skauti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import alexus.studio.skauti.ui.MainScreen
import alexus.studio.skauti.ui.theme.SkautiTheme
import alexus.studio.skauti.utils.ThemePreferences

class MainActivity : ComponentActivity() {
    private lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themePreferences = ThemePreferences(this)
        
        enableEdgeToEdge()
        setContent {
            // Načteme uložené téma při startu
            var darkTheme by remember { mutableStateOf(themePreferences.isDarkTheme()) }
            
            SkautiTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        isDarkTheme = darkTheme,
                        onThemeChanged = { newTheme -> 
                            darkTheme = newTheme
                            // Uložíme změnu tématu
                            themePreferences.setDarkTheme(newTheme)
                        }
                    )
                }
            }
        }
    }
}