package com.keuangan.app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.keuangan.app.data.TransactionRepository
import com.keuangan.app.ui.components.TransactionRow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/** Riwayat transaksi: cari + filter bulan + hapus. */
class RiwayatFragment : Fragment() {

    private val repository by lazy { TransactionRepository(requireContext().applicationContext) }
    private val currencyFormat: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))

    private lateinit var historyList: LinearLayout
    private lateinit var emptyHistory: TextView
    private lateinit var spinnerMonth: Spinner
    private lateinit var etSearch: EditText

    private var allTransactions: List<TransactionItem> = emptyList()
    private var selectedMonth: String? = null
    private var query: String = ""
    private var monthOptions: List<String> = emptyList()
    private lateinit var tvFilterMonth: TextView
    private var entrancePlayed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_riwayat, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        historyList = view.findViewById(R.id.historyList)
        emptyHistory = view.findViewById(R.id.emptyHistory)
        spinnerMonth = view.findViewById(R.id.spinnerMonth)
        etSearch = view.findViewById(R.id.etSearch)
        tvFilterMonth = view.findViewById(R.id.tvFilterMonth)

        tvFilterMonth.setOnClickListener { showMonthPicker() }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                query = s?.toString().orEmpty()
                render()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedMonth = if (position == 0) null else spinnerMonth.selectedItem.toString()
                render()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

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

    override fun onResume() {
        super.onResume()
        loadData()
    }

    fun loadData() {
        if (!isAdded) return
        viewLifecycleOwner.lifecycleScope.launch {
            allTransactions = repository.getTransactions()
            if (!isAdded) return@launch
            setupMonths()
            render()
        }
    }

    private fun setupMonths() {
        val months = allTransactions.map { monthFormat.format(java.util.Date(it.dateMs)) }
            .distinct()
            .sortedByDescending { monthFormat.parse(it)?.time ?: 0L }
        monthOptions = months
        if (spinnerMonth.adapter == null) {
            val options = listOf("Semua Bulan") + months
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                options
            ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            spinnerMonth.adapter = adapter
        }
        tvFilterMonth.text = selectedMonth ?: "Semua Bulan"
    }

    /** Pilih bulan lewat dialog (filter pill). */
    private fun showMonthPicker() {
        val options = listOf("Semua Bulan") + monthOptions
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pilih bulan")
            .setItems(options.toTypedArray()) { _, which ->
                selectedMonth = if (which == 0) null else options[which]
                tvFilterMonth.text = selectedMonth ?: "Semua Bulan"
                render()
            }
            .show()
    }

    private fun render() {
        val filtered = allTransactions.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.title.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true)
            val matchesMonth = selectedMonth == null ||
                monthFormat.format(java.util.Date(item.dateMs)) == selectedMonth
            matchesQuery && matchesMonth
        }

        historyList.removeAllViews()
        emptyHistory.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

        filtered.forEach { item ->
            val row = TransactionRow(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (14 * resources.displayMetrics.density).toInt() }
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
                onDelete = {
                    viewLifecycleOwner.lifecycleScope.launch {
                        repository.deleteTransaction(item.id)
                        loadData()
                    }
                }
            )
            historyList.addView(row)
        }
    }

    private fun styleFor(item: TransactionItem): Triple<Int, Int, Int> = when (item.category) {
        "Makanan" -> Triple(R.drawable.ic_restaurant, R.color.brand_text_primary, R.color.brand_yellow)
        "Transport" -> Triple(R.drawable.ic_directions_car, R.color.brand_blue, R.color.brand_light_blue)
        "Tagihan" -> Triple(R.drawable.ic_receipt, R.color.brand_blue, R.color.brand_light_blue)
        "Pemasukan" -> Triple(R.drawable.ic_wallet, R.color.brand_blue, R.color.brand_teal)
        else -> Triple(R.drawable.ic_add, R.color.brand_blue, R.color.brand_light_blue)
    }
}
