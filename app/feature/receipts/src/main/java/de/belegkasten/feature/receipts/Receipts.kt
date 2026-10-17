package de.belegkasten.feature.receipts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxWidth
import de.belegkasten.core.ui.formatCents
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.belegkasten.core.data.ReceiptRepository
import de.belegkasten.core.model.Receipt
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.catch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Receipt list + confirm screen (fields flagged by CheckBeforeSave).
// Read-only access never requires a license (ADR-008).

sealed interface ReceiptsUiState {
    data object Loading : ReceiptsUiState
    data class Success(val items: List<Receipt>) : ReceiptsUiState
    data class Error(val message: String) : ReceiptsUiState
}

@HiltViewModel
class ReceiptsViewModel @Inject constructor(
    repo: ReceiptRepository,
) : ViewModel() {
    val uiState = repo.observeAll()
        .map { ReceiptsUiState.Success(it) as ReceiptsUiState }
        .catch { emit(ReceiptsUiState.Error("Archiv konnte nicht geladen werden")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReceiptsUiState.Loading)
}

@Composable
fun ReceiptsRoute(onOpenReceipt: (String) -> Unit, viewModel: ReceiptsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Belege", style = MaterialTheme.typography.headlineSmall)
        when (val s = state) {
            is ReceiptsUiState.Loading -> Text("Lädt …")
            is ReceiptsUiState.Error -> Text(s.message)
            is ReceiptsUiState.Success -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (s.items.isEmpty()) item { Text("Noch keine Belege. Über Erfassen ein Bild oder PDF hinzufügen.") }
                items(s.items, key = { it.id }) { r ->
                    OutlinedButton(onClick = { onOpenReceipt(r.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("${r.merchant} · ${r.date.value} · ${formatCents(r.total.value)}")
                    }
                }
            }
        }
    }
}
