package de.belegkasten.feature.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.belegkasten.core.data.CheckBeforeSave
import de.belegkasten.core.data.ReceiptFiles
import de.belegkasten.core.data.ReceiptRepository
import de.belegkasten.core.database.ReceiptEntity
import de.belegkasten.core.license.LicenseManager
import de.belegkasten.core.ocr.OcrEngine
import de.belegkasten.core.ocr.RuleParser
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CaptureState(
    val canCapture: Boolean = false, val trialNote: String = "Wird geprüft …",
    val path: String? = null, val merchant: String = "", val date: String = "",
    val total: String = "", val category: String = "", val text: String = "",
    val positions: String = "",
    val busy: Boolean = false, val error: String? = null, val savedId: String? = null,
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val license: LicenseManager, private val files: ReceiptFiles,
    private val ocr: OcrEngine, private val parser: RuleParser,
    private val repo: ReceiptRepository, private val saved: SavedStateHandle,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(CaptureState(
        path = saved["path"], merchant = saved["merchant"] ?: "", date = saved["date"] ?: "",
        total = saved["total"] ?: "", category = saved["category"] ?: "", text = saved["text"] ?: "",
        positions = saved["positions"] ?: "",
    ))
    val state: StateFlow<CaptureState> = _state
    init { refresh() }

    fun refresh() = viewModelScope.launch {
        try {
            val status = license.status()
            _state.value = _state.value.copy(canCapture = status.captureEnabled, trialNote = status.note)
        } catch (e: Exception) { if (e is CancellationException) throw e; fail("Lizenzstatus kann nicht gelesen werden") }
    }

    fun edit(field: String, value: String) {
        if (_state.value.busy) return
        val valueLimited = value.take(when (field) { "text" -> 60000; "positions" -> 10000; else -> 500 })
        saved[field] = valueLimited
        _state.value = when (field) {
            "merchant" -> _state.value.copy(merchant = valueLimited)
            "date" -> _state.value.copy(date = valueLimited)
            "total" -> _state.value.copy(total = valueLimited)
            "category" -> _state.value.copy(category = valueLimited)
            "text" -> _state.value.copy(text = valueLimited)
            "positions" -> _state.value.copy(positions = valueLimited)
            else -> _state.value
        }.copy(error = null)
    }

    fun ingest(uri: Uri, shared: Boolean = false) {
        if (_state.value.busy || _state.value.path != null) return
        _state.value = _state.value.copy(busy = true, error = null)
        viewModelScope.launch {
            var path: String? = null
            try {
                check(license.captureEnabled()) { "Testphase beendet. Dein Archiv bleibt nutzbar." }
                path = files.import(uri)
                // Keep the original and a resumable draft even if OCR fails.
                saved["path"] = path
                saved["source"] = if (path.endsWith(".pdf")) "E_INVOICE_PDF" else if (uri.authority == context.packageName + ".files") "CAMERA" else if (shared) "SHARE" else "IMPORT"
                saved["receiptId"] = UUID.randomUUID().toString()
                saved["createdAt"] = System.currentTimeMillis()
                _state.value = _state.value.copy(path = path)
                val text = if (path.endsWith(".pdf")) "" else withContext(Dispatchers.IO) {
                    ocr.recognize(InputImage.fromFilePath(context, Uri.fromFile(files.resolve(path))))
                }.take(60000)
                val fields = parser.parse(text)
                _state.value = _state.value.copy(path = path, busy = false)
                edit("text", text)
                edit("merchant", text.lineSequence().firstOrNull { it.isNotBlank() }?.take(200) ?: "")
                edit("date", fields.dateIso ?: "")
                edit("total", fields.totalCents?.let { java.math.BigDecimal.valueOf(it, 2).toPlainString().replace('.', ',') } ?: "")
                edit("category", "Sonstiges")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                fail(if (path != null) "Texterkennung fehlgeschlagen. Original bleibt erhalten; Felder bitte manuell ausfüllen."
                    else "Import fehlgeschlagen. Bitte JPEG, PNG, WebP oder PDF bis 20 MB auswählen.")
            } finally {
                // Only our own provider's temporary camera outputs, never another app's file.
                if (uri.authority == context.packageName + ".files" && uri.path?.startsWith("/camera/") == true) {
                    runCatching { context.contentResolver.delete(uri, null, null) }
                }
            }
        }
    }

    fun save() {
        if (_state.value.busy) return
        val s = _state.value
        val amount = parser.toCents(s.total.trim())
        if (amount == null || amount < 0 || !CheckBeforeSave.validDate(s.date) || s.merchant.isBlank() || s.category.isBlank()) {
            fail("Händler, Kategorie, gültiges Datum (JJJJ-MM-TT) und Betrag (z. B. 12,99) prüfen"); return
        }
        val path = s.path ?: return
        val id = saved.get<String>("receiptId") ?: UUID.randomUUID().toString().also { saved["receiptId"] = it }
        val createdAt = saved.get<Long>("createdAt") ?: System.currentTimeMillis().also { saved["createdAt"] = it }
        val positions = try { parsePositions(s.positions, id, parser) }
        catch (e: IllegalArgumentException) { fail(e.message ?: "Positionen prüfen"); return }
        val receipt = ReceiptEntity(id, s.merchant.trim(), s.date, amount, s.category.trim(),
            path.takeUnless { it.endsWith(".pdf") }, saved["source"] ?: "IMPORT",
            path.takeIf { it.endsWith(".pdf") }, createdAt, s.text)
        if (CheckBeforeSave.checkReceipt(receipt, positions).isNotEmpty()) {
            fail("Positionssumme weicht von der Belegsumme ab oder Betrag ist zu groß. Bitte korrigieren."); return
        }
        _state.value = s.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repo.save(receipt, positions)
                }
                clearDraft()
                _state.value = _state.value.copy(savedId = id)
                refresh()
            } catch (e: Exception) { if (e is CancellationException) throw e; fail(e.message ?: "Speichern fehlgeschlagen") }
        }
    }

    fun fail(message: String) { _state.value = _state.value.copy(busy = false, error = message) }
    fun discard() {
        if (_state.value.busy) return
        val path = _state.value.path ?: return
        _state.value = _state.value.copy(busy = true)
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { repo.discardUnreferenced(path) }; clearDraft() }
            catch (e: Exception) { if (e is CancellationException) throw e; fail("Entwurf konnte nicht verworfen werden") }
        }
    }
    private fun clearDraft() {
        listOf("path", "merchant", "date", "total", "category", "text", "positions", "source", "receiptId").forEach { saved.remove<String>(it) }
        saved.remove<Long>("createdAt")
        _state.value = CaptureState(canCapture = _state.value.canCapture, trialNote = _state.value.trialNote)
    }
    fun savedConsumed() { _state.value = _state.value.copy(savedId = null) }
    fun original(path: String) = files.resolve(path)
}

