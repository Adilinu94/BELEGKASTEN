package de.belegkasten.core.license

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.licenseStore by preferencesDataStore("license")

// ADR-008 as code: two states — archive usable (always) vs capture enabled.
// Trial: 30 days OR 50 receipts, whichever comes first. Reading, searching,
// export and backup NEVER require a license. Only capture/import/analysis pause.
@Singleton
class LicenseManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val TRIAL_DAYS = 30L
        const val TRIAL_RECEIPTS = 50
        const val SKU_LIFETIME = "belegkasten_lifetime"
    }

    private val PURCHASED = booleanPreferencesKey("purchased")
    private val FIRST_START = longPreferencesKey("first_start_ms")
    private val RECEIPT_COUNT = intPreferencesKey("receipt_count")

    val purchased: Flow<Boolean> = context.licenseStore.data.map { it[PURCHASED] == true }

    suspend fun ensureFirstStart() {
        context.licenseStore.edit { p ->
            if (p[FIRST_START] == null) p[FIRST_START] = System.currentTimeMillis()
        }
    }

    suspend fun recordReceiptAdded() {
        context.licenseStore.edit { p ->
            p[RECEIPT_COUNT] = (p[RECEIPT_COUNT] ?: 0) + 1
        }
    }

    // Always true: archive stays readable without payment (ADR-008).
    fun archiveUsable(): Boolean = true

    suspend fun captureEnabled(): Boolean {
        val p = context.licenseStore.data.first()
        if (p[PURCHASED] == true) return true
        val first = p[FIRST_START] ?: System.currentTimeMillis()
        val daysUsed = (System.currentTimeMillis() - first) / 86_400_000L
        val count = p[RECEIPT_COUNT] ?: 0
        return daysUsed < TRIAL_DAYS && count < TRIAL_RECEIPTS
    }

    suspend fun markPurchased() {
        context.licenseStore.edit { it[PURCHASED] = true }
    }

    suspend fun trialStatus(): String {
        val p = context.licenseStore.data.first()
        if (p[PURCHASED] == true) return "gekauft"
        val first = p[FIRST_START] ?: System.currentTimeMillis()
        val daysLeft = TRIAL_DAYS - (System.currentTimeMillis() - first) / 86_400_000L
        val left = TRIAL_RECEIPTS - (p[RECEIPT_COUNT] ?: 0)
        return "Testphase: noch $daysLeft Tage oder $left Belege"
    }
}
