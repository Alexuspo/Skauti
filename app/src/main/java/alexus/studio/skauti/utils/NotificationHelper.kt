package alexus.studio.skauti.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import alexus.studio.skauti.R

object NotificationHelper {
    private const val CHANNEL_ID = "events_channel"
    private const val NOTIFICATION_ID = 1

    fun showNotification(context: Context, title: String, message: String) {
        // Vypnuto - žádná oznámení nebudou zobrazena
        return
    }

    private fun createNotificationChannel(context: Context) {
        // Vypnuto - kanál pro oznámení nebude vytvořen
        return
    }
} 