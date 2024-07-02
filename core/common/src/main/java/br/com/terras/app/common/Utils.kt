package br.com.terras.app.common

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale


fun Double.formatMoney(locale: Locale = Locale.getDefault()): String =
    DecimalFormat.getCurrencyInstance(locale).format(this)

fun Double.formatPercentage(locale: Locale = Locale.getDefault()): String {
    val symbols = DecimalFormatSymbols(locale)
    val formatter = DecimalFormat("+#,##0.0;-#,##0.0", symbols)
    return formatter.format(this) + "%"
}

fun String.toFormatDate(zoneId: ZoneId = ZoneId.systemDefault()): String =
    Instant.parse(this).atZone(zoneId).toLocalDateTime()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

fun Long.toFormatDate(zoneId: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(this).atZone(zoneId).toLocalDateTime()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))