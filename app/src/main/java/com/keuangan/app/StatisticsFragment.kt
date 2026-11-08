package com.keuangan.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.keuangan.app.R
import java.text.NumberFormat
import java.util.Locale

class StatisticsFragment : Fragment() {

    private val currencyFormat: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = TransactionDb(requireContext())
        val transactions = db.getAll()
        val categoryExpenses = db.getCategoryExpenses()

        val donut = view.findViewById<DonutChartView>(R.id.donutChart)
        val histogram = TransactionDb.calculateBalanceHistogram(transactions, 7, 30)
        donut.setSlices(histogram.first)

        val weekly = view.findViewById<WeeklyBarChartView>(R.id.weeklyChart)
        val (_, _, balance) = db.getSummary()
        weekly.setData(histogram.second)

        val list = view.findViewById<LinearLayout>(R.id.categoryList)
        list.removeAllViews()
        for ((name, amount) in categoryExpenses) {
            val row = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                ).apply { setMargins(0, 6, 0, 6) }
                val nameText = TextView(requireContext()).apply {
                    text = name
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(context, R.color.brand_text_primary))
                }
                val amountText = TextView(requireContext()).apply {
                    text = "-" + currencyFormat.format(amount)
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(context, R.color.expense_red))
                    gravity = android.view.Gravity.END
                }
                addView(nameText)
                addView(amountText)
            }
            list.addView(row)
        }

        val tvTotal = view.findViewById<TextView>(R.id.tvTotalCategory)
        tvTotal.text = "Total Kategori (7 hari): " + currencyFormat.format(balance)
    }
}
