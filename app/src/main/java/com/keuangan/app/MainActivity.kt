package com.keuangan.app

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.keuangan.app.data.TransactionRepository
import com.keuangan.app.ui.ThemeManager
import com.keuangan.app.ui.components.FloatingBottomNavView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var repository: TransactionRepository
    private lateinit var bottomNav: FloatingBottomNavView
    private val tabTags = listOf("home", "stats", "wallet", "profile")
    private var currentFragment = "home"
    private var homeFragment: HomeFragment? = null
    private var statsFragment: StatisticsFragment? = null
    private var walletFragment: WalletFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = TransactionRepository(this)

        if (savedInstanceState == null) {
            initFragments()
        }

        bottomNav = findViewById(R.id.bottomNavView)
        bottomNav.tabs = listOf(
            FloatingBottomNavView.Tab(R.drawable.ic_home, getString(R.string.tab_home)),
            FloatingBottomNavView.Tab(R.drawable.ic_pie_chart, getString(R.string.tab_stats)),
            FloatingBottomNavView.Tab(R.drawable.ic_wallet, getString(R.string.tab_wallet)),
            FloatingBottomNavView.Tab(R.drawable.ic_person, getString(R.string.tab_profile))
        )
        bottomNav.onTabSelected = { showTab(it) }

        findViewById<View>(R.id.fabAdd).setOnClickListener { showAddTransactionBottomSheet() }
    }

    private fun initFragments() {
        homeFragment = HomeFragment()
        statsFragment = StatisticsFragment()
        walletFragment = WalletFragment()
        val profileFragment = ProfileFragment()

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragmentContainer, homeFragment!!, "home")
        transaction.add(R.id.fragmentContainer, statsFragment!!, "stats")
        transaction.add(R.id.fragmentContainer, walletFragment!!, "wallet")
        transaction.add(R.id.fragmentContainer, profileFragment, "profile")
        transaction.hide(statsFragment!!)
        transaction.hide(walletFragment!!)
        transaction.hide(profileFragment)
        transaction.commit()
    }

    private fun showTab(index: Int) {
        val tag = tabTags[index]
        if (currentFragment == tag) return

        val transaction = supportFragmentManager.beginTransaction()
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        val current = supportFragmentManager.findFragmentByTag(currentFragment)
        val next = supportFragmentManager.findFragmentByTag(tag)
        if (current != null) transaction.hide(current)
        if (next != null) transaction.show(next)
        transaction.commit()

        currentFragment = tag
        refreshTab(tag)
    }

    private fun refreshTab(tag: String) {
        when (tag) {
            "home" -> homeFragment?.loadData()
            "stats" -> statsFragment?.loadData()
            "wallet" -> walletFragment?.loadData()
        }
    }

    private fun showAddTransactionBottomSheet() {
        val sheet = BottomSheetDialog(this).apply {
            setContentView(R.layout.bottom_sheet_add_transaction)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val root = sheet.window?.decorView?.findViewById<View>(android.R.id.content) ?: run {
            sheet.show()
            return
        }

        val amountInput = root.findViewById<EditText>(R.id.etModalAmount) ?: run {
            sheet.show()
            return
        }
        val prefix = root.findViewById<TextView>(R.id.tvCurrencyPrefix) ?: run {
            sheet.show()
            return
        }
        val titleInput = root.findViewById<EditText>(R.id.etModalTitle)
        val noteInput = root.findViewById<EditText>(R.id.etModalNote)
        val walletLabel = root.findViewById<TextView>(R.id.tvModalWallet)

        root.findViewById<View>(R.id.btnBackModal).setOnClickListener { sheet.dismiss() }

        val categories = listOf(
            root.findViewById<Chip>(R.id.chipFood),
            root.findViewById<Chip>(R.id.chipTransport),
            root.findViewById<Chip>(R.id.chipBills),
            root.findViewById<Chip>(R.id.chipShopping),
            root.findViewById<Chip>(R.id.chipEntertainment),
            root.findViewById<Chip>(R.id.chipHealth)
        )

        categories.forEach { it.isChecked = false }
        categories.first().isChecked = true

        var incomeMode = false
        var selectedWallet = walletLabel.text.toString().ifEmpty { "Tunai" }
        var selectedDateMs = System.currentTimeMillis()

        val dateLabel = root.findViewById<TextView>(R.id.tvModalDate)
        dateLabel.setOnClickListener {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = selectedDateMs }
            android.app.DatePickerDialog(
                this@MainActivity,
                { _, year, month, day ->
                    val picked = java.util.Calendar.getInstance().apply {
                        set(year, month, day, 12, 0, 0)
                    }
                    selectedDateMs = picked.timeInMillis
                    dateLabel.text = TransactionDb.formatDate(selectedDateMs)
                },
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        }

        walletLabel.setOnClickListener {
            lifecycleScope.launch {
                val wallets = repository.getWallets()
                if (wallets.isEmpty()) return@launch
                val names = wallets.map { it.name }.toTypedArray()
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Pilih dompet")
                    .setItems(names) { _, which ->
                        selectedWallet = names[which]
                        walletLabel.text = selectedWallet
                    }
                    .show()
            }
        }

        root.findViewById<TextView>(R.id.btnTabExpense).setOnClickListener {
            incomeMode = false
            categories.forEach { it.setTextColor(ContextCompat.getColor(root.context, R.color.brand_text_secondary)) }
            prefix.setTextColor(ContextCompat.getColor(root.context, R.color.expense_red))
            amountInput.setTextColor(ContextCompat.getColor(root.context, R.color.expense_red))
        }

        root.findViewById<TextView>(R.id.btnTabIncome).setOnClickListener {
            incomeMode = true
            categories.forEach { it.setTextColor(ContextCompat.getColor(root.context, R.color.brand_blue)) }
            prefix.setTextColor(ContextCompat.getColor(root.context, R.color.brand_blue))
            amountInput.setTextColor(ContextCompat.getColor(root.context, R.color.brand_blue))
        }

        root.findViewById<MaterialButton>(R.id.btnModalSave).setOnClickListener {
            val amount = amountInput.text.toString().toDoubleOrNull() ?: 0.0
            if (amount <= 0) {
                amountInput.error = "Masukkan jumlah"
                return@setOnClickListener
            }
            val title = titleInput?.text?.toString()?.takeIf { it.isNotEmpty() } ?: "Tanpa keterangan"
            val note = noteInput?.text?.toString()?.takeIf { it.isNotEmpty() } ?: ""
            val category = categories.firstOrNull { it.isChecked }?.text?.toString() ?: "Makanan"

            lifecycleScope.launch {
                repository.addTransaction(title, amount, incomeMode, category, note, selectedWallet, selectedDateMs)
                sheet.dismiss()
                homeFragment?.loadData()
                walletFragment?.loadData()
                Toast.makeText(applicationContext, R.string.toast_saved, Toast.LENGTH_SHORT).show()
            }
        }

        sheet.show()
    }
}
