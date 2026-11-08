package com.keuangan.app.data

import android.content.Context
import com.keuangan.app.TransactionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Repository: single source for the Home screen.
 * Reads the mock JSON payloads from assets so the UI can be tested without the
 * backend. Replace the body with the real API call when the endpoint is live —
 * the UI and the parsed models stay identical.
 */
class TransactionRepository(private val context: Context) {

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    suspend fun getTransactions(): List<TransactionItem> = withContext(Dispatchers.IO) {
        delay(350) // simulate network latency so the loading state is observable
        TransactionParser.parseTransactions(readAsset("mock/transactions.json"))
    }

    suspend fun getSummary(): Triple<Double, Double, Double> = withContext(Dispatchers.IO) {
        delay(350)
        TransactionParser.parseSummary(readAsset("mock/summary.json"))
    }

    suspend fun getBudget(): TransactionParser.Budget = withContext(Dispatchers.IO) {
        delay(350)
        TransactionParser.parseBudget(readAsset("mock/budget.json"))
    }
}
