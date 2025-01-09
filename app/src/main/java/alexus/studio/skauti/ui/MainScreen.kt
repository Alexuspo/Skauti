package alexus.studio.skauti.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.material3.AlertDialog as Dialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.content.Intent
import android.net.Uri
import alexus.studio.skauti.R
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import kotlin.random.Random
import kotlinx.coroutines.delay
import androidx.navigation.NavController

// Přidáme novou data třídu pro události
data class Event(
    val date: String,
    val name: String,
    val participants: String,
    val eventDate: LocalDate
)

// Přidáme objekt pro správu událostí
object EventRepository {
    val allEvents = listOf(
        Event("6. - 7. 9. 2024", "Pardské přespání", "Pardi vedoucí", 
            LocalDate.of(2024, 9, 6)),
        Event("14. 9. 2024", "Skautský frisbee turnaj o pizzu", "Všichni skauti", 
            LocalDate.of(2024, 9, 14)),
        Event("Nezveřejněno", "Korbo", "15+ skauti", 
            LocalDate.of(2024, 9, 30)), // Přibližné datum pro řazení
        Event("5. - 6. 10. 2024", "Celostředisková výprava", "Všichni z Ichthys", 
            LocalDate.of(2024, 10, 5)),
        Event("11. 10. 2024", "Výprava pro slibující", "Slibující 42 + 43 povinně", 
            LocalDate.of(2024, 10, 11)),
        Event("12. 10. 2024", "Jednodenní výprava", "Mimča (Alpaky a 2 pardský)", 
            LocalDate.of(2024, 10, 12)),
        Event("18. - 20. 10. 2024", "Výjezdní zasedání SRJ", "Středisková rada + kuchaři", 
            LocalDate.of(2024, 10, 18)),
        Event("25. - 28. 10. 2024", "Podzimky", "Pardi a Fénix všichni", 
            LocalDate.of(2024, 10, 25)),
        Event("1. - 3. 11. 2024", "Elixír", "15+", 
            LocalDate.of(2024, 11, 1)),
        Event("22. - 24. 11. 2024", "Střediskový RK PRSK", "Rádci, co tam ještě nebyli", 
            LocalDate.of(2024, 11, 22)),
        Event("29. 11. - 1. 12. 2024", "Víkendovka", "Všichni", 
            LocalDate.of(2024, 11, 29)),
        Event("19. 12. 2024", "Vánoční schůzka", "42 + 43 všichni", 
            LocalDate.of(2024, 12, 19)),
        Event("22. 12. 2024", "Mše na uvítání BS", "Kdokoliv", 
            LocalDate.of(2024, 12, 22)),
        Event("24. 12. 2024", "Betlémské světlo", "42 + 43 všichni", 
            LocalDate.of(2024, 12, 24)),
        Event("10. - 12. 1. 2025", "Výprava RK", "RK", 
            LocalDate.of(2025, 1, 10)),
        Event("17. - 19. 1. 2025", "Víkendovka", "Mladší", 
            LocalDate.of(2025, 1, 17)),
        Event("30. 1. - 2. 2. 2025", "Víkendovka", "Starší", 
            LocalDate.of(2025, 1, 30)),
        Event("14. - 16. 2. 2025", "Seznámení mladších střediskových vedoucích", "Mladší vedoucí + VO", 
            LocalDate.of(2025, 2, 14)),
        Event("7. - 9. 3. 2025", "Oddílová výprava 42", "42 všichni", 
            LocalDate.of(2025, 3, 7)),
        Event("7. - 9. 3. 2025", "Oddílová výprava 43", "43 všichni", 
            LocalDate.of(2025, 3, 7)),
        Event("21. - 23. 3. 2025", "Víkendovka", "42 + 43 vedoucí", 
            LocalDate.of(2025, 3, 21)),
        Event("29. 3. 2025", "Jednodenní výprava", "42 + 43 všichni", 
            LocalDate.of(2025, 3, 29)),
        Event("5. - 6. 4. 2025", "Svojsíkáč", "", 
            LocalDate.of(2025, 4, 5)),
        Event("11. - 13. 4. 2025", "Setkání oddílových rad", "", 
            LocalDate.of(2025, 4, 11)),
        Event("27. 4. 2025", "Středisková mše", "Kdokoliv", 
            LocalDate.of(2025, 4, 27)),
        Event("1. - 4. 5. 2025", "Hledejme cesty", "Kdokoliv", 
            LocalDate.of(2025, 5, 1)),
        Event("9. - 11. 5. 2025", "Puťák pro skautky a skauty", "42 + 43 skauti", 
            LocalDate.of(2025, 5, 9)),
        Event("16. - 18. 5. 2025", "Výprava pro vlčata", "43 vlčata", 
            LocalDate.of(2025, 5, 16)),
        Event("16. - 18. 5. 2025", "Puťák pro světlušky", "42 světlušky", 
            LocalDate.of(2025, 5, 16)),
        Event("6. - 8. 6. 2025", "Víkendová výprava", "42 + 43 všichni", 
            LocalDate.of(2025, 6, 6)),
        Event("27. 6. - 1. 7. 2025", "Stavba tábora", "42 + 43 starší 14 let", 
            LocalDate.of(2025, 6, 27)),
        Event("19. 7. - 2. 8. 2025", "Tábor 2024", "42 + 43 všichni", 
            LocalDate.of(2025, 7, 19)),
        Event("16. - 19. 8. 2025", "Bourání", "42 + 43 starší 14 let", 
            LocalDate.of(2025, 8, 16))
    ).sortedBy { it.eventDate }
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
                    icon = { Icon(Icons.Default.Group, contentDescription = null) },
                    label = { Text("Družiny") },
                    selected = currentRoute == "troops",
                    onClick = { navController.navigate("troops") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = null) },
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
            composable("home") { HomeScreen(navController) }
            composable("calendar") { CalendarScreen() }
            composable("troops") { TroopsScreen() }
            composable("about") { AboutScreen(isDarkTheme, onThemeChanged) }
            composable("map") { MapScreen() }
        }
    }
}

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    
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
                    Icons.Default.Group,
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

        val nextEvent = remember {
            EventRepository.allEvents
                .filter { it.eventDate.isAfter(LocalDate.now()) }
                .minByOrNull { it.eventDate }
        }

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
                        text = nextEvent.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Datum: ${nextEvent.date}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Účastníci: ${nextEvent.participants}",
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
fun CalendarScreen() {
    var showUpcoming by remember { mutableStateOf(true) }
    var showAllEvents by remember { mutableStateOf(false) }
    
    val currentDate = LocalDate.now()
    
    // Filtrujeme události podle data a řadíme je
    val upcomingEvents = EventRepository.allEvents
        .filter { it.eventDate >= currentDate }
        .sortedBy { it.eventDate }  // Nadcházející akce seřadíme od nejbližší

    val pastEvents = EventRepository.allEvents
        .filter { it.eventDate < currentDate }
        .sortedByDescending { it.eventDate }  // Proběhlé akce seřadíme od nejnovější po nejstarší

    // Vybereme správný seznam podle toho, co má být zobrazeno
    val filteredEvents = if (showUpcoming) upcomingEvents else pastEvents
    val displayedEvents = if (showAllEvents) filteredEvents else filteredEvents.take(5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
                    showAllEvents = false // Reset zobrazení při přepnutí
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showUpcoming) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text("Nadcházející akce (${upcomingEvents.size})")
            }
            Button(
                onClick = { 
                    showUpcoming = false
                    showAllEvents = false // Reset zobrazení při přepnutí
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!showUpcoming) MaterialTheme.colorScheme.surfaceVariant 
                                   else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text("Proběhlé akce (${pastEvents.size})")
            }
        }

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
        Dialog(onDismissRequest = { showBirthdayDialog = false }) {
            Surface(
                modifier = Modifier
                    .padding(16.dp)
                    .wrapContentSize()
                    .size(300.dp),  // Omezená velikost dialogu
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    BirthdayConfetti()
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "🎂 Odpočet do narozenin 🎂",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                            textAlign = TextAlign.Center
                        )
                        
                        val currentDate = LocalDate.now()
                        val birthdayThisYear = LocalDate.of(
                            currentDate.year + if (currentDate.monthValue > 4 || 
                                (currentDate.monthValue == 4 && currentDate.dayOfMonth > 27)) 1 else 0, 
                            4, 
                            27
                        )
                        val daysUntilBirthday = ChronoUnit.DAYS.between(currentDate, birthdayThisYear)

                        Text(
                            "$daysUntilBirthday",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Text(
                            if(daysUntilBirthday == 1L) "den" else "dní",
                            style = MaterialTheme.typography.titleSmall
                        )
                        
                        Button(
                            onClick = { showBirthdayDialog = false },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Zavřít")
                        }
                    }
                }
            }
        }
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