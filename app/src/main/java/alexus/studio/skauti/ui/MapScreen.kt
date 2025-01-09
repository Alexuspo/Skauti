package alexus.studio.skauti.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider

data class Location(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)

val scoutLocations = listOf(
    Location(
        "Klubovna Sušická",
        "Sušická 92, Plzeň",
        49.7328046,
        13.4154442
    ),
    Location(
        "Klubovna Jiráskovo náměstí",
        "Jiráskovo nám. 840, Plzeň",
        49.7360800,
        13.3933808
    ),
    Location(
        "Klubovna u Jiřího",
        "Ke Svatému Jiří 13/60, Plzeň",
        49.7638347,
        13.4141061
    )
)

@Composable
fun MapScreen() {
    val context = LocalContext.current
    Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var showErrorMessage by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) 
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    
                    // Nastavíme výchozí pozici na střed Plzně
                    controller.setZoom(13.0)
                    controller.setCenter(GeoPoint(49.7474447, 13.3776670))
                    
                    // Nastavíme limity pro zoom
                    minZoomLevel = 12.0
                    maxZoomLevel = 18.0

                    scoutLocations.forEach { location ->
                        val marker = Marker(this).apply {
                            position = GeoPoint(location.latitude, location.longitude)
                            title = location.name
                            snippet = location.address
                        }
                        overlays.add(marker)
                    }
                }.also { mapView = it }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                mapView = view
                view.onResume()
            }
        )

        Button(
            onClick = {
                if (hasLocationPermission) {
                    try {
                        val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        lastLocation?.let { location ->
                            // Kontrola, zda je uživatel v oblasti Plzně
                            if (isInPlzen(location.latitude, location.longitude)) {
                                mapView?.controller?.animateTo(
                                    GeoPoint(location.latitude, location.longitude)
                                )
                                showErrorMessage = false
                            } else {
                                showErrorMessage = true
                            }
                        }
                    } catch (e: SecurityException) {
                        // Ošetření chyby oprávnění
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("Moje poloha")
        }

        if (showErrorMessage) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    "Nenacházíte se v Plzni :(",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

private fun isInPlzen(latitude: Double, longitude: Double): Boolean {
    // Přibližné hranice Plzně
    val plzenBounds = object {
        val northLat = 49.7900
        val southLat = 49.7000
        val westLon = 13.3000
        val eastLon = 13.4500
    }
    
    return latitude in plzenBounds.southLat..plzenBounds.northLat &&
           longitude in plzenBounds.westLon..plzenBounds.eastLon
} 