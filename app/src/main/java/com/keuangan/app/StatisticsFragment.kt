package com.keuangan.app

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.keuangan.app.data.TransactionRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class StatisticsFragment : Fragment() {

    private val repository by lazy { TransactionRepository(requireContext().applicationContext) }

    private lateinit var donut: DonutChartView
    private lateinit var weekly: WeeklyBarChartView
    private lateinit var legend: LinearLayout
    private lateinit var tvMonth: TextView
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
    private var entrancePlayed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_statistics, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        donut = view.findViewById(R.id.donutChart)
        weekly = view.findViewById(R.id.weeklyChart)
        legend = view.findViewById(R.id.categoryLegend)
        tvMonth = view.findViewById(R.id.tvMonth)

        view.findViewById<View>(R.id.btnMonth).setOnClickListener { showMonthPicker() }
        updateMonthLabel()
        loadData()
        if (!isHidden) playEntrance()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        entrancePlayed = hidden
        if (!hidden) playEntrance()
    }

    private fun playEntrance() {
        if (entrancePlayed) return
        entrancePlayed = true
        val root = view ?: return
        root.animate().cancel()
        root.alpha = 0f
        root.translationY = (12 * resources.displayMetrics.density)
        root.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    fun loadData() {
        if (!isAdded) return
        viewLifecycleOwner.lifecycleScope.launch {
            val transactions = repository.getTransactions()
            val categoryExpenses = repository.getCategoryExpenses()
            if (!isAdded) return@launch

            val (slices, weeklyData) = TransactionDb.calculateBalanceHistogram(transactions, 7, 30)
            donut.setSlices(slices)
            weekly.setData(weeklyData)
            buildLegend(categoryExpenses)
        }
    }

    private fun buildLegend(categoryExpenses: List<Pair<String, Double>>) {
        legend.removeAllViews()
        val total = categoryExpenses.sumOf { it.second }.coerceAtLeast(1.0)

        categoryExpenses.forEach { (name, amount) ->
            val percent = ((amount / total) * 100).toInt()

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
            }

            val dot = View(requireContext()).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(ContextCompat.getColor(requireContext(), colorFor(name)))
                }
                layoutParams = LinearLayout.LayoutParams(dp(10), dp(10))
            }

            val nameText = TextView(requireContext()).apply {
                text = name
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_text_primary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val percentText = TextView(requireContext()).apply {
                text = "$percent%"
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_text_secondary))
            }

            row.addView(dot)
            row.addView(nameText)
            row.addView(percentText)
            legend.addView(row)
        }
    }

    private fun colorFor(category: String) = when (category) {
        "Makanan" -> R.color.chart_food
        "Transport" -> R.color.chart_transport
        "Tagihan" -> R.color.chart_bills
        else -> R.color.chart_other
    }

    private fun showMonthPicker() {
        val months = arrayOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pilih bulan")
            .setItems(months) { _, which ->
                selectedMonth = which
                updateMonthLabel()
            }
            .show()
    }

    private fun updateMonthLabel() {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val months = arrayOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        tvMonth.text = "${months[selectedMonth]} $year"
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
