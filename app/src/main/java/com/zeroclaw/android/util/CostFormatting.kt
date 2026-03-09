// Copyright 2026 ZeroClaw Community, MIT License

package com.zeroclaw.android.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Default monthly budget cap in USD when no user-configured limit is set. */
internal const val DEFAULT_MONTHLY_BUDGET_USD = 50.0

/** Progress ratio threshold at which the budget indicator turns the warning colour. */
internal const val BUDGET_WARNING_THRESHOLD = 0.8f

/** Maximum progress ratio for budget indicators, clamped to 1.0. */
internal const val MAX_PROGRESS = 1.0f

/**
 * Formats a USD cost value into a human-readable dollar string.
 *
 * Uses the current locale's currency formatting rules while forcing
 * the currency unit to USD.
 *
 * @param usd Cost in US dollars.
 * @return Formatted cost string.
 */
internal fun formatUsd(usd: Double): String =
    NumberFormat
        .getCurrencyInstance(Locale.getDefault())
        .apply {
            currency = Currency.getInstance("USD")
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(usd)
