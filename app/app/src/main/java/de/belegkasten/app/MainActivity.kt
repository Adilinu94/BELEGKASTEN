package de.belegkasten.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import de.belegkasten.core.ui.BelegkastenTheme
import de.belegkasten.feature.capture.CaptureRoute
import de.belegkasten.feature.receipts.ReceiptsRoute
import de.belegkasten.feature.receipts.ReceiptDetailRoute
import de.belegkasten.feature.search.SearchRoute
import de.belegkasten.feature.settings.SettingsRoute

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var incoming by mutableStateOf<List<Uri>>(emptyList())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState != null) incoming = savedInstanceState.getStringArrayList("incoming")?.map(Uri::parse).orEmpty()
        else receive(intent)
        setContent {
            BelegkastenTheme {
                val nav = rememberNavController()
                val entry by nav.currentBackStackEntryAsState()
                val destinations = listOf("capture" to "Erfassen", "receipts" to "Belege", "search" to "Suchen", "settings" to "Einstellungen")
                LaunchedEffect(incoming.firstOrNull()) { if (incoming.isNotEmpty()) nav.navigate("capture") { launchSingleTop = true } }
                fun detail(id: String) { nav.navigate("detail/${Uri.encode(id)}") }
                Scaffold(bottomBar = {
                    NavigationBar { destinations.forEach { (route, label) ->
                        NavigationBarItem(selected = entry?.destination?.route == route,
                            onClick = { nav.navigate(route) { popUpTo(nav.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Text(label.take(1)) }, label = { Text(label) })
                    } }
                }) { padding ->
                    NavHost(navController = nav, startDestination = "capture", modifier = Modifier.padding(padding)) {
                        composable("capture") { CaptureRoute(incoming.firstOrNull(), { incoming = incoming.drop(1) }, ::detail) }
                        composable("receipts") { ReceiptsRoute(::detail) }
                        composable("search") { SearchRoute(::detail) }
                        composable("settings") { SettingsRoute() }
                        composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { ReceiptDetailRoute({ nav.popBackStack() }) }
                    }
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); receive(intent) }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList("incoming", ArrayList(incoming.map(Uri::toString)))
        super.onSaveInstanceState(outState)
    }
    private fun receive(intent: Intent) {
        val uris = when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
            Intent.ACTION_SEND_MULTIPLE -> IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
            else -> emptyList()
        }.filter { it.scheme == "content" }.take(50)
        incoming = (incoming + uris).take(50)
    }
}
