package com.keuangan.app

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
import com.keuangan.app.ui.Halo
import com.keuangan.app.ui.ThemeManager
import com.keuangan.app.ui.components.BalanceCard
import com.keuangan.app.ui.components.EmptyState
import com.keuangan.app.ui.components.HeaderGreeting
import com.keuangan.app.ui.components.IconTile
import com.keuangan.app.ui.components.LoadingState
import com.keuangan.app.ui.components.MiniStatCard
import com.keuangan.app.ui.components.SectionHeader
import com.keuangan.app.ui.components.TransactionRow
import com.keuangan.app.ui.home.HomeData
import com.keuangan.app.ui.home.HomeUiState
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private val repository by lazy { TransactionRepository(requireContext().applicationContext) }
    private val currencyFormat: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }

    private lateinit var headerGreeting: HeaderGreeting
    private lateinit var balanceCard: BalanceCard
    private lateinit var incomeCard: MiniStatCard
    private lateinit var expenseCard: MiniStatCard
    private lateinit var chartCard: View
    private lateinit var weeklyChart: WeeklyBarChartView
    private lateinit var sectionHeader: SectionHeader
    private lateinit var transactionList: LinearLayout
    private lateinit var loadingState: LoadingState
    private lateinit var emptyState: EmptyState

    private var entrancePlayed = false
    private var currentData: HomeData? = null
    private val rows = mutableMapOf<Long, TransactionRow>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        headerGreeting = view.findViewById(R.id.headerGreeting)
        balanceCard = view.findViewById(R.id.balanceCard)
        incomeCard = view.findViewById(R.id.incomeCard)
        expenseCard = view.findViewById(R.id.expenseCard)
        chartCard = view.findViewById(R.id.chartCard)
        weeklyChart = view.findViewById(R.id.weeklyChart)
        sectionHeader = view.findViewById(R.id.sectionHeader)
        transactionList = view.findViewById(R.id.transactionList)
        loadingState = view.findViewById(R.id.loadingState)
        emptyState = view.findViewById(R.id.emptyState)
        emptyState.onActionClick = { (activity as? MainActivity)?.openAddTransaction() }

        headerGreeting.bind(greeting(), getString(R.string.app_name))
        sectionHeader.bind(getString(R.string.transactions_title), getString(R.string.see_all))
        headerGreeting.setOnBellClickListener { showNotifications() }
        sectionHeader.onActionClick = {
            (activity as? MainActivity)?.openHistory()
        }

        loadData()
        if (!isHidden) playEntrance()

        if (!ThemeManager.hasSeenIntro(requireContext())) {
            Halo.pulse(balanceCard, 450)
            Halo.pulse(chartCard, 650)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        entrancePlayed = hidden
        if (!hidden) playEntrance()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun playEntrance() {
        if (entrancePlayed) return
        if (!::headerGreeting.isInitialized || !::balanceCard.isInitialized) return
        entrancePlayed = true
        val views = listOf(headerGreeting, balanceCard, incomeCard, expenseCard, chartCard, sectionHeader, transactionList)
        views.forEachIndexed { index, v ->
            v.alpha = 0f
            v.translationY = (14 * resources.displayMetrics.density)
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((index * 70).toLong())
                .setDuration(320)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }

    /** Panel notifikasi: ringkasan data + pengingat anggaran. */
    private fun showNotifications() {
        val data = currentData
        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.sheet_notifications, null)
        sheet.setContentView(view)
        val list = view.findViewById<LinearLayout>(R.id.notificationList)
        list.removeAllViews()

        val items = mutableListOf<Triple<Int, String, String>>()
        if (data == null || data.transactions.isEmpty()) {
            items.add(Triple(R.drawable.ic_add, "Mulai mencatat", "Tambah transaksi pertamamu lewat tombol +"))
        } else {
            items.add(Triple(R.drawable.ic_wallet, "Saldo total", currencyFormat.format(data.balance)))
            items.add(Triple(R.drawable.ic_receipt, "Transaksi tercatat", "${data.transactions.size} transaksi"))
            items.add(
                Triple(
                    R.drawable.ic_bar_chart,
                    "Anggaran bulan ini",
                    "${data.budget.percentUsed}% terpakai · sisa ${currencyFormat.format(data.budget.remaining)}"
                )
            )
        }

        items.forEach { (icon, title, subtitle) ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(12) }
            }
            val tile = IconTile(requireContext()).apply {
                bind(icon, R.color.brand_blue, R.color.brand_light_blue)
            }
            val column = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(12)
                }
            }
            column.addView(TextView(requireContext()).apply {
                text = title
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_text_primary))
            })
            column.addView(TextView(requireContext()).apply {
                text = subtitle
                textSize = 13f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_text_secondary))
            })
            row.addView(tile)
            row.addView(column)
            list.addView(row)
        }
        sheet.show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    /** Public so [MainActivity] can refresh the screen after adding a transaction. */
    fun loadData() {
        if (!isAdded) return
        render(HomeUiState.Loading)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val transactions = repository.getTransactions()
                val (income, expense, balance) = repository.getSummary()
                val budget = repository.getBudget()
                val weekly = repository.getWeeklyExpense()
                if (!isAdded) return@launch

                val state = if (transactions.isEmpty()) {
                    HomeUiState.Empty
                } else {
                    HomeUiState.Success(HomeData(balance, income, expense, budget, transactions, weekly))
                }
                render(state)
            } catch (e: Exception) {
                if (isAdded) render(HomeUiState.Error(e.message ?: "Gagal memuat data"))
            }
        }
    }

    private fun render(state: HomeUiState) {
        when (state) {
            is HomeUiState.Loading -> {
                loadingState.visibility = View.VISIBLE
                emptyState.visibility = View.GONE
                transactionList.visibility = View.GONE
                balanceCard.visibility = View.INVISIBLE
                incomeCard.visibility = View.INVISIBLE
                expenseCard.visibility = View.INVISIBLE
                chartCard.visibility = View.INVISIBLE
            }
            is HomeUiState.Empty -> {
                loadingState.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
                emptyState.bind(
                    getString(R.string.empty_transactions),
                    getString(R.string.add_transaction)
                )
                transactionList.visibility = View.GONE
                balanceCard.visibility = View.INVISIBLE
                incomeCard.visibility = View.INVISIBLE
                expenseCard.visibility = View.INVISIBLE
                chartCard.visibility = View.INVISIBLE
            }
            is HomeUiState.Success -> {
                loadingState.visibility = View.GONE
                emptyState.visibility = View.GONE
                balanceCard.visibility = View.VISIBLE
                incomeCard.visibility = View.VISIBLE
                expenseCard.visibility = View.VISIBLE
                chartCard.visibility = View.VISIBLE
                transactionList.visibility = View.VISIBLE
                bind(state.data)
            }
            is HomeUiState.Error -> {
                loadingState.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
                emptyState.bind(state.message)
                transactionList.visibility = View.GONE
                balanceCard.visibility = View.INVISIBLE
                incomeCard.visibility = View.INVISIBLE
                expenseCard.visibility = View.INVISIBLE
                chartCard.visibility = View.INVISIBLE
            }
        }
    }

    private fun bind(data: HomeData) {
        currentData = data
        rows.clear()
        balanceCard.bind(
            getString(R.string.total_saldo_label_pattern, monthYear()),
            data.balance,
            data.income,
            data.expense,
            currencyFormat
        )
        incomeCard.bind(
            "Pemasukan",
            "+${currencyFormat.format(data.income)}",
            R.drawable.ic_arrow_up,
            R.color.income_green,
            R.color.income_container
        )
        expenseCard.bind(
            "Pengeluaran",
            "-${currencyFormat.format(data.expense)}",
            R.drawable.ic_arrow_down,
            R.color.expense_red,
            R.color.expense_container
        )
        if (data.weeklyExpense.isNotEmpty()) {
            weeklyChart.setData(
                data.weeklyExpense.map { it.toFloat() },
                TransactionDb.lastSevenDayLabels()
            )
        }

        transactionList.removeAllViews()
        data.transactions.forEach { item ->
            val row = TransactionRow(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (14 * resources.displayMetrics.density).toInt()
                }
            }
            val (icon, tint, background) = styleFor(item)
            row.bind(
                title = item.title,
                subtitle = "${item.category} · ${item.date}",
                amountText = if (item.isIncome) {
                    "+${currencyFormat.format(item.amount)}"
                } else {
                    "-${currencyFormat.format(item.amount)}"
                },
                isIncome = item.isIncome,
                iconRes = icon,
                iconTint = tint,
                iconBackground = background,
                onDelete = { confirmDelete(item) }
            )
            rows[item.id] = row
            transactionList.addView(row)
        }
    }

    private fun confirmDelete(item: TransactionItem) {
        val row = rows[item.id]
        if (row == null) {
            viewLifecycleOwner.lifecycleScope.launch {
                repository.deleteTransaction(item.id)
                loadData()
            }
            return
        }
        row.removeWithAnimation {
            viewLifecycleOwner.lifecycleScope.launch {
                repository.deleteTransaction(item.id)
                loadData()
            }
        }
    }

    private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..10 -> "Selamat pagi"
        in 11..14 -> "Selamat siang"
        in 15..18 -> "Selamat sore"
        else -> "Selamat malam"
    }

    private fun monthYear(): String =
        SimpleDateFormat("MMM yyyy", Locale("id", "ID")).format(Date())

    private fun styleFor(item: TransactionItem): Triple<Int, Int, Int> = when (item.category) {
        "Makanan" -> Triple(R.drawable.ic_restaurant, R.color.brand_text_primary, R.color.brand_yellow)
        "Transport" -> Triple(R.drawable.ic_directions_car, R.color.brand_blue, R.color.brand_light_blue)
        "Tagihan" -> Triple(R.drawable.ic_receipt, R.color.brand_blue, R.color.brand_light_blue)
        "Gaji" -> Triple(R.drawable.ic_wallet, R.color.brand_blue, R.color.brand_teal)
        "Bonus" -> Triple(R.drawable.ic_arrow_up, R.color.brand_blue, R.color.brand_teal)
        "Investasi" -> Triple(R.drawable.ic_bar_chart, R.color.brand_blue, R.color.brand_teal)
        "Pemasukan" -> Triple(R.drawable.ic_wallet, R.color.brand_blue, R.color.brand_teal)
        else -> Triple(R.drawable.ic_add, R.color.brand_blue, R.color.brand_light_blue)
    }
}
