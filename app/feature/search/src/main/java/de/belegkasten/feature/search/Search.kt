package de.belegkasten.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.belegkasten.core.model.SearchHit
import de.belegkasten.core.search.SearchEngine
import de.belegkasten.core.data.Diagnostics
import de.belegkasten.core.ui.formatCents
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

data class SearchState(val query: String = "", val hits: List<SearchHit> = emptyList(), val busy: Boolean = false, val error: String? = null)

@HiltViewModel
class SearchViewModel @Inject constructor(private val engine: SearchEngine, private val saved: SavedStateHandle, private val diagnostics: Diagnostics) : ViewModel() {
    val state = MutableStateFlow(SearchState(query = saved["query"] ?: ""))
    private var job: Job? = null
    init { query(state.value.query) }
    fun query(text: String) {
        job?.cancel()
        val q = text.take(500)
        saved["query"] = q
        state.value = SearchState(query = q, busy = q.isNotBlank())
        if (q.isBlank()) return
        job = viewModelScope.launch {
            delay(250)
            try {
                val hits = withContext(Dispatchers.IO) { engine.search(q) }
                state.value = SearchState(q, hits)
                runCatching {
                    diagnostics.record("search")
                    if (hits.isNotEmpty()) diagnostics.record("search_hit")
                }
            } catch (e: Exception) { if (e is CancellationException) throw e; state.value = SearchState(q, error = "Suche fehlgeschlagen. Bitte erneut versuchen.") }
        }
    }
}

@Composable
fun SearchRoute(onOpenReceipt: (String) -> Unit, viewModel: SearchViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Suchen", style = MaterialTheme.typography.headlineSmall)
        Text("Lokale Text- und Feldsuche. Semantische und Foto-Suche sind noch nicht freigegeben.")
        OutlinedTextField(value = s.query, onValueChange = viewModel::query, label = { Text("Händler, Artikel, Monat oder Jahr") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        if (s.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!s.busy && s.query.isNotBlank() && s.hits.isEmpty() && s.error == null) Text("Keine passenden Belege. Versuche ein Wort aus dem Bontext.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(s.hits, key = { it.receiptId }) { hit ->
                OutlinedButton(onClick = { onOpenReceipt(hit.receiptId) }, modifier = Modifier.fillMaxWidth()) {
                    Text("${hit.merchant} · ${hit.date.value} · ${formatCents(hit.total.value)}")
                }
            }
        }
    }
}
