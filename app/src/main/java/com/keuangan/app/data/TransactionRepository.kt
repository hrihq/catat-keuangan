package com.keuangan.app.data

import android.content.Context
import com.keuangan.app.TransactionDb
import com.keuangan.app.Goal
import com.keuangan.app.TransactionItem
import com.keuangan.app.Wallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository: single source for the app screens.
 * Reads the local database (starts empty on a fresh install). Replace the bodies
 * with API calls when the backend is live — the UI and models stay identical.
 */
class TransactionRepository(private val context: Context) {

    private val db = TransactionDb(context)

    suspend fun getTransactions(): List<TransactionItem> = withContext(Dispatchers.IO) {
        db.getAll()
    }

    suspend fun getSummary(): Triple<Double, Double, Double> = withContext(Dispatchers.IO) {
        db.getSummary()
    }

    suspend fun getBudget(): TransactionParser.Budget = withContext(Dispatchers.IO) {
        val expense = db.getSummary().second
        val month = SimpleDateFormat("MMM yyyy", Locale("id", "ID")).format(Date())
        TransactionParser.Budget(limit = 5_000_000.0, spent = expense, month = month)
    }

    /** Total pengeluaran 7 hari terakhir, urut Sen..Min. */
    suspend fun getWeeklyExpense(): List<Double> = withContext(Dispatchers.IO) {
        db.getWeeklyExpense()
    }

    suspend fun getCategoryExpenses(): List<Pair<String, Double>> = withContext(Dispatchers.IO) {
        db.getCategoryExpenses()
    }

    suspend fun getWallets(): List<Wallet> = withContext(Dispatchers.IO) {
        db.getWallets()
    }

    suspend fun getWalletBalance(name: String): Double = withContext(Dispatchers.IO) {
        db.getWalletBalance(name)
    }

    suspend fun addWallet(name: String, initialBalance: Double): Unit =
        withContext(Dispatchers.IO) { db.addWallet(name, initialBalance) }

    suspend fun deleteWallet(id: Long): Unit =
        withContext(Dispatchers.IO) { db.deleteWallet(id) }

    suspend fun getGoals(): List<Goal> = withContext(Dispatchers.IO) { db.getGoals() }

    suspend fun addGoal(name: String, targetAmount: Double, savedAmount: Double, icon: String): Unit =
        withContext(Dispatchers.IO) { db.addGoal(name, targetAmount, savedAmount, icon) }

    suspend fun deleteGoal(id: Long): Unit =
        withContext(Dispatchers.IO) { db.deleteGoal(id) }

    suspend fun deleteTransaction(id: Long): Unit =
        withContext(Dispatchers.IO) { db.delete(id) }

    suspend fun addTransaction(
        title: String,
        amount: Double,
        isIncome: Boolean,
        category: String,
        note: String,
        wallet: String,
        dateMs: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        db.insert(title, amount, isIncome, category, note, wallet, dateMs)
    }
}
