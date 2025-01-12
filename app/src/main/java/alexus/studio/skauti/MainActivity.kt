package alexus.studio.skauti

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import alexus.studio.skauti.services.BackgroundService
import alexus.studio.skauti.ui.MainScreen
import alexus.studio.skauti.ui.theme.SkautiTheme
import alexus.studio.skauti.utils.ThemePreferences

class MainActivity : ComponentActivity() {
    private lateinit var themePreferences: ThemePreferences
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // Pokud uživatel zamítl povolení, nabídneme mu přechod do nastavení
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themePreferences = ThemePreferences(this)

        // Kontrola a žádost o povolení oznámení
        checkNotificationPermission()

        // Spuštění služby na pozadí
        startBackgroundService()

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

    private fun startBackgroundService() {
        val serviceIntent = Intent(this, BackgroundService::class.java)
        startService(serviceIntent)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Služba bude pokračovat v běhu i po zavření aplikace
    }
}