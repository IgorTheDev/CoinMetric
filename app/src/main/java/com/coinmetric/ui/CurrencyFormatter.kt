package com.coinmetric.ui

import java.text.NumberFormat
import java.util.Locale

private val rubLocale = Locale("ru", "RU")

fun Int.toRubCurrency(): String = NumberFormat.getCurrencyInstance(rubLocale).format(this)

