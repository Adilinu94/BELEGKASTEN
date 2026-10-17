package de.belegkasten.feature.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.belegkasten.core.backup.BackupManager
import de.belegkasten.core.data.CsvExport
import de.belegkasten.core.data.Diagnostics
import de.belegkasten.core.license.BillingManager
import de.belegkasten.core.license.LicenseManager
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backup: BackupManager, private val csv: CsvExport, val diagnostics: Diagnostics,
    private val license: LicenseManager, val billing: BillingManager,
) : ViewModel() {
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    val status = MutableStateFlow("")
    // Survives configuration changes while SAF is open, but never persists to disk.
    var pendingPassword: String = ""
    init { refresh() }
    fun refresh() = viewModelScope.launch { status.value = license.trialStatus() }
    fun perform(action: String, uri: Uri, password: String = "") {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                message.value = when (action) {
                    "backup" -> { backup.export(password, uri); runCatching { diagnostics.record("backup") }; "Sicherung geschrieben. Passwort sicher verwahren; ohne Passwort keine Wiederherstellung." }
                    "restore" -> { val count = backup.validateAndImport(password, uri); runCatching { diagnostics.record("restore") }; "$count Belege wiederhergestellt. Vorhandene Belege bleiben erhalten." }
                    "csv" -> "${csv.export(uri)} Belege als CSV exportiert (kein DATEV-Buchungsstapel)."
                    "diagnostic" -> { diagnostics.export(uri); "Diagnose geschrieben. Die Datei enthält nur Tageszählungen, keine Beleginhalte." }
                    else -> error("Unbekannte Aktion")
                }
            } catch (e: Exception) { if (e is CancellationException) throw e; message.value = "Aktion fehlgeschlagen. Passwort, Dateiformat, freien Speicher und Dateizugriff prüfen. Vorhandene Belege werden nicht überschrieben."; runCatching { diagnostics.record("error") } }
            finally { busy.value = false; refresh() }
        }
    }
}

private fun Context.activity(): Activity? = when (this) { is Activity -> this; is ContextWrapper -> baseContext.activity(); else -> null }

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val billingMessage by viewModel.billing.message.collectAsStateWithLifecycle()
    val billingBusy by viewModel.billing.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null && viewModel.pendingPassword.isNotEmpty()) viewModel.perform("backup", uri, viewModel.pendingPassword)
        else if (uri != null) viewModel.message.value = "Passwort nach Prozessneustart nicht mehr vorhanden. Sicherung bitte erneut starten."
        viewModel.pendingPassword = ""
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) { restoreUri = uri; dialog = "restore" } }
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let { viewModel.perform("csv", it) } }
    val diagnostic = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri -> uri?.let { viewModel.perform("diagnostic", it) } }
    LaunchedEffect(billingMessage) { viewModel.refresh() }
    if (dialog != null) AlertDialog(
        onDismissRequest = { dialog = null; password = ""; confirmation = ""; restoreUri = null },
        title = { Text(if (dialog == "backup") "Sicherung verschlüsseln" else "Sicherung wiederherstellen") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Mindestens 8 Zeichen. Ohne dieses Passwort ist die Sicherung unbrauchbar. Kein Upload; lokale Datei.")
            if (dialog == "restore") Text("Belege werden ergänzt. Bei widersprüchlichen IDs oder Anhängen wird die Wiederherstellung abgebrochen.")
            OutlinedTextField(value = password, onValueChange = { password = it.take(256) }, label = { Text("Passwort") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            if (dialog == "backup") OutlinedTextField(value = confirmation, onValueChange = { confirmation = it.take(256) }, label = { Text("Passwort wiederholen") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        } },
        confirmButton = { TextButton(enabled = password.length >= 8 && (dialog != "backup" || password == confirmation), onClick = {
            if (dialog == "backup") { viewModel.pendingPassword = password; export.launch("belegkasten.bkbf") }
            else restoreUri?.let { viewModel.perform("restore", it, password) }
            dialog = null; password = ""; confirmation = ""; restoreUri = null
        }) { Text(if (dialog == "backup") "Datei wählen" else "Geprüft wiederherstellen") } },
        dismissButton = { TextButton(onClick = { dialog = null; password = ""; confirmation = ""; restoreUri = null }) { Text("Abbrechen") } },
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Einstellungen", style = MaterialTheme.typography.headlineSmall)
        Text(status)
        Text("Lesen, Suchen, Öffnen, CSV und Sicherung bleiben ohne Kauf frei. Nur neue Belege pausieren.")
        if (busy || billingBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
        message?.let { Text(it) }; billingMessage?.let { Text(it) }
        Button(enabled = !busy, onClick = { dialog = "backup" }, modifier = Modifier.fillMaxWidth()) { Text("Verschlüsselte Sicherung exportieren") }
        OutlinedButton(enabled = !busy, onClick = { restore.launch(arrayOf("application/octet-stream", "application/zip", "*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Sicherung wiederherstellen") }
        OutlinedButton(enabled = !busy, onClick = { csv.launch("belegkasten.csv") }, modifier = Modifier.fillMaxWidth()) { Text("Belege als CSV exportieren") }
        OutlinedButton(enabled = !busy, onClick = { diagnostic.launch("belegkasten-diagnose.txt") }, modifier = Modifier.fillMaxWidth()) { Text("Freiwillige Diagnose exportieren") }
        Text("Diagnose: ausschließlich lokale Tageszählungen, keine Texte, Bilder, Händler oder Kaufdaten. Die Datei kannst du vor dem Teilen prüfen.")
        Button(enabled = !billingBusy && !busy, onClick = { context.activity()?.let(viewModel.billing::purchase) }, modifier = Modifier.fillMaxWidth()) { Text("Einmalkauf im Play Store") }
        OutlinedButton(enabled = !billingBusy && !busy, onClick = { viewModel.billing.restore() }, modifier = Modifier.fillMaxWidth()) { Text("Käufe wiederherstellen") }
        Text("Erinnerung und Archiv sind eine Gedächtnisstütze, keine GoBD-Ablage. Keine automatische Buchhaltung.")
        Text("Semantische Suche, Foto-Suche und automatische KI-Feldextraktion sind noch nicht freigegeben.")
    }
}
