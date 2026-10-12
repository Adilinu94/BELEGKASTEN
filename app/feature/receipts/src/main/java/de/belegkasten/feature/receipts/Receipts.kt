package de.belegkasten.feature.receipts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
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

// Receipt list + confirm screen (fields flagged by CheckBeforeSave).
// Read-only access never requires a license (ADR-008).

sealed interface ReceiptsUiState {
    data object Loading : ReceiptsUiState
    data class Success(val items: List<Receipt>) : ReceiptsUiState
}

@HiltViewModel
class ReceiptsViewModel @Inject constructor(
    repo: ReceiptRepository,
) : ViewModel() {
    val uiState = repo.observeAll()
        .map { ReceiptsUiState.Success(it) as ReceiptsUiState }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReceiptsUiState.Loading)
}

@Composable
fun ReceiptsRoute(viewModel: ReceiptsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Belege")
        when (val s = state) {
            is ReceiptsUiState.Loading -> Text("Lädt …")
            is ReceiptsUiState.Success -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(s.items) { r ->
                    Text("${r.merchant} · ${r.date.value} · ${r.total.value / 100}.${"%02d".format(r.total.value % 100)} €")
                }
            }
        }
    }
}
