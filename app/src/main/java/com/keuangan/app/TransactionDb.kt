package com.keuangan.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TransactionItem(
    val id: Long,
    val title: String,
    val amount: Double,
    val isIncome: Boolean,
    val category: String = "Makanan",
    val date: String,
    val note: String = ""
)

// ponytail: SQLiteOpenHelper used over Room. Upgrade to Room if reactive flows, complex joins, or automated schema migrations are required.
class TransactionDb(context: Context) : SQLiteOpenHelper(context, "keuangan.db", null, 2) {

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
                note TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE transactions ADD COLUMN category TEXT NOT NULL DEFAULT 'Makanan'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            } catch (e: Exception) {
                // columns may already exist
            }
        }
    }

    fun insert(title: String, amount: Double, isIncome: Boolean, category: String = "Makanan", note: String = ""): Long {
        val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID")).format(Date())
        val values = ContentValues().apply {
            put("title", title)
            put("amount", amount)
            put("is_income", if (isIncome) 1 else 0)
            put("category", category)
            put("date", dateStr)
            put("note", note)
        }
        return writableDatabase.insert("transactions", null, values)
    }

    fun getAll(): List<TransactionItem> {
        val list = mutableListOf<TransactionItem>()
        val cursor = readableDatabase.rawQuery(
            "SELECT id, title, amount, is_income, category, date, note FROM transactions ORDER BY id DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    TransactionItem(
                        id = it.getLong(0),
                        title = it.getString(1),
                        amount = it.getDouble(2),
                        isIncome = it.getInt(3) == 1,
                        category = it.getString(4) ?: "Makanan",
                        date = it.getString(5),
                        note = it.getString(6) ?: ""
                    )
                )
            }
        }
        return list
    }

    fun delete(id: Long): Int {
        return writableDatabase.delete("transactions", "id = ?", arrayOf(id.toString()))
    }

    fun getSummary(): Triple<Double, Double, Double> {
        var income = 0.0
        var expense = 0.0
        val cursor = readableDatabase.rawQuery(
            "SELECT is_income, SUM(amount) FROM transactions GROUP BY is_income",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                val isInc = it.getInt(0) == 1
                val sum = it.getDouble(1)
                if (isInc) income = sum else expense = sum
            }
        }
        return Triple(income, expense, income - expense)
    }

    fun getCategoryExpenses(): List<Pair<String, Double>> {
        val list = mutableListOf<Pair<String, Double>>()
        val cursor = readableDatabase.rawQuery(
            "SELECT category, SUM(amount) FROM transactions WHERE is_income = 0 GROUP BY category ORDER BY SUM(amount) DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(Pair(it.getString(0) ?: "Lainnya", it.getDouble(1)))
            }
        }
        return list
    }


    companion object {
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
                val dayIndex = parseDate(t.date)?.get(java.util.Calendar.DAY_OF_WEEK)?.minus(1) ?: -1
                if (dayIndex in 0..6) {
                    dailyTotals[dayIndex] = dailyTotals.getOrDefault(dayIndex, 0.0) + t.amount
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

            val maxValue = dailyTotals.values.maxOrNull() ?: 1.0
            val weekly = dailyTotals.values.map { (it / maxValue).toFloat() }.toList()
            return Pair(slices, weekly)
        }

        private fun parseDate(date: String): java.util.Calendar? {
            return try {
                val sdf = java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.US)
                val d = sdf.parse(date)
                d?.let { java.util.Calendar.getInstance().apply { time = it } }
            } catch (e: Exception) {
                null
            }
        }
    }

}