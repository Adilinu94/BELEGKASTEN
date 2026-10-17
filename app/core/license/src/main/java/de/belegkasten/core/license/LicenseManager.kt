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
import de.belegkasten.core.database.ReceiptDao

private val Context.licenseStore by preferencesDataStore("license")

// ADR-008 as code: two states — archive usable (always) vs capture enabled.
// Trial: 30 days OR 50 receipts, whichever comes first. Reading, searching,
// export and backup NEVER require a license. Only capture/import/analysis pause.
@Singleton
class LicenseManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val receipts: ReceiptDao,
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
            p[RECEIPT_COUNT] = ((p[RECEIPT_COUNT] ?: 0).toLong() + 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        }
    }

    // Always true: archive stays readable without payment (ADR-008).
    fun archiveUsable(): Boolean = true

    suspend fun captureEnabled(): Boolean {
        return status().captureEnabled
    }

    suspend fun markPurchased() {
        context.licenseStore.edit { it[PURCHASED] = true }
    }

    // Only a successful explicit Play query may invalidate the offline cache.
    suspend fun clearVerifiedMissingPurchase() {
        context.licenseStore.edit { it[PURCHASED] = false }
    }

    suspend fun trialStatus(): String {
        return status().note
    }

    suspend fun status(): TrialStatus {
        ensureFirstStart()
        val p = context.licenseStore.data.first()
        // DB count survives a process death immediately after commit and cannot
        // be bypassed by the UI. Restoring is always allowed, even past 50.
        return trialStatus(p[PURCHASED] == true, p[FIRST_START]!!,
            System.currentTimeMillis(), maxOf(p[RECEIPT_COUNT] ?: 0, receipts.count()))
    }
}
