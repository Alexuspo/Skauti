package alexus.studio.skauti.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

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

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    controller.setZoom(13.0)
                    controller.setCenter(GeoPoint(49.7442397, 13.4076437))

                    scoutLocations.forEach { location ->
                        val marker = Marker(this).apply {
                            position = GeoPoint(location.latitude, location.longitude)
                            title = location.name
                            snippet = location.address
                        }
                        overlays.add(marker)
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { mapView ->
                mapView.onResume()
            }
        )
    }
} 