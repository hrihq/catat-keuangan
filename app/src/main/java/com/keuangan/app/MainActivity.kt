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

        if (!ThemeManager.hasSeenIntro(this)) {
            playIntroHalo()
        }
    }

    /** Animasi halo perdana + petunjuk cara pakai navbar. */
    private fun playIntroHalo() {
        val fabHalo = findViewById<View>(R.id.fabHalo)
        val fab = findViewById<View>(R.id.fabAdd)
        val nav = findViewById<View>(R.id.bottomNav)

        repeat(2) { index ->
            fabHalo.postDelayed({
                fabHalo.animate().cancel()
                fabHalo.scaleX = 1f
                fabHalo.scaleY = 1f
                fabHalo.alpha = 0.55f
                fabHalo.animate()
                    .scaleX(1.5f)
                    .scaleY(1.5f)
                    .alpha(0f)
                    .setDuration(900)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }, (index * 1000).toLong())
        }

        com.keuangan.app.ui.Halo.pulse(fab, 250)
        com.keuangan.app.ui.Halo.pulse(nav, 600)

        android.widget.Toast.makeText(
            this, "Tarik bar bawah untuk berpindah tab", android.widget.Toast.LENGTH_LONG
        ).show()

        ThemeManager.markIntroSeen(this)
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
            window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
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
                if (wallets.isEmpty()) {
                    android.widget.Toast.makeText(
                        this@MainActivity, "Belum ada dompet. Tambah di tab Dompet.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                val picker = BottomSheetDialog(this@MainActivity)
                val pickerView = layoutInflater.inflate(R.layout.sheet_wallet_picker, null)
                picker.setContentView(pickerView)
                val list = pickerView.findViewById<android.widget.LinearLayout>(R.id.walletOptions)
                wallets.forEach { wallet ->
                    val row = android.widget.TextView(this@MainActivity).apply {
                        text = wallet.name
                        textSize = 15f
                        setPadding(
                            (16 * resources.displayMetrics.density).toInt(),
                            (14 * resources.displayMetrics.density).toInt(),
                            (16 * resources.displayMetrics.density).toInt(),
                            (14 * resources.displayMetrics.density).toInt()
                        )
                        setTextColor(ContextCompat.getColor(context, R.color.brand_text_primary))
                        setBackgroundResource(R.drawable.bg_transaction_row)
                        setOnClickListener {
                            selectedWallet = wallet.name
                            walletLabel.text = selectedWallet
                            picker.dismiss()
                        }
                    }
                    list.addView(row)
                }
                picker.show()
            }
        }

        val segmentedToggle = root.findViewById<com.keuangan.app.ui.components.SegmentedToggle>(R.id.segmentedMode)
        segmentedToggle.onModeChanged = { income ->
            incomeMode = income
            val amountColor = if (income) R.color.brand_blue else R.color.expense_red
            val categoryColor = if (income) R.color.brand_blue else R.color.brand_text_secondary
            categories.forEach { it.setTextColor(ContextCompat.getColor(root.context, categoryColor)) }
            prefix.setTextColor(ContextCompat.getColor(root.context, amountColor))
            amountInput.setTextColor(ContextCompat.getColor(root.context, amountColor))
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
                // refresh via tag lookup too, in case the fragment reference is stale
                homeFragment?.loadData()
                (supportFragmentManager.findFragmentByTag("home") as? HomeFragment)?.loadData()
                (supportFragmentManager.findFragmentByTag("stats") as? StatisticsFragment)?.loadData()
                (supportFragmentManager.findFragmentByTag("wallet") as? WalletFragment)?.loadData()
                Toast.makeText(applicationContext, R.string.toast_saved, Toast.LENGTH_SHORT).show()
            }
        }

        sheet.show()
    }
}
