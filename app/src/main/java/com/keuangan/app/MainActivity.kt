package com.keuangan.app

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip

class MainActivity : AppCompatActivity() {

    private lateinit var db: TransactionDb
    private var currentFragment = "home"
    private var homeFragment: HomeFragment? = null
    private var incomeMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = TransactionDb(this)

        if (savedInstanceState == null) {
            initFragments()
        }

        setupTabListeners()
    }

    private fun initFragments() {
        val transaction = supportFragmentManager.beginTransaction()
        homeFragment = HomeFragment()
        transaction.add(R.id.fragmentContainer, homeFragment!!, "home")
        transaction.add(R.id.fragmentContainer, StatisticsFragment(), "stats")
        transaction.add(R.id.fragmentContainer, WalletFragment(), "wallet")
        transaction.add(R.id.fragmentContainer, ProfileFragment(), "profile")
        transaction.hide(supportFragmentManager.findFragmentByTag("stats")!!)
        transaction.hide(supportFragmentManager.findFragmentByTag("wallet")!!)
        transaction.hide(supportFragmentManager.findFragmentByTag("profile")!!)
        transaction.commit()
    }

    private fun showFragment(tag: String) {
        if (currentFragment == tag) return

        val transaction = supportFragmentManager.beginTransaction()
        val currentFrag = supportFragmentManager.findFragmentByTag(currentFragment)
        val nextFrag = supportFragmentManager.findFragmentByTag(tag)

        if (currentFrag != null) transaction.hide(currentFrag)
        if (nextFrag != null) transaction.show(nextFrag)

        transaction.commit()
        currentFragment = tag
        if (tag == "home") homeFragment?.loadData()
        updateTabIndicator(tag)
    }

    private fun updateTabIndicator(tag: String) {
        val tabHome = findViewById<View>(R.id.tabHome)
        val tabStats = findViewById<View>(R.id.tabStats)
        val tabWallet = findViewById<View>(R.id.tabWallet)
        val tabProfile = findViewById<View>(R.id.tabProfile)

        val indicatorHome = findViewById<View>(R.id.indicatorHome)
        val indicatorStats = findViewById<View>(R.id.indicatorStats)
        val indicatorWallet = findViewById<View>(R.id.indicatorWallet)
        val indicatorProfile = findViewById<View>(R.id.indicatorProfile)

        listOf(indicatorHome, indicatorStats, indicatorWallet, indicatorProfile).forEach {
            it.visibility = View.GONE
        }

        when (tag) {
            "home" -> indicatorHome.visibility = View.VISIBLE
            "stats" -> indicatorStats.visibility = View.VISIBLE
            "wallet" -> indicatorWallet.visibility = View.VISIBLE
            "profile" -> indicatorProfile.visibility = View.VISIBLE
        }
    }

    private fun setupTabListeners() {
        findViewById<View>(R.id.tabHome).setOnClickListener { showFragment("home") }
        findViewById<View>(R.id.tabStats).setOnClickListener { showFragment("stats") }
        findViewById<View>(R.id.tabWallet).setOnClickListener { showFragment("wallet") }
        findViewById<View>(R.id.tabProfile).setOnClickListener { showFragment("profile") }
        findViewById<View>(R.id.fabAdd).setOnClickListener { showAddTransactionBottomSheet() }
    }

    private fun showAddTransactionBottomSheet() {
        val sheet = BottomSheetDialog(applicationContext).apply {
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
            root.findViewById<com.google.android.material.chip.Chip>(R.id.chipFood),
            root.findViewById<com.google.android.material.chip.Chip>(R.id.chipTransport),
            root.findViewById<com.google.android.material.chip.Chip>(R.id.chipBills),
            root.findViewById<com.google.android.material.chip.Chip>(R.id.chipShopping),
            root.findViewById<com.google.android.material.chip.Chip>(R.id.chipEntertainment),
            root.findViewById<com.google.android.material.chip.Chip>(R.id.chipHealth)
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

        root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnModalSave).setOnClickListener {
            val amount = amountInput.text.toString().toDoubleOrNull() ?: 0.0
            if (amount <= 0) {
                amountInput.error = "Masukkan jumlah"
                return@setOnClickListener
            }
            db.insert(
                titleInput?.text?.toString().takeIf { it.isNotEmpty() } ?: "Tanpa keterangan",
                amount,
                incomeMode,
                categories.firstOrNull { it.isChecked }?.text.toString() ?: "Makanan",
                noteInput?.text?.toString().takeIf { it.isNotEmpty() } ?: null
            )
            sheet.dismiss()
            homeFragment?.loadData()
            Toast.makeText(applicationContext, R.string.toast_saved, Toast.LENGTH_SHORT).show()
        }

        sheet.show()
    }
}
