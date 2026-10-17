package de.belegkasten.core.ui

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

fun formatCents(cents: Long): String = NumberFormat.getCurrencyInstance(Locale.GERMANY)
    .format(BigDecimal.valueOf(cents, 2))
