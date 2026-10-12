package de.belegkasten.feature.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.util.concurrent.TimeUnit

// Settings: encrypted backup export/import, license state, trial info,
// warranty defaults per category. Backup/export stay available without
// license (ADR-008).

@Composable
fun SettingsRoute() {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Einstellungen")
        Button(onClick = {}) { Text("Sicherung exportieren (verschlüsselt)") }
        OutlinedButton(onClick = {}) { Text("Sicherung wiederherstellen") }
        Text("Ohne Kauf bleibt alles lesbar — nur neue Belege pausieren.")
        Text("Aufbewahrung ist eine Gedächtnisstütze, keine GoBD-Ablage.")
    }
}

// Daily local check: return/warranty deadlines + retention hints.
// Local notification only, no network.
class DeadlineWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val today = LocalDate.now().toString()
        // Query DeadlineDao.dueUntil(today + 21 days), notify per item, mark notified.
        // Wired in Application.onCreate via schedule().
        return Result.success()
    }

    companion object {
        const val NAME = "deadline-check"
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<DeadlineWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME, ExistingPeriodicWorkPolicy.KEEP, req,
            )
        }
    }
}