@Composable
fun CaptureRoute(
    sharedInput: Uri? = null, onSharedConsumed: () -> Unit = {}, onSaved: (String) -> Unit,
    viewModel: CaptureViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var camera by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { viewModel.ingest(it) } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) camera = true else viewModel.fail("Kamerazugriff abgelehnt. Datei-Import bleibt verfügbar.")
    }
    LaunchedEffect(sharedInput, s.path, s.busy) {
        if (sharedInput != null && s.path == null && !s.busy) { viewModel.ingest(sharedInput, shared = true); onSharedConsumed() }
    }
    LaunchedEffect(s.savedId) { s.savedId?.let { onSaved(it); viewModel.savedConsumed() } }
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)
        viewModel.refresh()
        onDispose { lifecycle.removeObserver(observer) }
    }
    BackHandler(enabled = s.path != null) { if (!s.busy) confirmDiscard = true }
    if (camera) CameraDialog(onDismiss = { camera = false }, onCaptured = { uri -> camera = false; viewModel.ingest(uri) }, onError = { camera = false; viewModel.fail(it) })
    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false }, title = { Text("Entwurf verwerfen?") },
        text = { Text("Der noch nicht gespeicherte Beleg wird gelöscht.") },
        confirmButton = { TextButton(onClick = { viewModel.discard(); confirmDiscard = false }) { Text("Verwerfen") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Weiter prüfen") } })
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (s.path == null) "Erfassen" else "Prüfen vor dem Speichern", style = MaterialTheme.typography.headlineSmall)
        Text(s.trialNote)
        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (s.busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Bitte warten …") }
        if (s.path == null) {
            Text("Bild aufnehmen oder Bild/PDF importieren. Die Verarbeitung bleibt lokal.")
            Button(enabled = s.canCapture && !s.busy, onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) camera = true
                else permission.launch(Manifest.permission.CAMERA)
            }, modifier = Modifier.fillMaxWidth()) { Text("Kamera öffnen") }
            OutlinedButton(enabled = s.canCapture && !s.busy, onClick = { picker.launch(arrayOf("image/*", "application/pdf")) }, modifier = Modifier.fillMaxWidth()) { Text("Bild / PDF importieren") }
            if (!s.canCapture && !s.busy) Text("Neue Belege pausieren. Kaufen oder Käufe wiederherstellen findest du in den Einstellungen.")
        } else {
            Text(if (s.path!!.endsWith(".pdf")) "PDF bleibt als Originalanhang erhalten. Felder bitte manuell bestätigen." else "OCR ist ein Vorschlag. Bitte alle Felder kontrollieren.")
            if (!s.path!!.endsWith(".pdf")) OriginalPreview(s.path!!, viewModel)
            listOf("merchant" to ("Händler" to s.merchant), "date" to ("Datum (JJJJ-MM-TT)" to s.date),
                "total" to ("Summe in Euro" to s.total), "category" to ("Kategorie" to s.category)).forEach { (key, field) ->
                OutlinedTextField(value = field.second, onValueChange = { viewModel.edit(key, it) }, label = { Text(field.first) }, enabled = !s.busy, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = if (key == "total") KeyboardType.Decimal else KeyboardType.Text), modifier = Modifier.fillMaxWidth())
            }
            Text("Positionen optional: je Zeile Bezeichnung;Menge;Einzelpreis. Die Summe muss stimmen. Beispiel: Bohrer;2;12,99")
            OutlinedTextField(value = s.positions, onValueChange = { viewModel.edit("positions", it) }, label = { Text("Manuell geprüfte Positionen") }, enabled = !s.busy, minLines = 2, maxLines = 6, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = s.text, onValueChange = { viewModel.edit("text", it) }, label = { Text("Erkannter Text / Notiz") }, enabled = !s.busy, minLines = 3, maxLines = 8, modifier = Modifier.fillMaxWidth())
            Button(onClick = viewModel::save, enabled = !s.busy, modifier = Modifier.fillMaxWidth()) { Text("Geprüft speichern") }
            OutlinedButton(onClick = { confirmDiscard = true }, enabled = !s.busy, modifier = Modifier.fillMaxWidth()) { Text("Entwurf verwerfen") }
        }
    }
}

@Composable
private fun OriginalPreview(path: String, viewModel: CaptureViewModel) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val file = viewModel.original(path)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 1
                    while (bounds.outWidth / inSampleSize > 1600 || bounds.outHeight / inSampleSize > 1600) inSampleSize *= 2
                }
                BitmapFactory.decodeFile(file.path, options)
            }.getOrNull()
        }
    }
    bitmap?.let { Image(it.asImageBitmap(), contentDescription = "Originalbeleg zum Vergleich mit den erkannten Feldern", modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)) }
        ?: Text("Bildvorschau nicht verfügbar; Original kann nach dem Speichern geöffnet werden.")
}
