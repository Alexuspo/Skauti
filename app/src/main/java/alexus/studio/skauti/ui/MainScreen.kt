package alexus.studio.skauti.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import alexus.studio.skauti.R
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.random.Random
import kotlinx.coroutines.delay
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.format.DateTimeFormatter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import alexus.studio.skauti.utils.EventInitializer

data class Event(
    val date: String = "",  // formát DD.MM.YYYY
    val name: String = "",
    val participants: String = "",
    val eventDate: String = ""  // formát YYYY-MM-DD
) {
    fun toLocalDate(): LocalDate {
        return try {
            // Pokusíme se nejprve parsovat eventDate (YYYY-MM-DD)
            LocalDate.parse(eventDate)
        } catch (e: Exception) {
            try {
                // Pokud se to nepodaří, zkusíme parsovat date (DD.MM.YYYY)
                val parts = date.split(".")
                if (parts.size == 3) {
                    LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                } else {
                    LocalDate.now() // Fallback na dnešní datum
                }
            } catch (e: Exception) {
                LocalDate.now() // Fallback na dnešní datum
            }
        }
    }
}

class EventViewModel : ViewModel() {
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()
    
    private val _nextEvent = MutableStateFlow<Event?>(null)
    val nextEvent: StateFlow<Event?> = _nextEvent.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _connectionStatus = MutableStateFlow<String>("Kontroluji připojení...")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()
    
    private var valueEventListener: ValueEventListener? = null
    
    companion object {
        private const val DATABASE_URL = "https://skauti-app-default-rtdb.europe-west1.firebasedatabase.app"
    }
    
    init {
        testFirebaseConnection()
        setupFirebaseListener()
    }
    
    private fun testFirebaseConnection() {
        val database = FirebaseDatabase.getInstance(DATABASE_URL)
        val testRef = database.getReference(".info/connected")
        
        testRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                _connectionStatus.value = if (connected) {
                    "Firebase připojen"
                } else {
                    "Firebase odpojen"
                }
                println("Firebase status: ${_connectionStatus.value}")
            }
            
            override fun onCancelled(error: DatabaseError) {
                _connectionStatus.value = "Chyba připojení: ${error.message}"
                println("Firebase chyba: ${error.message}")
            }
        })
    }
    
    fun refreshData() {
        _isLoading.value = true
        setupFirebaseListener()
    }
    
    private fun setupFirebaseListener() {
        val database = FirebaseDatabase.getInstance(DATABASE_URL)
        val eventsRef = database.getReference("events")
        
        // Odstraníme starý listener, pokud existuje
        valueEventListener?.let { eventsRef.removeEventListener(it) }
        
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                println("Firebase onDataChange - počet událostí: ${snapshot.childrenCount}")
                val eventsList = mutableListOf<Event>()
                snapshot.children.forEach { childSnapshot ->
                    try {
                        val event = childSnapshot.getValue(Event::class.java)
                        println("Načtená událost: ${event?.name}, Datum: ${event?.date}")
                        event?.let { 
                            if (it.name.isNotEmpty() && it.date.isNotEmpty()) {
                                eventsList.add(it)
                            }
                        }
                    } catch (e: Exception) {
                        println("Chyba při načítání události: ${e.message}")
                    }
                }
                
                println("Celkem načteno událostí: ${eventsList.size}")
                _events.value = eventsList
                updateNextEvent(eventsList)
                _isLoading.value = false
            }
            
            override fun onCancelled(error: DatabaseError) {
                println("Firebase Error: ${error.message}")
                _isLoading.value = false
            }
        }
        
        eventsRef.addValueEventListener(valueEventListener!!)
    }
    
    private fun updateNextEvent(events: List<Event>) {
        val currentDate = LocalDate.now()
        val nextEvent = events
            .filter { event ->
                try {
                    val eventDate = event.toLocalDate()
                    eventDate.isAfter(currentDate.minusDays(1))
                } catch (e: Exception) {
                    false
                }
            }
            .minByOrNull { event ->
                ChronoUnit.DAYS.between(
                    currentDate,
                    event.toLocalDate()
                )
            }
        _nextEvent.value = nextEvent
    }
    
    override fun onCleared() {
        super.onCleared()
        // Odstraníme listener při zničení ViewModelu
        val database = FirebaseDatabase.getInstance()
        val eventsRef = database.getReference("events")
        valueEventListener?.let { eventsRef.removeEventListener(it) }
    }
}

