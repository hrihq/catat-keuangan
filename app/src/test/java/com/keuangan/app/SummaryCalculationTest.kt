package com.keuangan.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SummaryCalculationTest {

    @Test
    fun testBalanceCalculation() {
        val transactions = listOf(
            TransactionItem(1, "Gaji", 5000000.0, true, "Gaji", "2026-11-04 10:00"),
            TransactionItem(2, "Makan", 50000.0, false, "Makanan", "2026-11-04 12:00"),
            TransactionItem(3, "Bensin", 100000.0, false, "Transport", "2026-11-04 14:00")
        )

        val income = transactions.filter { it.isIncome }.sumOf { it.amount }
        val expense = transactions.filter { !it.isIncome }.sumOf { it.amount }
        val balance = income - expense

        assertEquals(5000000.0, income, 0.001)
        assertEquals(150000.0, expense, 0.001)
        assertEquals(4850000.0, balance, 0.001)
    }
}
