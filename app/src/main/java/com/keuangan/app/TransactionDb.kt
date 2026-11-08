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

// ponytail: SQLiteOpenHelper used over Room. Upgrade to Room if reactive flows, complex joins, or automated schema migrations are required.
class TransactionDb(context: Context) : SQLiteOpenHelper(context, "keuangan.db", null, 3) {

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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            db.execSQL("DROP TABLE IF EXISTS transactions")
            db.execSQL("DROP TABLE IF EXISTS wallets")
            onCreate(db)
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
