package de.belegkasten.core.license

data class TrialStatus(val purchased: Boolean, val daysLeft: Long, val receiptsLeft: Int) {
    val captureEnabled: Boolean get() = purchased || (daysLeft > 0 && receiptsLeft > 0)
    val note: String get() = if (purchased) "Einmalkauf aktiv" else
        "Testphase: noch $daysLeft Tage oder $receiptsLeft Belege. Archiv bleibt frei."
}

fun trialStatus(purchased: Boolean, firstStart: Long, now: Long, count: Int): TrialStatus {
    // Never show more than 30 days; offline clock tamper resistance is not claimed.
    val elapsed = (now - firstStart).coerceAtLeast(0)
    val days = (30 - elapsed / 86_400_000L).coerceIn(0, 30)
    return TrialStatus(purchased, days, (50 - count.coerceAtLeast(0)).coerceAtLeast(0))
}
