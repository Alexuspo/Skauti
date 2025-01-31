package alexus.studio.skauti

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import alexus.studio.skauti.R

class SkautiApplication : Application() {
    companion object {
        private const val DATABASE_URL = "https://skauti-app-default-rtdb.europe-west1.firebasedatabase.app"
        private const val CHANNEL_ID = "events_channel"
        private const val NOTIFICATION_ID = 1
    }

    private var lastKnownEvents = mutableMapOf<String, Event>()

    override fun onCreate() {
        super.onCreate()
        
        try {
            FirebaseApp.initializeApp(this)
            Log.d("Firebase", "Firebase inicializován")
            
            val database = FirebaseDatabase.getInstance(DATABASE_URL)
            setupEventsListener(database)
            
            // Test připojení
            val connectedRef = database.getReference(".info/connected")
            connectedRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    if (connected) {
                        Log.d("Firebase", "Připojeno k Firebase")
                    } else {
                        Log.d("Firebase", "Odpojeno od Firebase")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Chyba připojení: ${error.message}")
                }
            })
            
        } catch (e: Exception) {
            Log.e("Firebase", "Chyba při inicializaci Firebase: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun setupEventsListener(database: FirebaseDatabase) {
        database.reference.child("events").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val event = snapshot.getValue(Event::class.java)
                event?.let {
                    if (!lastKnownEvents.containsKey(snapshot.key)) {
                        lastKnownEvents[snapshot.key ?: ""] = it
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val event = snapshot.getValue(Event::class.java)
                event?.let {
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
        })
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