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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.LinearProgressIndicator
import alexus.studio.skauti.ui.TroopsScreen
import androidx.compose.runtime.mutableStateOf

private val skautFont = FontFamily(
    Font(R.font.skaut, FontWeight.Normal)
)

private val DarkColorScheme = darkColorScheme()
private val LightColorScheme = lightColorScheme()

@Composable
private fun UpdateDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dostupná aktualizace") },
        text = { Text("Je k dispozici nová verze aplikace. Chcete ji nainstalovat?") },
        confirmButton = {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://play.google.com/store/apps/details?id=alexus.studio.skauti")
                        setPackage("com.android.vending")
                    }
                    context.startActivity(intent)
                    onDismiss()
                }
            ) {
                Text("Aktualizovat")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Později")
            }
        }
    )
}

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
    private var valueEventListener: ValueEventListener? = null
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
    
    private val _appInfo = MutableStateFlow<AppInfo?>(null)
    val appInfo: StateFlow<AppInfo?> = _appInfo.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private var application: Application? = null
    
    companion object {
        private const val DATABASE_URL = "https://skauti-app-default-rtdb.europe-west1.firebasedatabase.app"
        private const val GITHUB_RELEASE_URL = "https://github.com/Alexuspo/Skauti/releases"
    }
    
    init {
        testFirebaseConnection()
        setupFirebaseListener()
        loadAppInfo()
    }
    
    fun initialize(app: Application) {
        application = app
        checkAuthentication()
        checkAppVersion()
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
                
                try {
                    val inputStream = application?.assets?.open("version.txt")
                    val localVersion = inputStream?.bufferedReader().use { it?.readText() }?.trim() ?: "0.0.0"
                    val firebaseVersion = version.trim()
                    
                    println("Lokální verze: '$localVersion'")
                    println("Firebase verze: '$firebaseVersion'")
                    
                    // Porovnání verzí s odstraněním bílých znaků
                    val needsUpdate = localVersion.replace("\\s".toRegex(), "") != firebaseVersion.replace("\\s".toRegex(), "")
                    println("Potřebuje aktualizaci: $needsUpdate")
                    
                    _showUpdateDialog.value = needsUpdate
                } catch (e: Exception) {
                    println("Chyba při čtení verze: ${e.message}")
                    e.printStackTrace()
                    _showUpdateDialog.value = false
                }
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

    private fun checkAppVersion() {
        // Tato funkce už není potřeba, vše se děje v loadAppInfo
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
    }

    fun getUpdateUrl(): String {
        return GITHUB_RELEASE_URL
    }

    fun resetAuthentication() {
        val sharedPrefs = application?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPrefs?.edit()?.putBoolean("is_authenticated", false)?.apply()
        _isAuthenticated.value = false
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
                    val dateRange = event.date.split(" - ")
                    val startDate = if (dateRange[0].contains(".")) {
                        val parts = dateRange[0].split(".")
                        LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                    } else {
                        LocalDate.parse(event.eventDate)
                    }
                    
                    val endDate = if (dateRange.size > 1 && dateRange[1].contains(".")) {
                        val parts = dateRange[1].split(".")
                        LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                    } else {
                        startDate
                    }
                    
                    // Událost je nadcházející, pokud končí dnes nebo v budoucnu
                    !endDate.isBefore(LocalDate.now())
                } catch (e: Exception) {
                    println("Chyba při filtrování události ${event.name}: ${e.message}")
                    false
                }
            }
            .sortedBy { it.toLocalDate() }

        val pastEvents = events
            .filter { event ->
                try {
                    val dateRange = event.date.split(" - ")
                    val endDate = if (dateRange.size > 1 && dateRange[1].contains(".")) {
                        val parts = dateRange[1].split(".")
                        LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                    } else if (dateRange[0].contains(".")) {
                        val parts = dateRange[0].split(".")
                        LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                    } else {
                        LocalDate.parse(event.eventDate)
                    }
                    // Událost je proběhlá, pouze pokud skončila před dneškem
                    endDate.isBefore(LocalDate.now())
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
    
    // Parse start and end dates for the event
    val dateRange = event.date.split(" - ")
    val startDate = try {
        if (dateRange[0].contains(".")) {
            val parts = dateRange[0].split(".")
            LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
        } else {
            LocalDate.parse(event.eventDate)
        }
    } catch (e: Exception) {
        LocalDate.now()
    }
    
    val endDate = try {
        if (dateRange.size > 1 && dateRange[1].contains(".")) {
            val parts = dateRange[1].split(".")
            LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
        } else {
            startDate
        }
    } catch (e: Exception) {
        startDate
    }
    
    val currentDate = LocalDate.now()
    val isOngoing = !currentDate.isBefore(startDate) && !currentDate.isAfter(endDate)
    
    // Calculate progress for ongoing events
    val progress = if (isOngoing) {
        val totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1
        val daysElapsed = ChronoUnit.DAYS.between(startDate, currentDate) + 1
        (daysElapsed.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

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
            
            // Show progress for ongoing events
            if (isOngoing && !event.cancelled) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "Akce právě probíhá",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                }
            }
            
            if (!event.cancelled) {
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
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
    val context = LocalContext.current

    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme
    
    if (showUpdateDialog) {
        UpdateDialog { viewModel.dismissUpdateDialog() }
    }

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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography.copy(
            displayLarge = MaterialTheme.typography.displayLarge.copy(fontFamily = skautFont),
            displayMedium = MaterialTheme.typography.displayMedium.copy(fontFamily = skautFont),
            displaySmall = MaterialTheme.typography.displaySmall.copy(fontFamily = skautFont),
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontFamily = skautFont),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = skautFont),
            headlineSmall = MaterialTheme.typography.headlineSmall.copy(fontFamily = skautFont),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = skautFont),
            titleMedium = MaterialTheme.typography.titleMedium.copy(fontFamily = skautFont),
            titleSmall = MaterialTheme.typography.titleSmall.copy(fontFamily = skautFont),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = skautFont),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = skautFont),
            bodySmall = MaterialTheme.typography.bodySmall.copy(fontFamily = skautFont),
            labelLarge = MaterialTheme.typography.labelLarge.copy(fontFamily = skautFont),
            labelMedium = MaterialTheme.typography.labelMedium.copy(fontFamily = skautFont),
            labelSmall = MaterialTheme.typography.labelSmall.copy(fontFamily = skautFont)
        )
    ) {
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
                            DropdownMenuItem(
                                text = { Text("Reset přihlášení") },
                                onClick = { 
                                    viewModel.resetAuthentication()
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
                        label = { Text("Domů", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = currentRoute == "home",
                        onClick = { navController.navigate("home") },
                        modifier = Modifier.weight(1f)
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        label = { Text("Kalendář", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = currentRoute == "calendar",
                        onClick = { navController.navigate("calendar") },
                        modifier = Modifier.weight(1f)
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("Družiny", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = currentRoute == "troops",
                        onClick = { navController.navigate("troops") },
                        modifier = Modifier.weight(1f)
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        label = { Text("Mapa", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = currentRoute == "map",
                        onClick = { navController.navigate("map") },
                        modifier = Modifier.weight(1f)
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        label = { Text("O oddíle", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = currentRoute == "about",
                        onClick = { navController.navigate("about") },
                        modifier = Modifier.weight(1f)
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
                // Open the specified URL in a browser
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://eu.zonerama.com/42Fenix43Pardi/"))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
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

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen(false, {})
} 