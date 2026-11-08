package com.keuangan.app

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.keuangan.app.ui.components.FloatingBottomNavView

class MainActivity : AppCompatActivity() {

    private lateinit var db: TransactionDb
    private lateinit var bottomNav: FloatingBottomNavView
    private val tabTags = listOf("home", "stats", "wallet", "profile")
    private var currentFragment = "home"
    private var homeFragment: HomeFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = TransactionDb(this)

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
        val statsFragment = StatisticsFragment()
        val walletFragment = WalletFragment()
        val profileFragment = ProfileFragment()

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragmentContainer, homeFragment!!, "home")
        transaction.add(R.id.fragmentContainer, statsFragment, "stats")
        transaction.add(R.id.fragmentContainer, walletFragment, "wallet")
        transaction.add(R.id.fragmentContainer, profileFragment, "profile")
        transaction.hide(statsFragment)
        transaction.hide(walletFragment)
        transaction.hide(profileFragment)
        transaction.commit()
    }

    private fun showTab(index: Int) {
        val tag = tabTags[index]
        if (currentFragment == tag) return

        val transaction = supportFragmentManager.beginTransaction()
        val currentFrag = supportFragmentManager.findFragmentByTag(currentFragment)
        val nextFrag = supportFragmentManager.findFragmentByTag(tag)

        if (currentFrag != null) transaction.hide(currentFrag)
        if (nextFrag != null) transaction.show(nextFrag)

        transaction.commit()
        currentFragment = tag
        if (tag == "home") homeFragment?.loadData()
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
            db.insert(title, amount, incomeMode, category, note)
            sheet.dismiss()
            homeFragment?.loadData()
            Toast.makeText(applicationContext, R.string.toast_saved, Toast.LENGTH_SHORT).show()
        }

        sheet.show()
    }
}
