package com.keuangan.app.data

import com.keuangan.app.TransactionItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses the mock/backend JSON payloads into UI models.
 * Backend contract: every endpoint returns the standard envelope
 * { "success": true, "message": "...", "data": {...}, "error": null }
 */
object TransactionParser {

    fun parseTransactions(json: String): List<TransactionItem> {
        val array = JSONArray(json)
        val result = mutableListOf<TransactionItem>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            result.add(
                TransactionItem(
                    id = o.getLong("id"),
                    title = o.getString("title"),
                    amount = o.getDouble("amount"),
                    isIncome = o.getBoolean("isIncome"),
                    category = o.optString("category", "Makanan"),
                    date = o.optString("date", ""),
                    note = o.optString("note", "")
                )
            )
        }
        return result
    }

    fun parseSummary(json: String): Triple<Double, Double, Double> {
        val o = JSONObject(json)
        return Triple(
            o.getDouble("income"),
            o.getDouble("expense"),
            o.getDouble("balance")
        )
    }

    data class Budget(val limit: Double, val spent: Double, val month: String) {
        val percentUsed: Int get() = if (limit > 0) ((spent / limit) * 100).toInt().coerceIn(0, 100) else 0
        val remaining: Double get() = (limit - spent).coerceAtLeast(0.0)
    }

    fun parseBudget(json: String): Budget {
        val o = JSONObject(json)
        return Budget(o.getDouble("limit"), o.getDouble("spent"), o.optString("month", ""))
    }
}
