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
import com.keuangan.app.util.Haptics
import com.keuangan.app.util.Motion
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class StatisticsFragment : Fragment() {

    private val repository by lazy { TransactionRepository(requireContext().applicationContext) }
    private val currencyFormat: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }

    private lateinit var donut: DonutChartView
    private lateinit var weekly: WeeklyBarChartView
    private lateinit var legend: LinearLayout
    private lateinit var tvMonth: TextView
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
    private var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
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

        view.findViewById<View>(R.id.btnMonth).setOnClickListener { pill ->
            Haptics.tap(pill)
            pressPill(pill)
            showMonthPicker()
        }
        updateMonthLabel()
        loadData()
        if (!isHidden) playEntrance()
    }

    /** Umpan balik tekan pada pill bulan. */
    private fun pressPill(view: View) {
        if (!Motion.shouldAnimate(requireContext())) return
        view.animate().cancel()
        view.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(80)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(160)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
            .start()
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

    fun loadData(animateIn: Boolean = false) {
        if (!isAdded) return
        viewLifecycleOwner.lifecycleScope.launch {
            val transactions = repository.getTransactions()
            val categoryExpenses = repository.getCategoryExpenses()
            if (!isAdded) return@launch

            // Donut & legend: hanya transaksi pada bulan yang dipilih.
            val monthTransactions = transactions.filter { item ->
                val cal = Calendar.getInstance().apply { timeInMillis = item.dateMs }
                cal.get(Calendar.MONTH) == selectedMonth && cal.get(Calendar.YEAR) == selectedYear
            }
            val slices = TransactionDb.calculateBalanceHistogram(monthTransactions, 7, 30).first
            // Grafik mingguan: 7 hari terakhir (sesuai label kartu).
            val weeklyData = repository.getWeeklyExpense().map { it.toFloat() }
            val monthExpense = monthTransactions.filter { !it.isIncome }.sumOf { it.amount }
            donut.setCenterAmount(monthExpense, currencyFormat)
            donut.setSlices(slices)
            weekly.setData(weeklyData, TransactionDb.lastSevenDayLabels())
            buildLegend(categoryExpenses)
            if (animateIn) animateChartsIn()
        }
    }

    /** Ganti bulan: angka geser + grafik fade masuk. */
    private fun selectMonth(month: Int, year: Int) {
        val animate = Motion.shouldAnimate(requireContext())
        val shift = 18f * resources.displayMetrics.density
        if (animate) {
            val views = listOf<View>(donut, weekly, legend)
            views.forEach {
                it.animate().cancel()
                it.animate().alpha(0f).translationX(-shift * 0.6f).setDuration(130).start()
            }
            tvMonth.animate().cancel()
            tvMonth.animate().alpha(0f).translationX(-shift).setDuration(130).start()
        }
        selectedMonth = month
        selectedYear = year
        updateMonthLabel()
        if (animate) {
            tvMonth.alpha = 0f
            tvMonth.translationX = shift
            listOf<View>(donut, weekly, legend).forEach {
                it.alpha = 0f
                it.translationX = shift * 0.6f
            }
        }
        loadData(animateIn = animate)
    }

    private fun animateChartsIn() {
        val shift = 18f * resources.displayMetrics.density
        tvMonth.animate().alpha(1f).translationX(0f).setDuration(220)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
        listOf<View>(donut, weekly, legend).forEachIndexed { index, chart ->
            chart.animate()
                .alpha(1f)
                .translationX(0f)
                .setStartDelay(index * 40L)
                .setDuration(240)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
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
        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.sheet_month_picker, null)
        sheet.setContentView(view)

        val months = monthNames()
        val yearLabel = view.findViewById<TextView>(R.id.tvYear)
        val grid = view.findViewById<LinearLayout>(R.id.monthGrid)
        var year = selectedYear

        fun rebuild() {
            yearLabel.text = year.toString()
            grid.removeAllViews()
            for (row in 0 until 4) {
                val rowLayout = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                for (col in 0 until 3) {
                    val monthIndex = row * 3 + col
                    val selected = monthIndex == selectedMonth && year == selectedYear
                    val button = TextView(requireContext()).apply {
                        text = months[monthIndex]
                        gravity = Gravity.CENTER
                        textSize = 14f
                        setPadding(0, dp(16), 0, dp(16))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                            marginStart = dp(4)
                            marginEnd = dp(4)
                            topMargin = dp(4)
                            bottomMargin = dp(4)
                        }
                        setBackgroundResource(R.drawable.bg_month_item)
                        backgroundTintList = android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                if (selected) R.color.brand_light_blue else R.color.brand_surface
                            )
                        )
                        setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                if (selected) R.color.brand_blue else R.color.brand_text_primary
                            )
                        )
                        setOnClickListener {
                            Haptics.tap(this)
                            pressPill(this)
                            sheet.dismiss()
                            selectMonth(monthIndex, year)
                        }
                    }
                    rowLayout.addView(button)
                }
                grid.addView(rowLayout)
            }
        }

        view.findViewById<View>(R.id.btnPrevYear).setOnClickListener { year--; rebuild() }
        view.findViewById<View>(R.id.btnNextYear).setOnClickListener { year++; rebuild() }

        rebuild()
        sheet.show()
    }

    private fun monthNames() = arrayOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    private fun updateMonthLabel() {
        tvMonth.text = "${monthNames()[selectedMonth]} $selectedYear"
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
