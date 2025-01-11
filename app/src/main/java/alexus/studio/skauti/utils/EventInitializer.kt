package alexus.studio.skauti.utils

import com.google.firebase.database.FirebaseDatabase
import alexus.studio.skauti.ui.Event
import alexus.studio.skauti.ui.AppInfo

object EventInitializer {
    private const val DATABASE_URL = "https://skauti-app-default-rtdb.europe-west1.firebasedatabase.app"

    fun initializeAppInfo() {
        val database = FirebaseDatabase.getInstance(DATABASE_URL)
        val appInfoRef = database.getReference("appInfo")
        
        val appInfo = mapOf(
            "version" to "2.1.0",
            "author" to "Made by: Alexus"
        )
        
        appInfoRef.setValue(appInfo)
            .addOnSuccessListener {
                println("Informace o aplikaci byly úspěšně inicializovány")
            }
            .addOnFailureListener { e ->
                println("Chyba při inicializaci informací o aplikaci: ${e.message}")
            }
    }

    fun initializeEvents() {
        val database = FirebaseDatabase.getInstance(DATABASE_URL)
        val eventsRef = database.getReference("events")
        
        val events = listOf(
            Event(
                name = "Fénix víkend",
                date = "6.9.2024",
                participants = "Fénix vedoucí",
                eventDate = "2024-09-06",
                registrationLink = ""
            ),
            Event(
                name = "Pardské přespání",
                date = "6.9.2024 - 7.9.2024",
                participants = "Pardi vedoucí",
                eventDate = "2024-09-06",
                registrationLink = ""
            ),
            Event(
                name = "Skautský frisbee turnaj o pizzu",
                date = "14.9.2024",
                participants = "Všichni skauti",
                eventDate = "2024-09-14",
                registrationLink = ""
            ),
            Event(
                name = "Korbo",
                date = "Nezveřejněno",
                participants = "15+ skauti",
                eventDate = "2024-09-30",
                registrationLink = ""
            ),
            Event(
                name = "Celostředisková výprava",
                date = "5.10.2024 - 6.10.2024",
                participants = "Všichni z Ichthys",
                eventDate = "2024-10-05",
                registrationLink = ""
            ),
            Event(
                name = "Výprava pro slibující",
                date = "11.10.2024",
                participants = "Slibující 42 + 43 povinně",
                eventDate = "2024-10-11",
                registrationLink = ""
            ),
            Event(
                name = "Jednodenní výprava",
                date = "12.10.2024",
                participants = "Mimča (Alpaky a 2 pardský)",
                eventDate = "2024-10-12",
                registrationLink = ""
            ),
            Event(
                name = "Výjezdní zasedání SRJ",
                date = "18.10.2024 - 20.10.2024",
                participants = "Středisková rada + kuchaři",
                eventDate = "2024-10-18",
                registrationLink = ""
            ),
            Event(
                name = "Podzimky",
                date = "25.10.2024 - 28.10.2024",
                participants = "Pardi a Fénix všichni",
                eventDate = "2024-10-25",
                registrationLink = ""
            ),
            Event(
                name = "Elixír",
                date = "1.11.2024 - 3.11.2024",
                participants = "15+",
                eventDate = "2024-11-01",
                registrationLink = ""
            ),
            Event(
                name = "Střediskový RK PRSK",
                date = "22.11.2024 - 24.11.2024",
                participants = "Rádci, co tam ještě nebyli",
                eventDate = "2024-11-22",
                registrationLink = ""
            ),
            Event(
                name = "Víkendovka",
                date = "29.11.2024 - 1.12.2024",
                participants = "Všichni",
                eventDate = "2024-11-29",
                registrationLink = ""
            ),
            Event(
                name = "Vánoční schůzka",
                date = "19.12.2024",
                participants = "42 + 43 všichni",
                eventDate = "2024-12-19",
                registrationLink = ""
            ),
            Event(
                name = "Mše na uvítání BS",
                date = "22.12.2024",
                participants = "Kdokoliv",
                eventDate = "2024-12-22",
                registrationLink = ""
            ),
            Event(
                name = "Betlémské světlo",
                date = "24.12.2024",
                participants = "42 + 43 všichni",
                eventDate = "2024-12-24",
                registrationLink = ""
            ),
            Event(
                name = "Výprava RK",
                date = "10.1.2025 - 12.1.2025",
                participants = "RK",
                eventDate = "2025-01-10",
                registrationLink = ""
            ),
            Event(
                name = "Víkendovka",
                date = "17.1.2025 - 19.1.2025",
                participants = "Mladší",
                eventDate = "2025-01-17",
                registrationLink = ""
            ),
            Event(
                name = "Víkendovka",
                date = "30.1.2025 - 2.2.2025",
                participants = "Starší",
                eventDate = "2025-01-30",
                registrationLink = ""
            ),
            Event(
                name = "Seznámení mladších střediskových vedoucích",
                date = "14.2.2025 - 16.2.2025",
                participants = "Mladší vedoucí + VO",
                eventDate = "2025-02-14",
                registrationLink = ""
            ),
            Event(
                name = "Oddílová výprava 42",
                date = "7.3.2025 - 9.3.2025",
                participants = "42 všichni",
                eventDate = "2025-03-07",
                registrationLink = ""
            ),
            Event(
                name = "Oddílová výprava 43",
                date = "7.3.2025 - 9.3.2025",
                participants = "43 všichni",
                eventDate = "2025-03-07",
                registrationLink = ""
            ),
            Event(
                name = "Víkendovka",
                date = "21.3.2025 - 23.3.2025",
                participants = "42 + 43 vedoucí",
                eventDate = "2025-03-21",
                registrationLink = ""
            ),
            Event(
                name = "Jednodenní výprava",
                date = "29.3.2025",
                participants = "42 + 43 všichni",
                eventDate = "2025-03-29",
                registrationLink = ""
            ),
            Event(
                name = "Svojsíkáč",
                date = "5.4.2025 - 6.4.2025",
                participants = "",
                eventDate = "2025-04-05",
                registrationLink = ""
            ),
            Event(
                name = "Setkání oddílových rad",
                date = "11.4.2025 - 13.4.2025",
                participants = "",
                eventDate = "2025-04-11",
                registrationLink = ""
            ),
            Event(
                name = "Středisková mše",
                date = "27.4.2025",
                participants = "Kdokoliv",
                eventDate = "2025-04-27",
                registrationLink = ""
            ),
            Event(
                name = "Hledejme cesty",
                date = "1.5.2025 - 4.5.2025",
                participants = "Kdokoliv",
                eventDate = "2025-05-01",
                registrationLink = ""
            ),
            Event(
                name = "Puťák pro skautky a skauty",
                date = "9.5.2025 - 11.5.2025",
                participants = "42 + 43 skauti",
                eventDate = "2025-05-09",
                registrationLink = ""
            ),
            Event(
                name = "Výprava pro vlčata",
                date = "16.5.2025 - 18.5.2025",
                participants = "43 vlčata",
                eventDate = "2025-05-16",
                registrationLink = ""
            ),
            Event(
                name = "Puťák pro světlušky",
                date = "16.5.2025 - 18.5.2025",
                participants = "42 světlušky",
                eventDate = "2025-05-16",
                registrationLink = ""
            ),
            Event(
                name = "Víkendová výprava",
                date = "6.6.2025 - 8.6.2025",
                participants = "42 + 43 všichni",
                eventDate = "2025-06-06",
                registrationLink = ""
            ),
            Event(
                name = "Stavba tábora",
                date = "27.6.2025 - 1.7.2025",
                participants = "42 + 43 starší 14 let",
                eventDate = "2025-06-27",
                registrationLink = ""
            ),
            Event(
                name = "Tábor 2024",
                date = "19.7.2025 - 2.8.2025",
                participants = "42 + 43 všichni",
                eventDate = "2025-07-19",
                registrationLink = ""
            ),
            Event(
                name = "Bourání",
                date = "16.8.2025 - 19.8.2025",
                participants = "42 + 43 starší 14 let",
                eventDate = "2025-08-16",
                registrationLink = ""
            )
        )

        // Smazání existujících událostí a nahrání nových
        eventsRef.removeValue().addOnCompleteListener {
            events.forEach { event ->
                eventsRef.push().setValue(event)
            }
        }
    }
} 