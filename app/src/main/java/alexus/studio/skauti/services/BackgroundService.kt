package alexus.studio.skauti.services

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.*
import alexus.studio.skauti.MainActivity
import alexus.studio.skauti.R
import alexus.studio.skauti.utils.NotificationHelper

class BackgroundService : Service() {
    private var database: FirebaseDatabase? = null
    private var eventsListener: ChildEventListener? = null
    private val lastKnownEvents = mutableMapOf<String, Event>()

    companion object {
        private const val DATABASE_URL = "https://skauti-app-default-rtdb.europe-west1.firebasedatabase.app"
        private const val FOREGROUND_NOTIFICATION_ID = 9999
        private const val FOREGROUND_CHANNEL_ID = "foreground_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createForegroundNotificationChannel()
        startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification())
        setupFirebaseListener()
    }

    private fun createForegroundNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Služba na pozadí"
            val descriptionText = "Služba pro kontrolu nových událostí"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(FOREGROUND_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle("Skauti")
            .setContentText("Kontroluji nové události")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BackgroundService", "Služba spuštěna")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupFirebaseListener() {
        try {
            database = FirebaseDatabase.getInstance(DATABASE_URL)
            database?.setPersistenceEnabled(true)
            
            eventsListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val event = snapshot.getValue(Event::class.java)
                    event?.let {
                        if (!lastKnownEvents.containsKey(snapshot.key)) {
                            lastKnownEvents[snapshot.key ?: ""] = it
                            if (it.registrationEnabled && !it.registrationLink.isNullOrEmpty()) {
                                NotificationHelper.showNotification(
                                    context = applicationContext,
                                    title = "Nová možnost přihlášení",
                                    message = "Byla přidána možnost přihlášení na událost: ${it.name}"
                                )
                            }
                        }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val event = snapshot.getValue(Event::class.java)
                    event?.let {
                        val oldEvent = lastKnownEvents[snapshot.key]
                        // Commenting out the notification logic
                        /*
                        if (oldEvent?.registrationLink != it.registrationLink && 
                            it.registrationEnabled && 
                            !it.registrationLink.isNullOrEmpty()
                        ) {
                            NotificationHelper.showNotification(
                                context = applicationContext,
                                title = "Aktualizace přihlašování",
                                message = "Byla přidána možnost přihlášení na událost: ${it.name}"
                            )
                        }
                        */
                        lastKnownEvents[snapshot.key ?: ""] = it
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    lastKnownEvents.remove(snapshot.key)
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Chyba při sledování událostí: ${error.message}")
                }
            }

            database?.reference?.child("events")?.addChildEventListener(eventsListener!!)
            Log.d("BackgroundService", "Firebase listener nastaven")
            
        } catch (e: Exception) {
            Log.e("Firebase", "Chyba při inicializaci Firebase: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        eventsListener?.let { listener ->
            database?.reference?.child("events")?.removeEventListener(listener)
        }
        // Restartujeme službu pokud byla ukončena
        val intent = Intent(applicationContext, BackgroundService::class.java)
        startService(intent)
    }
}

data class Event(
    val name: String = "",
    val date: String = "",
    val participants: String = "",
    val eventDate: String = "",
    val registrationLink: String? = null,
    val registrationEnabled: Boolean = false
) 