package br.com.terras.app.common

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs


fun Double.formatMoney(locale: Locale = Locale.getDefault()): String =
    DecimalFormat.getCurrencyInstance(locale).format(this)

fun Double.formatAxisMoney(locale: Locale = Locale.getDefault()): String {
    val formatter = DecimalFormat.getCurrencyInstance(locale)
    val absValue = abs(this)
    when {
        absValue >= 1000.0 -> {
            formatter.maximumFractionDigits = 0
            formatter.minimumFractionDigits = 0
        }
        absValue >= 1.0 -> {
            formatter.maximumFractionDigits = 2
            formatter.minimumFractionDigits = 2
        }
        absValue >= 0.01 -> {
            formatter.maximumFractionDigits = 4
            formatter.minimumFractionDigits = 2
        }
        else -> {
            formatter.maximumFractionDigits = 6
            formatter.minimumFractionDigits = 2
        }
    }
    return formatter.format(this)
}

fun Double.formatPercentage(locale: Locale = Locale.getDefault()): String {
    val symbols = DecimalFormatSymbols(locale)
    val formatter = DecimalFormat("+#,##0.0;-#,##0.0", symbols)
    return formatter.format(this) + "%"
}

fun Double.formatCryptoQuantity(locale: Locale = Locale.getDefault()): String {
    val symbols = DecimalFormatSymbols(locale)
    val maxDecimals = when {
        abs(this) >= 100.0 -> 4
        abs(this) >= 1.0 -> 6
        else -> 8
    }
    val pattern = "#,##0." + "#".repeat(maxDecimals)
    return DecimalFormat(pattern, symbols).format(this)
}

fun String.toFormatDate(zoneId: ZoneId = ZoneId.systemDefault()): String =
    Instant.parse(this).atZone(zoneId).toLocalDateTime()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

fun Long.toFormatDate(zoneId: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(this).atZone(zoneId).toLocalDateTime()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

fun Long.toChartAxisTime(spanHours: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
    val pattern = when {
        spanHours <= 48L -> "HH:mm"
        spanHours <= 24L * 120L -> "dd/MM"
        else -> "MMM/yy"
    }
    return Instant.ofEpochMilli(this)
        .atZone(zoneId)
        .toLocalDateTime()
        .format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

fun Long.toChartHour(zoneId: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(this)
        .atZone(zoneId)
        .toLocalDateTime()
        .format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))

fun Long.toChartDay(zoneId: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(this)
        .atZone(zoneId)
        .toLocalDateTime()
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()))

fun List<Double>.downsampleSparkline(maxPoints: Int = 24): List<Double> {
    if (size <= maxPoints || maxPoints < 2) return this
    val lastIdx = lastIndex
    return List(maxPoints) { i ->
        val mappedIndex = ((i.toFloat() / (maxPoints - 1)) * lastIdx).toInt().coerceIn(0, lastIdx)
        this[mappedIndex]
    }
}