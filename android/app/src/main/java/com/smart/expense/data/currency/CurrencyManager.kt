package com.smart.expense.data.currency

import java.util.Locale

data class CurrencyModel(
    val code: String,
    val symbol: String,
    val name: String,
    val exchangeRateFromInr: Double // Conversion multiplier from base INR
)

object CurrencyManager {

    val CURRENCIES = listOf(
        CurrencyModel("INR", "₹", "Indian Rupee", 1.0),
        CurrencyModel("USD", "$", "US Dollar", 0.012),
        CurrencyModel("EUR", "€", "Euro", 0.011),
        CurrencyModel("GBP", "£", "British Pound", 0.0095),
        CurrencyModel("AED", "د.إ", "UAE Dirham", 0.044),
        CurrencyModel("JPY", "¥", "Japanese Yen", 1.85)
    )

    fun getCurrency(code: String): CurrencyModel {
        return CURRENCIES.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: CURRENCIES[0]
    }

    fun format(amountInInr: Double, currencyCode: String = "INR"): String {
        val curr = getCurrency(currencyCode)
        val converted = amountInInr * curr.exchangeRateFromInr
        return when (curr.code) {
            "JPY" -> "${curr.symbol}${String.format(Locale.US, "%,.0f", converted)}"
            else -> "${curr.symbol}${String.format(Locale.US, "%,.2f", converted)}"
        }
    }
}

