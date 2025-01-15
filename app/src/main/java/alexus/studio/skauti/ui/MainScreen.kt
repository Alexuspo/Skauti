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
import androidx.compose.ui.text.style.TextDecoration
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
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.cos
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.Offset
import android.content.Context
import android.app.Application
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation

data class Event(
    val date: String = "",
    val name: String = "",
    val participants: String = "",
    val eventDate: String = "",
    val registrationLink: String = "",
    val registrationEnabled: Boolean = false,
    val cancelled: Boolean = false
) {
    fun toLocalDate(): LocalDate {
        return try {
            // Pokusíme se nejprve parsovat eventDate (YYYY-MM-DD)
            LocalDate.parse(eventDate)
        } catch (e: Exception) {
            try {
                // Pokud se to nepodaří, zkusíme parsovat date (DD.MM.YYYY)
                val parts = date.split(".")
                if (parts.size >= 3) {
                    val year = if (parts[2].contains(" ")) parts[2].split(" ")[0] else parts[2]
                    LocalDate.of(year.toInt(), parts[1].toInt(), parts[0].toInt())
                } else {
                    LocalDate.now() // Fallback na dnešní datum
                }
            } catch (e: Exception) {
                LocalDate.now() // Fallback na dnešní datum
            }
        }
    }
}

data class AppInfo(
    val version: String,
    val author: String
)

class EventViewModel : ViewModel() {
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()
    
    private val _nextEvent = MutableStateFlow<Event?>(null)
    val nextEvent: StateFlow<Event?> = _nextEvent.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _connectionStatus = MutableStateFlow<String>("Kontroluji připojení...")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()
    
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    private var valueEventListener: ValueEventListener? = null
    
    private val _appInfo = MutableStateFlow<AppInfo?>(null)
    val appInfo: StateFlow<AppInfo?> = _appInfo.asStateFlow()

    private var application: Application? = null
    
    companion object {
        private const val DATABASE_URL = "https://skauti-app-default-rtdb.europe-west1.firebasedatabase.app"
    }
    
    init {
        testFirebaseConnection()
        setupFirebaseListener()
        loadAppInfo()
    }
    
    fun initialize(app: Application) {
        application = app
        checkAuthentication()
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
            }
            
            override fun onCancelled(error: DatabaseError) {
                _connectionStatus.value = "Chyba připojení: ${error.message}"
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
        
        valueEventListener?.let { eventsRef.removeEventListener(it) }
        
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val eventsList = mutableListOf<Event>()
                snapshot.children.forEach { childSnapshot ->
                    try {
                        val event = childSnapshot.getValue(Event::class.java)
                        event?.let { 
                            if (it.name.isNotEmpty() && it.date.isNotEmpty()) {
                                eventsList.add(it)
                            }
                        }
                    } catch (e: Exception) {
                        println("Chyba při načítání události: ${e.message}")
                    }
                }
                
                _events.value = eventsList
                updateNextEvent(eventsList)
                _isLoading.value = false
            }
            
            override fun onCancelled(error: DatabaseError) {
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
                    eventDate.isAfter(currentDate.minusDays(1)) && !event.cancelled
                } catch (e: Exception) {
                    false
                }
            }
            .minByOrNull { event ->
                ChronoUnit.DAYS.between(currentDate, event.toLocalDate())
            }
        _nextEvent.value = nextEvent
    }
    
    private fun loadAppInfo() {
        val database = FirebaseDatabase.getInstance(DATABASE_URL)
        val appInfoRef = database.getReference("appInfo")
        
        appInfoRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val version = snapshot.child("version").getValue(String::class.java) ?: "2.1.0"
                val author = snapshot.child("author").getValue(String::class.java) ?: "Made by: Alexus"
                _appInfo.value = AppInfo(version, author)
            }
            
            override fun onCancelled(error: DatabaseError) {
                println("Chyba při načítání informací o aplikaci: ${error.message}")
            }
        })
    }
    
    private fun checkAuthentication() {
        val sharedPrefs = application?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        _isAuthenticated.value = sharedPrefs?.getBoolean("is_authenticated", false) ?: false
    }

    fun authenticate(password: String): Boolean {
        if (password == "Pardi2024") {
            val sharedPrefs = application?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            sharedPrefs?.edit()?.putBoolean("is_authenticated", true)?.apply()
            _isAuthenticated.value = true
            return true
        }
        return false
    }
    
    override fun onCleared() {
        super.onCleared()
        val database = FirebaseDatabase.getInstance(DATABASE_URL)
        val eventsRef = database.getReference("events")
        valueEventListener?.let { eventsRef.removeEventListener(it) }
    }
}

@Composable
fun CalendarScreen(viewModel: EventViewModel = viewModel()) {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.initialize(context.applicationContext as Application)
    }
    
    var showUpcoming by remember { mutableStateOf(true) }
    var showAllEvents by remember { mutableStateOf(false) }
    val events by viewModel.events.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentDate = LocalDate.now()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
                EventCard(event = event, context = LocalContext.current, viewModel = viewModel)
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

