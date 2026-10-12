package de.belegkasten.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.belegkasten.core.model.SearchHit
import de.belegkasten.core.search.SearchEngine
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Word search ("der vom Baumarkt im März"). Photo search is a beta feature
// behind a flag and only ships if the spike passes (Bauplan 0.4).
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val engine: SearchEngine,
) : ViewModel() {
    private val _hits = MutableStateFlow<List<SearchHit>>(emptyList())
    val hits: StateFlow<List<SearchHit>> = _hits

    suspend fun query(text: String) {
        if (text.isBlank()) { _hits.value = emptyList(); return }
        _hits.value = engine.search(text)
    }
}

@Composable
fun SearchRoute(viewModel: SearchViewModel = hiltViewModel()) {
    var q by remember { mutableStateOf("") }
    val hits by viewModel.hits.collectAsState()
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Suchen")
        OutlinedTextField(
            value = q,
            onValueChange = { q = it; scope.launch { viewModel.query(it) } },
            label = { Text("Beschreib den Beleg …") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(hits) { h ->
                Text("${h.merchant} · ${h.date.value} · ${h.total.value / 100}.${"%02d".format(h.total.value % 100)} €")
            }
        }
    }
}
