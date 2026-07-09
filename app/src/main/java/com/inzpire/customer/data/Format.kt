package com.inzpire.customer.data

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

/** Ports `src/lib/inzpire/format.ts` so amounts read identically to the web app. */

fun formatINR(n: Double?): String {
    if (n == null) return "—"
    val rounded = n.roundToLong()
    val grouped = groupIndianDigits(kotlin.math.abs(rounded))
    val sign = if (rounded < 0) "-" else ""
    return "$sign₹$grouped"
}

fun formatINRCompact(n: Double?): String {
    if (n == null) return "—"
    val crore = 1_00_00_000.0
    val lakh = 1_00_000.0
    return when {
        n >= crore -> "₹${"%.2f".format(Locale.US, n / crore)} Cr"
        n >= lakh -> "₹${"%.2f".format(Locale.US, n / lakh)} L"
        n >= 1_000.0 -> "₹${"%.1f".format(Locale.US, n / 1_000.0)}K"
        else -> "₹${n.roundToLong()}"
    }
}

/** Indian digit grouping (last 3 digits, then groups of 2): 1,00,00,000 */
private fun groupIndianDigits(value: Long): String {
    val s = value.toString()
    if (s.length <= 3) return s
    val head = s.substring(0, s.length - 3)
    val tail = s.substring(s.length - 3)
    val grouped = StringBuilder()
    var i = head.length
    while (i > 0) {
        val start = maxOf(0, i - 2)
        grouped.insert(0, head.substring(start, i))
        if (start > 0) grouped.insert(0, ",")
        i = start
    }
    return "$grouped,$tail"
}

private val SHORT_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val LONG_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

fun formatShortDate(isoTimestamp: String?): String? {
    if (isoTimestamp == null) return null
    return runCatching { OffsetDateTime.parse(isoTimestamp).format(SHORT_DATE_FORMATTER) }.getOrNull()
}

fun formatLongDate(isoTimestamp: String?): String? {
    if (isoTimestamp == null) return null
    return runCatching { OffsetDateTime.parse(isoTimestamp).format(LONG_DATE_FORMATTER) }.getOrNull()
}
