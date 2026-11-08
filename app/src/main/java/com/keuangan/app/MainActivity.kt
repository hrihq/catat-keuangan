package com.keuangan.app

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.R as MaterialR
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors

class MainActivity : AppCompatActivity() {

    private lateinit var db: TransactionDb
    private var currentFragment = "home"
    private var homeFragment: HomeFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = TransactionDb(this)

        if (savedInstanceState == null) {
            initFragments()
        }

        setupTabListeners()
        updateTabIndicator("home")
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
        data class Tab(val rootId: Int, val iconId: Int, val labelId: Int)

        val tabs = listOf(
            Tab(R.id.tabHome, R.id.navIconHome, R.id.navLabelHome),
            Tab(R.id.tabStats, R.id.navIconStats, R.id.navLabelStats),
            Tab(R.id.tabWallet, R.id.navIconWallet, R.id.navLabelWallet),
            Tab(R.id.tabProfile, R.id.navIconProfile, R.id.navLabelProfile)
        )
        val activeRootId = when (tag) {
            "home" -> R.id.tabHome
            "stats" -> R.id.tabStats
            "wallet" -> R.id.tabWallet
            else -> R.id.tabProfile
        }

        for (tab in tabs) {
            val icon = findViewById<ImageView>(tab.iconId)
            val label = findViewById<TextView>(tab.labelId)
            val active = tab.rootId == activeRootId
            if (active) {
                icon.setBackgroundResource(R.drawable.bg_nav_active)
                val color = MaterialColors.getColor(icon, MaterialR.attr.colorPrimary)
                icon.setColorFilter(color)
                label.setTextColor(color)
            } else {
                icon.background = null
                val color = MaterialColors.getColor(icon, MaterialR.attr.colorOnSurfaceVariant)
                icon.setColorFilter(color)
                label.setTextColor(color)
            }
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
