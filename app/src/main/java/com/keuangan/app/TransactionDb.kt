package com.keuangan.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TransactionItem(
    val id: Long,
    val title: String,
    val amount: Double,
    val isIncome: Boolean,
    val category: String = "Makanan",
    val date: String,
    val note: String = "",
    val wallet: String = "Tunai",
    val dateMs: Long = 0L
)

data class Wallet(
    val id: Long,
    val name: String,
    val initialBalance: Double
)

data class Goal(
    val id: Long,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val icon: String = "target"
) {
    val progress: Int
        get() = if (targetAmount > 0) ((savedAmount / targetAmount) * 100).toInt().coerceIn(0, 100) else 0
    val isReached: Boolean
        get() = targetAmount > 0 && savedAmount >= targetAmount
}

data class GoalSaving(
    val id: Long,
    val goalId: Long,
    val amount: Double,
    val note: String,
    val dateMs: Long
)

// ponytail: SQLiteOpenHelper used over Room. Upgrade to Room if reactive flows, complex joins, or automated schema migrations are required.
class TransactionDb(context: Context) : SQLiteOpenHelper(context, "keuangan.db", null, 5) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                amount REAL NOT NULL,
                is_income INTEGER NOT NULL,
                category TEXT NOT NULL DEFAULT 'Makanan',
                date TEXT NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                wallet TEXT NOT NULL DEFAULT 'Tunai',
                date_ms INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE wallets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                initial_balance REAL NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                target_amount REAL NOT NULL,
                saved_amount REAL NOT NULL DEFAULT 0,
                icon TEXT NOT NULL DEFAULT 'target'
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE goal_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                goal_id INTEGER NOT NULL,
                amount REAL NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                date_ms INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            db.execSQL("DROP TABLE IF EXISTS transactions")
            db.execSQL("DROP TABLE IF EXISTS wallets")
            onCreate(db)
        } else {
            if (oldVersion < 4) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        target_amount REAL NOT NULL,
                        saved_amount REAL NOT NULL DEFAULT 0,
                        icon TEXT NOT NULL DEFAULT 'target'
                    )
                    """.trimIndent()
                )
            }
            if (oldVersion < 5) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS goal_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        goal_id INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        note TEXT NOT NULL DEFAULT '',
                        date_ms INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }
    }

    fun insert(
        title: String,
        amount: Double,
        isIncome: Boolean,
        category: String = "Makanan",
        note: String = "",
        wallet: String = "Tunai",
        dateMs: Long = System.currentTimeMillis()
    ): Long {
        val values = ContentValues().apply {
            put("title", title)
            put("amount", amount)
            put("is_income", if (isIncome) 1 else 0)
            put("category", category)
            put("date", formatDate(dateMs))
            put("note", note)
            put("wallet", wallet)
            put("date_ms", dateMs)
        }
        return writableDatabase.insert("transactions", null, values)
    }

    fun getAll(): List<TransactionItem> {
        val list = mutableListOf<TransactionItem>()
        val cursor = readableDatabase.rawQuery(
            "SELECT id, title, amount, is_income, category, date, note, wallet, date_ms " +
                "FROM transactions ORDER BY date_ms DESC, id DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                val dateMs = it.getLong(8)
                list.add(
                    TransactionItem(
                        id = it.getLong(0),
                        title = it.getString(1),
                        amount = it.getDouble(2),
                        isIncome = it.getInt(3) == 1,
                        category = it.getString(4) ?: "Makanan",
                        date = formatDate(dateMs),
                        note = it.getString(6) ?: "",
                        wallet = it.getString(7) ?: "Tunai",
                        dateMs = dateMs
                    )
                )
            }
        }
        return list
    }

    fun delete(id: Long): Int =
        writableDatabase.delete("transactions", "id = ?", arrayOf(id.toString()))

    fun getSummary(): Triple<Double, Double, Double> {
        var income = 0.0
        var expense = 0.0
        val cursor = readableDatabase.rawQuery(
            "SELECT is_income, SUM(amount) FROM transactions GROUP BY is_income",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                if (it.getInt(0) == 1) income = it.getDouble(1) else expense = it.getDouble(1)
            }
        }
        return Triple(income, expense, income - expense)
    }

    fun getCategoryExpenses(): List<Pair<String, Double>> {
        val list = mutableListOf<Pair<String, Double>>()
        val cursor = readableDatabase.rawQuery(
            "SELECT category, SUM(amount) FROM transactions WHERE is_income = 0 " +
                "GROUP BY category ORDER BY SUM(amount) DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) list.add(Pair(it.getString(0) ?: "Lainnya", it.getDouble(1)))
        }
        return list
    }

    fun getWallets(): List<Wallet> {
        val list = mutableListOf<Wallet>()
        val cursor = readableDatabase.rawQuery(
            "SELECT id, name, initial_balance FROM wallets ORDER BY id ASC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(Wallet(it.getLong(0), it.getString(1) ?: "", it.getDouble(2)))
            }
        }
        return list
    }

    fun addWallet(name: String, initialBalance: Double = 0.0): Long {
        val values = ContentValues().apply {
            put("name", name)
            put("initial_balance", initialBalance)
        }
        return writableDatabase.insert("wallets", null, values)
    }

    fun deleteWallet(id: Long): Int =
        writableDatabase.delete("wallets", "id = ?", arrayOf(id.toString()))

    // --- Target keuangan ---
    fun getGoals(): List<Goal> {
        val list = mutableListOf<Goal>()
        readableDatabase.rawQuery(
            "SELECT id, name, target_amount, saved_amount, icon FROM goals ORDER BY id ASC",
            null
        ).use {
            while (it.moveToNext()) {
                list.add(
                    Goal(
                        id = it.getLong(0),
                        name = it.getString(1) ?: "",
                        targetAmount = it.getDouble(2),
                        savedAmount = it.getDouble(3),
                        icon = it.getString(4) ?: "target"
                    )
                )
            }
        }
        return list
    }

    fun addGoal(name: String, targetAmount: Double, savedAmount: Double = 0.0, icon: String = "target"): Long {
        val values = ContentValues().apply {
            put("name", name)
            put("target_amount", targetAmount)
            put("saved_amount", savedAmount)
            put("icon", icon)
        }
        return writableDatabase.insert("goals", null, values)
    }

    fun deleteGoal(id: Long): Int =
        writableDatabase.delete("goals", "id = ?", arrayOf(id.toString()))

    /** Tambah tabungan ke target (tabel goals + riwayat). */
    fun addGoalSaving(
        id: Long,
        amount: Double,
        note: String = "",
        dateMs: Long = System.currentTimeMillis()
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL(
                "UPDATE goals SET saved_amount = saved_amount + ? WHERE id = ?",
                arrayOf(amount, id)
            )
            val values = ContentValues().apply {
                put("goal_id", id)
                put("amount", amount)
                put("note", note)
                put("date_ms", dateMs)
            }
            db.insert("goal_history", null, values)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getGoalHistory(goalId: Long): List<GoalSaving> {
        val list = mutableListOf<GoalSaving>()
        readableDatabase.rawQuery(
            "SELECT id, goal_id, amount, note, date_ms FROM goal_history " +
                "WHERE goal_id = ? ORDER BY date_ms DESC, id DESC",
            arrayOf(goalId.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    GoalSaving(
                        id = cursor.getLong(0),
                        goalId = cursor.getLong(1),
                        amount = cursor.getDouble(2),
                        note = cursor.getString(3) ?: "",
                        dateMs = cursor.getLong(4)
                    )
                )
            }
        }
        return list
    }

    fun updateGoal(id: Long, name: String, targetAmount: Double): Int {
        val values = ContentValues().apply {
            put("name", name)
            put("target_amount", targetAmount)
        }
        return writableDatabase.update("goals", values, "id = ?", arrayOf(id.toString()))
    }

    /** Pengeluaran 7 hari terakhir per hari (Sen..Min). */
    fun getWeeklyExpense(): List<Double> {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val dayMs = 24 * 60 * 60 * 1000L
        val start = calendar.timeInMillis - 6 * dayMs
        val result = MutableList(7) { 0.0 }
        readableDatabase.rawQuery(
            "SELECT date_ms, SUM(amount) FROM transactions " +
                "WHERE is_income = 0 AND date_ms >= ? GROUP BY date_ms",
            arrayOf(start.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val dayIndex = ((cursor.getLong(0) - start) / dayMs).toInt()
                if (dayIndex in 0..6) result[dayIndex] = cursor.getDouble(1)
            }
        }
        return result
    }

    /** Saldo dompet = saldo awal + semua transaksi pada dompet tersebut. */
    fun getWalletBalance(name: String): Double {
        var total = 0.0
        readableDatabase.rawQuery(
            "SELECT IFNULL(SUM(CASE WHEN is_income = 1 THEN amount ELSE -amount END), 0) " +
                "FROM transactions WHERE wallet = ?",
            arrayOf(name)
        ).use { if (it.moveToFirst()) total = it.getDouble(0) }
        val initial = getWallets().firstOrNull { it.name == name }?.initialBalance ?: 0.0
        return initial + total
    }

    companion object {

        /** Label hari untuk 7 hari terakhir (urut hari terlama -> hari ini). */
        fun lastSevenDayLabels(): List<String> {
            val names = arrayOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
            val dayMs = 24 * 60 * 60 * 1000L
            val result = mutableListOf<String>()
            for (i in 6 downTo 0) {
                val cal = java.util.Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis() - i * dayMs
                }
                result.add(names[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1])
            }
            return result
        }
        fun formatDate(ms: Long): String {
            val now = Calendar.getInstance()
            val then = Calendar.getInstance().apply { timeInMillis = ms }
            val dayDiff = (now.get(Calendar.YEAR) * 366 + now.get(Calendar.DAY_OF_YEAR)) -
                (then.get(Calendar.YEAR) * 366 + then.get(Calendar.DAY_OF_YEAR))
            return when (dayDiff) {
                0 -> "Hari ini"
                1 -> "Kemarin"
                else -> SimpleDateFormat("d MMM", Locale("id", "ID")).format(Date(ms))
            }
        }

        /**
         * Donut: proporsi per kategori. Bar: 7 nilai Sen..Min, dinormalisasi 0..1.
         */
        fun calculateBalanceHistogram(
            transactions: List<TransactionItem>,
            weeks: Int,
            days: Int
        ): Pair<List<DonutChartView.Slice>, List<Float>> {
            val expenses = transactions.filter { !it.isIncome }
            val categoryTotals = java.util.LinkedHashMap<String, Double>()
            val dailyTotals = java.util.LinkedHashMap<Int, Double>()

            for (t in expenses) {
                categoryTotals.merge(t.category, t.amount) { a, b -> a + b }
                if (t.dateMs > 0) {
                    val day = Calendar.getInstance().apply { timeInMillis = t.dateMs }
                        .get(Calendar.DAY_OF_WEEK)
                    dailyTotals[day] = dailyTotals.getOrDefault(day, 0.0) + t.amount
                }
            }

            val total = categoryTotals.values.sum().coerceAtLeast(1.0)
            val slices = categoryTotals.map { (name, amount) ->
                val color = when (name) {
                    "Makanan" -> R.color.chart_food
                    "Transport" -> R.color.chart_transport
                    "Tagihan" -> R.color.chart_bills
                    else -> R.color.chart_other
                }
                DonutChartView.Slice(((amount / total) * 100.0).toFloat(), color)
            }

            val maxValue = dailyTotals.values.maxOrNull() ?: 0.0
            val dayOrder = intArrayOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
                Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
            )
            val weekly = dayOrder.map { calDay ->
                val value = dailyTotals[calDay] ?: 0.0
                if (maxValue > 0.0) (value / maxValue).toFloat() else 0f
            }
            return Pair(slices, weekly)
        }
    }
}