@Composable
fun CalendarScreen(viewModel: EventViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    var showUpcoming by remember { mutableStateOf(true) }
    var showAllEvents by remember { mutableStateOf(false) }
    val events by viewModel.events.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val currentDate = LocalDate.now()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Status připojení
        Text(
            text = connectionStatus,
            style = MaterialTheme.typography.bodyMedium,
            color = if (connectionStatus.contains("připojen")) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Tlačítko pro aktualizaci
        Button(
            onClick = { viewModel.refreshData() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            if (isLoading) {
                Text("Aktualizuji...")
            } else {
                Text("Aktualizovat")
            }
        }

        // Přepínací tlačítka
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { 
                    showUpcoming = true
                    showAllEvents = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showUpcoming) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text("Nadcházející")
            }
            Button(
                onClick = { 
                    showUpcoming = false
                    showAllEvents = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!showUpcoming) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text("Proběhlé")
            }
        }
        
        // Filtrujeme události podle data
        val upcomingEvents = events
            .filter { event ->
                try {
                    val eventDate = event.toLocalDate()
                    eventDate.isAfter(currentDate.minusDays(1))
                } catch (e: Exception) {
                    println("Chyba při filtrování události ${event.name}: ${e.message}")
                    false
                }
            }
            .sortedBy { it.toLocalDate() }

        val pastEvents = events
            .filter { event ->
                try {
                    val eventDate = event.toLocalDate()
                    eventDate.isBefore(currentDate)
                } catch (e: Exception) {
                    println("Chyba při filtrování události ${event.name}: ${e.message}")
                    false
                }
            }
            .sortedByDescending { it.toLocalDate() }

        // Debug výpisy
        LaunchedEffect(events) {
            println("Počet všech událostí: ${events.size}")
            println("Počet nadcházejících událostí: ${upcomingEvents.size}")
            println("Počet proběhlých událostí: ${pastEvents.size}")
            events.forEach { event ->
                println("Událost: ${event.name}, Datum: ${event.date}, EventDate: ${event.eventDate}")
            }
        }

        // Vybereme správný seznam podle toho, co má být zobrazeno
        val filteredEvents = if (showUpcoming) upcomingEvents else pastEvents
        val displayedEvents = if (showAllEvents) filteredEvents else filteredEvents.take(5)

        // Seznam událostí
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(displayedEvents) { event ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = event.date,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = event.name,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(
                            text = event.participants,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Tlačítko "Zobrazit více"
        if (!showAllEvents && filteredEvents.size > 5) {
            Button(
                onClick = { showAllEvents = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Zobrazit více (${filteredEvents.size - 5})")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: "home"
    
    // Vytvoříme viewModel zde, aby byl sdílený mezi všemi obrazovkami
    val viewModel: EventViewModel = viewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Nastavení"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Tmavý režim") },
                            onClick = { },
                            trailingIcon = {
                                Switch(
                                    checked = isDarkTheme,
                                    onCheckedChange = { 
                                        onThemeChanged(it)
                                        showMenu = false
                                    }
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Kontrola připojení") },
                            onClick = { 
                                viewModel.refreshData()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Inicializovat události") },
                            onClick = { 
                                EventInitializer.initializeEvents()
                                showMenu = false
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Domů") },
                    selected = currentRoute == "home",
                    onClick = { navController.navigate("home") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    label = { Text("Kalendář") },
                    selected = currentRoute == "calendar",
                    onClick = { navController.navigate("calendar") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Družiny") },
                    selected = currentRoute == "troops",
                    onClick = { navController.navigate("troops") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    label = { Text("Mapa") },
                    selected = currentRoute == "map",
                    onClick = { navController.navigate("map") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text("O oddíle") },
                    selected = currentRoute == "about",
                    onClick = { navController.navigate("about") }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") { HomeScreen(navController, viewModel) }
            composable("calendar") { CalendarScreen(viewModel) }
            composable("troops") { TroopsScreen() }
            composable("about") { AboutScreen(isDarkTheme, onThemeChanged) }
            composable("map") { MapScreen() }
        }
    }
}

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: EventViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val nextEvent by viewModel.nextEvent.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo Skauti",
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 16.dp)
        )
        
        Text(
            "43. Oddíl Pardi",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Button(
            onClick = { navController.navigate("calendar") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Kalendář akcí")
            }
        }

        Button(
            onClick = { navController.navigate("troops") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Družiny")
            }
        }

        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://eu.zonerama.com/Pardi/1364990"))
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Fotogalerie")
            }
        }

        // Nejbližší akce se přesune pod tlačítka
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Nejbližší akce:",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (nextEvent != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Text(
                        text = nextEvent!!.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Datum: ${nextEvent!!.date}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Účastníci: ${nextEvent!!.participants}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            Text(
                "Momentálně nejsou naplánovány žádné akce",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun AboutScreen(isDarkTheme: Boolean, onThemeChanged: (Boolean) -> Unit) {
    var showBirthdayDialog by remember { mutableStateOf(false) }
    var clickCount by remember { mutableStateOf(0) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                "O oddílu",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "O oddílu",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Text(
                        text = "Jsme chlapecký oddíl z křesťanského střediska Ichthys s více než dvacetipětiletou historií.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "Klubovnu máme v Plzni na Slovanech v areálu dominikánského kláštera, kde probíhají jednotlivé " +
                              "schůzky družin, turnaje v deskových hrách či sportovní aktivity a podobně.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "Přibližně jednou za měsíc pořádáme jednodenní výpravy do přírody, občas se vydáme na vícedenní " +
                              "akci a o prázdninách nás čeká tábor na louce u říčky Úhlavky nedaleko Kladrub.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Kontaktní informace",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Levá strana s kontaktními informacemi
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Vedoucí oddílu",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Text(
                                text = "Pavel Balda",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            Row(
                                modifier = Modifier.padding(bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = "Telefon",
                                    modifier = Modifier.padding(end = 8.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "777 362 036",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = "Email",
                                    modifier = Modifier.padding(end = 8.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "pardi@skaut.cz",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        
                        // Pravá strana s obrázkem
                        Image(
                            painter = painterResource(id = R.drawable.paja),
                            contentDescription = "Vedoucí oddílu",
                            modifier = Modifier
                                .size(150.dp)
                                .padding(start = 16.dp)
                                .clickable {
                                    clickCount++
                                    if (clickCount >= 3) {
                                        showBirthdayDialog = true
                                        clickCount = 0
                                    }
                                }
                        )
                    }
                }
            }
        }
    }

    if (showBirthdayDialog) {
        AlertDialog(
            onDismissRequest = { showBirthdayDialog = false },
            title = { Text("Narozeniny") },
            text = { Text("Přejeme vše nejlepší k narozeninám!") },
            confirmButton = {
                Button(onClick = { showBirthdayDialog = false }) {
                    Text("Děkuji")
                }
            }
        )
    }
}

@Composable
fun TroopsScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                "Oddílová rada",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                "Vedoucí oddílu: Pavel Balda",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                "Zástupce vedoucího oddílu: František Verner",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                "Rádci oddílu:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Column(modifier = Modifier.padding(start = 16.dp, bottom = 24.dp)) {
                listOf(
                    "Štěpán Brožek", "Martin Kuchta", "David Hroch",
                    "Zdeněk Strach", "Jan Jirka - \"Hřebík\"", "Jan Holý",
                    "Adam Dejmal", "David Pop - \"Dave\"", "Antonín Mik - \"Korýš\"",
                    "Tomáš Náhlík - \"Jonatán\"", "Matouš Buldra", "Oskar Buben",
                    "Jan Lotschar"
                ).forEach { name ->
                    Text(
                        name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            // Vlčácké družiny
            listOf(
                Triple("Vlčácká družina - Tyrkysová šestka", 
                       "Pondělí 17:30 - 19:00, Klubovna - Jiráskovo Náměstí",
                       listOf("Štěpán Brožek", "Martin Kuchta")),
                Triple("Vlčácká družina - Orlí šestka",
                       "Úterý 17:30 - 19:00, Klubovna - Jiráskovo Náměstí",
                       listOf("Jan Jirka - \"Hřebík\"", "Jan Holý", "Adam Dejmal")),
                Triple("Vlčácká družina - Dračí šestka",
                       "Pondělí 16:00 - 17:30, Klubovna - Jiráskovo Náměstí",
                       listOf("Matouš Buldra", "Oskar Buben", "Jan Lontschar"))
            ).forEach { (name, time, leaders) ->
                TroopCard(name, time, leaders)
            }

            // Skautské družiny
            listOf(
                Triple("Skautská družina - Vlci",
                       "Pondělí 16:30 - 18:00, Klubovna - Sušická 92",
                       listOf("David Hroch", "Zdeněk Strach")),
                Triple("Skautská družina - Sloníři",
                       "Středa 17:00 - 18:30, Klubovna - Sušická 92",
                       listOf("David Pop - \"Dave\"", "Antonín Mik - \"Korýš\"", "Tomáš Náhlík - \"Jonatán\""))
            ).forEach { (name, time, leaders) ->
                TroopCard(name, time, leaders)
            }

            // Družina mladších vedoucích
            TroopCard(
                "Družina mladších vedoucích - Nutrie",
                "Pondělí 18:30 - 20:00",
                emptyList()
            )
        }
    }
}

@Composable
private fun TroopCard(name: String, time: String, leaders: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                time,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = if (leaders.isEmpty()) 0.dp else 8.dp)
            )
            if (leaders.isNotEmpty()) {
                Text(
                    "Rádcové:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                leaders.forEach { leader ->
                    Text(
                        leader,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen(false, {})
}

@Composable
fun BirthdayConfetti() {
    val particles = remember { mutableStateListOf<Particle>() }
    val infiniteTransition = rememberInfiniteTransition()
    
    // Inicializace částic - rozprostřeme je po celé šířce
    LaunchedEffect(Unit) {
        repeat(50) {
            particles.add(
                Particle(
                    x = Random.nextFloat() * 1000,  // Zvětšíme rozsah pro x
                    y = Random.nextFloat() * -1000,  // Začneme více nad horním okrajem
                    color = listOf(
                        Color(0xFFFF1744),  // Červená
                        Color(0xFFFFD700),  // Zlatá
                        Color(0xFF00E676),  // Zelená
                        Color(0xFF2979FF),  // Modrá
                        Color(0xFFFF4081),  // Růžová
                        Color(0xFFFFEB3B)   // Žlutá
                    ).random(),
                    speed = Random.nextFloat() * 3 + 0.5f  // Větší rozsah rychlostí
                )
            )
        }
    }

    // Animace pozice - prodloužíme dobu animace
    val animatedPosition by infiniteTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            // Upravíme výpočet pozice pro plynulejší pohyb
            val yPos = (particle.y + (animatedPosition * particle.speed)) % (size.height + 1000)
            val xPos = (particle.x) % size.width
            
            drawCircle(
                color = particle.color,
                radius = 3f,  // Menší částice pro lepší vzhled
                center = androidx.compose.ui.geometry.Offset(
                    if (xPos < 0) size.width + xPos else xPos,
                    if (yPos < 0) size.height + yPos else yPos
                )
            )
        }
    }
}

// Upravená data třída pro částice
private data class Particle(
    val x: Float,
    val y: Float,
    val color: Color,
    val speed: Float  // Přidáme rychlost pro různou rychlost pádu
) 

// Funkce pro přidání událostí do Firebase
private fun addEventsToFirebase() {
    val database = FirebaseDatabase.getInstance()
    val eventsRef = database.getReference("events")
    
    val events = listOf(
        Event(
            name = "Fénix víkend",
            date = "6.9.2024",
            participants = "Fénix vedoucí",
            eventDate = "2024-09-06"
        ),
        Event(
            name = "Pardské přespání",
            date = "6.9.2024 - 7.9.2024",
            participants = "Pardi vedoucí",
            eventDate = "2024-09-06"
        ),
        // ... další události
    )
    
    events.forEach { event ->
        eventsRef.push().setValue(event)
    }
} 