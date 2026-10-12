package de.belegkasten.feature.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.belegkasten.core.license.LicenseManager
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Capture screen: camera auto-capture per receipt (CameraX), share-intent and
// gallery import handled by MainActivity -> this ViewModel. Capture is blocked
// when the trial ended; archive/search stay available (ADR-008).
@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val license: LicenseManager,
) : ViewModel() {
    private val _state = MutableStateFlow(CaptureState())
    val state: StateFlow<CaptureState> = _state

    data class CaptureState(
        val canCapture: Boolean = true,
        val trialNote: String = "",
        val lastError: String? = null,
    )

    suspend fun refresh() {
        license.ensureFirstStart()
        _state.value = CaptureState(
            canCapture = license.captureEnabled(),
            trialNote = license.trialStatus(),
        )
    }
}

@Composable
fun CaptureRoute(viewModel: CaptureViewModel = hiltViewModel()) {
    // CameraX PreviewView is bound in the AndroidView below once hardware is
    // attached; until then this placeholder keeps build + preview green.
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Erfassen")
        Text("Beleg vor die Kamera halten — Aufnahme erfolgt automatisch.")
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Aufnehmen") }
        OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Aus Galerie / Datei") }
    }
}
