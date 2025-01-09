package alexus.studio.skauti

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SkautiApplication : Application() {
    companion object {
        private const val DATABASE_URL = "https://skauti-app-default-rtdb.europe-west1.firebasedatabase.app"
    }

    override fun onCreate() {
        super.onCreate()
        
        try {
            // Inicializace Firebase
            FirebaseApp.initializeApp(this)
            Log.d("Firebase", "Firebase inicializován")
            
            // Konfigurace Firebase Database s explicitní URL
            val database = FirebaseDatabase.getInstance(DATABASE_URL)
            
            // Test připojení
            val connectedRef = database.getReference(".info/connected")
            connectedRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    if (connected) {
                        Log.d("Firebase", "Připojeno k Firebase")
                        // Test zápisu po úspěšném připojení
                        testDatabaseWrite(database)
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
    
    private fun testDatabaseWrite(database: FirebaseDatabase) {
        database.reference.child("test").setValue("test_${System.currentTimeMillis()}")
            .addOnSuccessListener {
                Log.d("Firebase", "Test zápisu úspěšný")
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Test zápisu selhal: ${e.message}")
            }
    }
} 