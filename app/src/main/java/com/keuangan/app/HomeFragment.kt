package com.keuangan.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var db: TransactionDb
    private lateinit var adapter: TransactionAdapter
    private val indonesianLocale = Locale("id", "ID")
    private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(indonesianLocale).apply {
        maximumFractionDigits = 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = TransactionDb(requireContext())
        val tvTotalBalance = view.findViewById<TextView>(R.id.tvTotalBalance)
        val tvTotalIncome = view.findViewById<TextView>(R.id.tvTotalIncome)
        val tvTotalExpense = view.findViewById<TextView>(R.id.tvTotalExpense)
        val tvBudgetSubtitle = view.findViewById<TextView>(R.id.tvBudgetSubtitle)
        val pbBudget = view.findViewById<LinearProgressIndicator>(R.id.pbBudget)
        val tvEmptyState = view.findViewById<TextView>(R.id.tvEmptyState)
        val rvTransactions = view.findViewById<RecyclerView>(R.id.rvTransactions)

        adapter = TransactionAdapter(
            currencyFormat = currencyFormat,
            onItemClick = { /* TODO */ }
        )
        rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        rvTransactions.adapter = adapter

        loadData(tvTotalBalance, tvTotalIncome, tvTotalExpense, tvBudgetSubtitle, pbBudget, tvEmptyState, rvTransactions)
    }

    private fun loadData(
        tvTotalBalance: TextView,
        tvTotalIncome: TextView,
        tvTotalExpense: TextView,
        tvBudgetSubtitle: TextView,
        pbBudget: LinearProgressIndicator,
        tvEmptyState: TextView,
        rvTransactions: RecyclerView
    ) {
        val items = db.getAll()
        val (income, expense, balance) = db.getSummary()

        tvTotalBalance.text = currencyFormat.format(balance)
        tvTotalIncome.text = "+ ${currencyFormat.format(income)}"
        tvTotalExpense.text = "- ${currencyFormat.format(expense)}"

        if (items.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            rvTransactions.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            rvTransactions.visibility = View.VISIBLE
            adapter.setData(items)
        }

        val monthlyBudget = 5000000.0
        val usedPercentage = if (monthlyBudget > 0) ((expense / monthlyBudget) * 100).toInt().coerceIn(0, 100) else 0
        val remainingBudget = (monthlyBudget - expense).coerceAtLeast(0.0)

        pbBudget.progress = usedPercentage
        tvBudgetSubtitle.text = "$usedPercentage% terpakai · sisa ${currencyFormat.format(remainingBudget)}"
    }
}
