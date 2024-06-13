package br.com.terras.app.common

import java.text.DecimalFormat
import java.util.Locale


fun Double.formatMoney(): String =
    DecimalFormat.getCurrencyInstance(Locale.getDefault()).format(this)

fun Double.formatPercentage(): String =
    DecimalFormat("+#,##0,00;-#").format(this) + "%"