package de.belegkasten.core.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// No query text, merchant, OCR, receipt IDs, file paths, purchase tokens or logs.
@Singleton
class Diagnostics @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs = context.getSharedPreferences("diagnostic_aggregates", Context.MODE_PRIVATE)
    @Synchronized fun record(event: String) {
        require(event in setOf("capture", "search", "search_hit", "backup", "restore", "error"))
        val key = java.time.LocalDate.now().toString() + ":" + event
        prefs.edit().putLong(key, (prefs.getLong(key, 0) + 1).coerceAtLeast(0)).apply()
        val cutoff = java.time.LocalDate.now().minusDays(30).toString()
        val old = prefs.all.keys.filter { it.substringBefore(':') < cutoff }
        if (old.isNotEmpty()) prefs.edit().also { editor -> old.forEach(editor::remove) }.apply()
    }
    suspend fun export(target: Uri) = withContext(Dispatchers.IO) {
        val text = buildString {
            append("BELEGKASTEN Diagnose — freiwilliger Export, nur lokale Tageszählungen\n")
            append("Android SDK: ${android.os.Build.VERSION.SDK_INT}\n")
            val cutoff = java.time.LocalDate.now().minusDays(30).toString()
            prefs.all.toSortedMap().filterKeys { it.substringBefore(':') >= cutoff }.forEach { (key, value) -> append("$key=$value\n") }
        }
        requireNotNull(context.contentResolver.openOutputStream(target, "wt")).use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }
}