@Composable
fun EventCard(event: Event, context: Context, viewModel: EventViewModel) {
    var showAuthDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

    if (showAuthDialog) {
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = { Text("První přihlášení") },
            text = {
                Column {
                    Text("Pro přístup k přihlašování na akce zadejte heslo:")
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Heslo") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (viewModel.authenticate(password)) {
                        showAuthDialog = false
                    } else {
                        password = ""
                    }
                }) {
                    Text("Potvrdit")
                }
            },
            dismissButton = {
                Button(onClick = { showAuthDialog = false }) {
                    Text("Zrušit")
                }
            }
        )
    }

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
                style = MaterialTheme.typography.titleMedium.copy(
                    textDecoration = if (event.cancelled) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = event.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    textDecoration = if (event.cancelled) TextDecoration.LineThrough else TextDecoration.None
                ),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = event.participants,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = if (event.cancelled) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (!event.cancelled) {  // Zobrazím tlačítko pro přihlášení pouze pokud není událost zrušená
                Button(
                    onClick = {
                        if (!isAuthenticated) {
                            showAuthDialog = true
                        } else if (event.registrationEnabled && event.registrationLink.isNotEmpty()) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.registrationLink))
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (event.registrationEnabled) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    enabled = event.registrationEnabled && event.registrationLink.isNotEmpty()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Zapsat se",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(if (event.registrationEnabled) "Zapsat se" else "Zápis uzavřen")
                    }
                }
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
    var showAboutDialog by remember { mutableStateOf(false) }
    
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: "home"
    val viewModel: EventViewModel = viewModel()
    val appInfo by viewModel.appInfo.collectAsState()

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("O aplikaci") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.alex),
                        contentDescription = "Logo vývojáře",
                        modifier = Modifier
                            .size(150.dp)
                            .padding(vertical = 16.dp)
                    )
                    Text(
                        text = appInfo?.author ?: "Made by: Alexus",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Verze: ${appInfo?.version ?: "2.1.0"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text("Zavřít")
                }
            }
        )
    }

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
                            },
                            trailingIcon = {
                                Text(
                                    text = if (viewModel.connectionStatus.collectAsState().value.contains("připojen")) 
                                        "✓" else "✗",
                                    color = if (viewModel.connectionStatus.collectAsState().value.contains("připojen")) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.error
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("O aplikaci") },
                            onClick = { 
                                showAboutDialog = true
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
    viewModel: EventViewModel = viewModel()
) {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.initialize(context.applicationContext as Application)
    }
    
    val nextEvent by viewModel.nextEvent.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
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
}

@Composable
fun AboutScreen(isDarkTheme: Boolean, onThemeChanged: (Boolean) -> Unit) {
    var showBirthdayDialog by remember { mutableStateOf(false) }
    var clickCount by remember { mutableStateOf(0) }
    var showConfetti by remember { mutableStateOf(false) }
    
    // Datum narozenin (27. dubna)
    val birthdayDate = LocalDate.of(LocalDate.now().year, 4, 27)
    // Pokud už narozeniny tento rok byly, přičteme rok
    val nextBirthday = if (birthdayDate.isBefore(LocalDate.now())) {
        birthdayDate.plusYears(1)
    } else {
        birthdayDate
    }
    
    // Výpočet dnů do narozenin
    val daysUntilBirthday = ChronoUnit.DAYS.between(LocalDate.now(), nextBirthday)

    if (showConfetti) {
        BirthdayConfetti()
    }
    
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
                                        showConfetti = true
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
            onDismissRequest = { 
                showBirthdayDialog = false
                showConfetti = false
            },
            title = { Text("Do narozenin zbývá", textAlign = TextAlign.Center) },
            text = { 
                Box(
                        modifier = Modifier
                            .fillMaxWidth()
                        .height(200.dp)
                    ) {
                    if (showConfetti) {
                        BirthdayConfetti()
                    }
                        Text(
                        text = "$daysUntilBirthday dní",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(vertical = 16.dp),
                            textAlign = TextAlign.Center
                        )
                }
            },
            confirmButton = {
                Button(onClick = { 
                    showBirthdayDialog = false
                    showConfetti = false
                }) {
                            Text("Zavřít")
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
    
    LaunchedEffect(Unit) {
        repeat(50) {
            particles.add(
                Particle(
                    x = Random.nextFloat() * 1000f,
                    y = Random.nextFloat() * -800f,
                    color = listOf(
                        Color(0xFFFF1744),  // Červená
                        Color(0xFFFFD700),  // Zlatá
                        Color(0xFF00E676),  // Zelená
                        Color(0xFF2979FF),  // Modrá
                        Color(0xFFFF4081),  // Růžová
                        Color(0xFFFFEB3B),  // Žlutá
                        Color(0xFF9C27B0),  // Fialová
                        Color(0xFF00BCD4)   // Tyrkysová
                    ).random(),
                    speed = Random.nextFloat() * 1f + 0.5f,
                    size = Random.nextFloat() * 4f + 2f,
                    angle = Random.nextFloat() * 360f
                )
            )
        }
    }

    val animatedPosition by infiniteTransition.animateFloat(
        initialValue = -800f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val rotationAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val yPos = (particle.y + (animatedPosition * particle.speed)) % (size.height + 800f)
            val xPos = (particle.x + sin(rotationAnimation * PI.toFloat() / 180f).toFloat() * 20f * cos(yPos / 100f).toFloat()) % size.width
            
            withTransform({
                translate(
                    left = if (xPos < 0f) size.width + xPos else xPos,
                    top = if (yPos < 0f) size.height + yPos else yPos
                )
                rotate(particle.angle + rotationAnimation / 2f)
            }) {
            drawCircle(
                    color = particle.color.copy(alpha = 0.9f),
                    radius = particle.size,
                    center = Offset(0f, 0f)
                )
            }
        }
    }
}

// Upravená data třída pro částice s novými vlastnostmi
private data class Particle(
    val x: Float,
    val y: Float,
    val color: Color,
    val speed: Float,
    val size: Float,
    val angle: Float
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