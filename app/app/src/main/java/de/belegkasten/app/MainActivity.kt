package de.belegkasten.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import de.belegkasten.core.ui.BelegkastenTheme
import de.belegkasten.feature.capture.CaptureRoute
import de.belegkasten.feature.receipts.ReceiptsRoute
import de.belegkasten.feature.search.SearchRoute
import de.belegkasten.feature.settings.SettingsRoute

// Flow: Erfassen -> Prüfen (Capture) -> Suchen -> Detail -> Einstellungen.
// Every destination is reachable without license (ADR-008).
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BelegkastenTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val nav = rememberNavController()
                    NavHost(navController = nav, startDestination = "capture") {
                        composable("capture") { CaptureRoute() }
                        composable("receipts") { ReceiptsRoute() }
                        composable("search") { SearchRoute() }
                        composable("settings") { SettingsRoute() }
                    }
                }
            }
        }
    }
}
