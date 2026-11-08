package com.keuangan.app.ui.home

import com.keuangan.app.TransactionItem
import com.keuangan.app.data.TransactionParser

/** Parsed, ready-to-render payload for the Home screen. */
data class HomeData(
    val balance: Double,
    val income: Double,
    val expense: Double,
    val budget: TransactionParser.Budget,
    val transactions: List<TransactionItem>,
    val weeklyExpense: List<Double> = emptyList()
)

/** UI state machine for the Home screen. */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val data: HomeData) : HomeUiState
    data object Empty : HomeUiState
    data class Error(val message: String) : HomeUiState
}
