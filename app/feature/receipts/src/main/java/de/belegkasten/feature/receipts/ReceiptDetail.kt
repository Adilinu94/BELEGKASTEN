package de.belegkasten.feature.receipts

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.belegkasten.core.data.ReceiptDetail
import de.belegkasten.core.data.ReceiptFiles
import de.belegkasten.core.data.ReceiptRepository
import de.belegkasten.core.ui.formatCents
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@HiltViewModel
class ReceiptDetailViewModel @Inject constructor(private val repo: ReceiptRepository, saved: SavedStateHandle, val files: ReceiptFiles) : ViewModel() {
    private val id: String = requireNotNull(saved["id"])
    val detail = MutableStateFlow<ReceiptDetail?>(null)
    val loading = MutableStateFlow(true)
    val error = MutableStateFlow<String?>(null)
    init { viewModelScope.launch {
        try { detail.value = withContext(Dispatchers.IO) { repo.detail(id) } }
        catch (e: Exception) { if (e is CancellationException) throw e; error.value = "Beleg kann nicht geladen werden" }
        finally { loading.value = false }
    } }
    fun setDeadline(itemId: String, type: String, dueDate: String) = viewModelScope.launch {
        try { withContext(Dispatchers.IO) { repo.setDeadline(itemId, type, dueDate) }; detail.value = repo.detail(id) }
        catch (e: Exception) { if (e is CancellationException) throw e; error.value = e.message ?: "Frist kann nicht gespeichert werden" }
    }
}

@Composable
fun ReceiptDetailRoute(onBack: () -> Unit, viewModel: ReceiptDetailViewModel = hiltViewModel()) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var fileError by remember { mutableStateOf<String?>(null) }
    var deadlineItem by remember { mutableStateOf<String?>(null) }
    var deadlineType by remember { mutableStateOf("RETURN") }
    var deadlineDate by remember { mutableStateOf("") }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (!it) fileError = "Frist gespeichert, aber Benachrichtigungen sind nicht erlaubt. Bitte in Android-Einstellungen aktivieren."
    }
    if (deadlineItem != null) AlertDialog(onDismissRequest = { deadlineItem = null }, title = { Text("Frist selbst festlegen") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Keine Zusage des Händlers: Datum anhand deiner Unterlagen prüfen.")
            Row { TextButton(onClick = { deadlineType = "RETURN" }) { Text("Rückgabe") }; TextButton(onClick = { deadlineType = "WARRANTY" }) { Text("Garantie") } }
            Text(if (deadlineType == "RETURN") "Rückgabefrist" else "Garantiefrist")
            OutlinedTextField(value = deadlineDate, onValueChange = { deadlineDate = it.take(10) }, label = { Text("Fällig am (JJJJ-MM-TT)") }, singleLine = true)
        } },
        confirmButton = { TextButton(enabled = de.belegkasten.core.data.CheckBeforeSave.validDate(deadlineDate), onClick = {
            viewModel.setDeadline(deadlineItem!!, deadlineType, deadlineDate)
            if (android.os.Build.VERSION.SDK_INT >= 33) permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            deadlineItem = null
        }) { Text("Frist speichern") } }, dismissButton = { TextButton(onClick = { deadlineItem = null }) { Text("Abbrechen") } })
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("Zurück") }
        Text("Belegdetails", style = MaterialTheme.typography.headlineSmall)
        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!loading && detail == null && error == null) Text("Beleg nicht gefunden")
        detail?.let { d ->
            Text(d.receipt.merchant, style = MaterialTheme.typography.titleLarge)
            Text("${d.receipt.date.value} · ${formatCents(d.receipt.total.value)}")
            Text("Kategorie: ${d.receipt.category}")
            d.items.forEach { item ->
                Text("${item.quantity} × ${item.label} · ${formatCents(item.priceCents)}")
             OutlinedButton(onClick = { deadlineItem = item.id; deadlineDate = ""; deadlineType = "RETURN" }) { Text("Rückgabe-/Garantiefrist festlegen") }
            }
            d.deadlines.forEach { Text("${if (it.type == "RETURN") "Rückgabe" else "Garantie"}: ${it.dueDate}") }
            val path = d.receipt.attachmentPath ?: d.receipt.imagePath
            if (path != null) {
                fun open(share: Boolean) {
                    try {
                        val file = viewModel.files.resolve(path)
                        require(file.isFile) { "Originaldatei fehlt" }
                        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
                        val mime = if (path.endsWith(".pdf")) "application/pdf" else "image/*"
                        val intent = if (share) Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri)
                            else Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context.startActivity(if (share) Intent.createChooser(intent, "Original teilen") else intent)
                    } catch (_: ActivityNotFoundException) { fileError = "Keine passende App zum Öffnen installiert" }
                    catch (_: Exception) { fileError = "Originaldatei fehlt oder kann nicht geöffnet werden" }
                }
                Button(onClick = { open(false) }, modifier = Modifier.fillMaxWidth()) { Text("Original öffnen") }
                OutlinedButton(onClick = { open(true) }, modifier = Modifier.fillMaxWidth()) { Text("Original teilen") }
            }
            fileError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text("Bontext / Notiz", style = MaterialTheme.typography.titleMedium)
            Text(d.receipt.ocrText.ifBlank { "Kein Text gespeichert" })
        }
    }
}
