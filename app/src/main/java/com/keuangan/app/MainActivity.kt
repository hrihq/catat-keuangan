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

    private val tabIndexToTag = mapOf(0 to "home", 1 to "stats", 3 to "target", 4 to "profile")
    private val tagToTabIndex = mapOf("home" to 0, "stats" to 1, "target" to 3, "profile" to 4)
    private val tabTags = listOf("home", "stats", "target", "profile")

    private var currentTag = "home"
    private var lastTabTag = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = TransactionRepository(this)

        if (savedInstanceState == null) {
            initFragments()
        } else {
            // Pulihkan tab yang sedang tampil agar indikator nav & state tidak salah
            // (mis. setelah ganti tema -> activity di-recreate).
            val visibleTag = tabTags.firstOrNull { tag ->
                supportFragmentManager.findFragmentByTag(tag)?.isVisible == true
            } ?: "home"
            currentTag = visibleTag
            lastTabTag = visibleTag
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStackImmediate(
                    null,
                    androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
            }
        }

        bottomNav = findViewById(R.id.bottomNavView)
        bottomNav.tabs = listOf(
            FloatingBottomNavView.Tab(R.drawable.ic_home, getString(R.string.tab_home)),
            FloatingBottomNavView.Tab(R.drawable.ic_pie_chart, getString(R.string.tab_analytics)),
            FloatingBottomNavView.Tab(R.drawable.ic_add, ""),
            FloatingBottomNavView.Tab(R.drawable.ic_target, getString(R.string.tab_target)),
            FloatingBottomNavView.Tab(R.drawable.ic_settings, getString(R.string.tab_settings))
        )
        bottomNav.centerActionIndex = 2
        bottomNav.onTabSelected = { index -> tabIndexToTag[index]?.let { showTab(it) } }
        bottomNav.onCenterAction = { showAddTransactionBottomSheet() }
        bottomNav.setSelectedIndex(tagToTabIndex[currentTag] ?: 0, animate = false)

        // Liquid glass: blur konten halaman di belakang nav.
        bottomNav.bindBackdrop(findViewById(R.id.fragmentContainer))

        if (!ThemeManager.hasSeenIntro(this)) {
            playIntroHalo()
        }
    }

    /** Animasi halo perdana + petunjuk cara pakai navbar. */
    private fun playIntroHalo() {
        val nav = findViewById<View>(R.id.bottomNav)

        com.keuangan.app.ui.Halo.pulse(nav, 600)

        android.widget.Toast.makeText(
            this, "Tarik bar bawah untuk berpindah tab", android.widget.Toast.LENGTH_LONG
        ).show()

        ThemeManager.markIntroSeen(this)
    }

    private fun initFragments() {
        val home = HomeFragment()
        val stats = StatisticsFragment()
        val target = TargetFragment()
        val profile = ProfileFragment()

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragmentContainer, home, "home")
        transaction.add(R.id.fragmentContainer, stats, "stats")
        transaction.add(R.id.fragmentContainer, target, "target")
        transaction.add(R.id.fragmentContainer, profile, "profile")
        transaction.hide(stats)
        transaction.hide(target)
        transaction.hide(profile)
        transaction.commit()
    }

    private fun showTab(tag: String) {
        if (currentTag == tag) return
        lastTabTag = tag

        val transaction = supportFragmentManager.beginTransaction()
        transaction.setCustomAnimations(R.anim.tab_in, R.anim.tab_out)
        supportFragmentManager.findFragmentByTag(currentTag)?.let { transaction.hide(it) }
        supportFragmentManager.findFragmentByTag(tag)?.let { transaction.show(it) }
        transaction.commit()

        currentTag = tag
        refreshTab(tag)
        bottomNav.refreshGlass()
    }

    /** Layar sub (riwayat / dompet) dibuka di atas tab dengan back stack. */
    private fun openSubScreen(fragment: androidx.fragment.app.Fragment, tag: String) {
        val current = supportFragmentManager.findFragmentByTag(currentTag) ?: return
        supportFragmentManager.beginTransaction()
            .hide(current)
            .add(R.id.fragmentContainer, fragment, tag)
            .addToBackStack(tag)
            .commit()
        currentTag = tag
        bottomNav.refreshGlass()
    }

    fun openHistory() = openSubScreen(RiwayatFragment(), "riwayat")
    fun openWallet() = openSubScreen(WalletFragment(), "wallet")

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            currentTag = lastTabTag
            bottomNav.setSelectedIndex(tagToTabIndex[lastTabTag] ?: 0)
            refreshTab(lastTabTag)
            bottomNav.refreshGlass()
        } else {
            super.onBackPressed()
        }
    }

    private fun refreshTab(tag: String) {
        when (tag) {
            "home" -> (supportFragmentManager.findFragmentByTag("home") as? HomeFragment)?.loadData()
            "stats" -> (supportFragmentManager.findFragmentByTag("stats") as? StatisticsFragment)?.loadData()
            "target" -> (supportFragmentManager.findFragmentByTag("target") as? TargetFragment)?.loadData()
        }
    }

    private fun refreshAll() {
        refreshTab("home")
        refreshTab("stats")
        refreshTab("target")
        (supportFragmentManager.findFragmentByTag("wallet") as? WalletFragment)?.loadData()
        (supportFragmentManager.findFragmentByTag("riwayat") as? RiwayatFragment)?.loadData()
    }

    /** Pasang pemisah ribuan otomatis (mis. 20000 -> 20.000) pada input angka. */
    private fun installThousandSeparator(editText: EditText) {
        val formatter = java.text.NumberFormat.getIntegerInstance(java.util.Locale("id", "ID"))
        editText.addTextChangedListener(object : android.text.TextWatcher {
            private var editing = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                if (editing || s == null) return
                val original = s.toString()
                val digits = original.filter { it.isDigit() }
                if (digits.isEmpty()) {
                    if (original.isNotEmpty()) {
                        editing = true
                        s.clear()
                        editing = false
                    }
                    return
                }
                val formatted = formatter.format(digits.toLong())
                if (formatted != original) {
                    editing = true
                    s.replace(0, s.length, formatted)
                    android.text.Selection.setSelection(s, s.length)
                    editing = false
                }
            }
        })
    }

    /** Baca angka berformat ribuan ("20.000") menjadi Double. */
    private fun parseFormattedNumber(text: String): Double {
        val clean = text.filter { it.isDigit() }
        return clean.toDoubleOrNull() ?: 0.0
    }

    /** Kategori pengeluaran untuk form catat transaksi. */
    private fun expenseCategoryOptions(): List<com.keuangan.app.ui.components.CategoryPicker.Option> =
        listOf(
            com.keuangan.app.ui.components.CategoryPicker.Option(
                "Makanan", R.drawable.ic_restaurant, R.color.brand_text_primary, R.color.brand_yellow
            ),
            com.keuangan.app.ui.components.CategoryPicker.Option(
                "Transport", R.drawable.ic_directions_car, R.color.brand_blue, R.color.brand_light_blue
            ),
            com.keuangan.app.ui.components.CategoryPicker.Option(
                "Tagihan", R.drawable.ic_receipt, R.color.brand_blue, R.color.brand_light_blue
            ),
            com.keuangan.app.ui.components.CategoryPicker.Option(
                "Belanja", R.drawable.ic_shopping, R.color.brand_blue, R.color.brand_light_blue
            ),
            com.keuangan.app.ui.components.CategoryPicker.Option(
                "Hiburan", R.drawable.ic_movie, R.color.brand_blue, R.color.brand_light_blue
            ),
            com.keuangan.app.ui.components.CategoryPicker.Option(
                "Kesehatan", R.drawable.ic_health, R.color.brand_blue, R.color.brand_light_blue
            ),
            com.keuangan.app.ui.components.CategoryPicker.Option(
                "Lainnya", R.drawable.ic_add, R.color.brand_blue, R.color.brand_light_blue
            )
        )

    /** Kategori pemasukan (terpisah dari kategori pengeluaran). */
    private fun incomeCategoryOptions(): List<com.keuangan.app.ui.components.CategoryPicker.Option> =
        listOf(
            com.keuangan.app.ui.components.CategoryPicker.Option(
                "Gaji", R.drawable.ic_wallet, R.color.brand_blue, R.color.brand_teal
            ),
            com.keuangan.app.ui.components.CategoryPicker.Option(
                "Bonus", R.drawable.ic_arrow_up, R.color.brand_blue, R.color.brand_light_blue
            ),
            com.keuangan.app.ui.components.CategoryPicker.Option(
                "Investasi", R.drawable.ic_bar_chart, R.color.brand_blue, R.color.brand_light_blue
            ),
            com.keuangan.app.ui.components.CategoryPicker.Option(
                "Lainnya", R.drawable.ic_add, R.color.brand_blue, R.color.brand_light_blue
            )
        )

    /** Dipakai EmptyState (Beranda) sebagai satu-satunya pintu tambah transaksi. */
    fun openAddTransaction() = showAddTransactionBottomSheet()

    private fun showAddTransactionBottomSheet() {
        val sheet = BottomSheetDialog(this).apply {
            setContentView(R.layout.bottom_sheet_add_transaction)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            // Batasi tinggi sheet agar kontenFormulir bisa discroll penuh (tidak terpotong).
            behavior.maxHeight = (resources.displayMetrics.heightPixels * 0.92f).toInt()
            behavior.skipCollapsed = true
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        }

        // Bentuk sheet: sudut atas membulat, latar mengikuti tema.
        sheet.window?.setBackgroundDrawableResource(android.R.color.transparent)
        sheet.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundResource(R.drawable.bg_sheet_top)

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
        val walletLabel = root.findViewById<TextView>(R.id.tvModalWalletValue)
        val walletRow = root.findViewById<View>(R.id.tvModalWallet)

        // Otomatis tambah pemisah ribuan (titik) saat mengetik angka.
        installThousandSeparator(amountInput)

        root.findViewById<View>(R.id.btnBackModal).setOnClickListener { sheet.dismiss() }

        var incomeMode = false
        var selectedCategory = "Makanan"
        val categoryPicker = root.findViewById<com.keuangan.app.ui.components.CategoryPicker>(R.id.categoryPicker)
        if (categoryPicker != null) {
            categoryPicker.setOptions(expenseCategoryOptions())
            categoryPicker.setSelected("Makanan", animate = false)
            categoryPicker.onSelectionChanged = { selectedCategory = it }
        }
        // Prefix "Rp" netral sampai ada isian, lalu ikut mode (merah/hijau/biru).
        fun applyAmountColors() {
            val hasInput = amountInput.text?.isNotEmpty() == true
            val modeColor = if (incomeMode) R.color.brand_blue else R.color.expense_red
            prefix.setTextColor(
                ContextCompat.getColor(root.context, if (hasInput) modeColor else R.color.brand_text_secondary)
            )
            amountInput.setTextColor(
                ContextCompat.getColor(root.context, if (hasInput) modeColor else R.color.brand_text_primary)
            )
        }
        amountInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) = applyAmountColors()
        })
        applyAmountColors()
        var selectedWallet = walletLabel.text.toString().ifEmpty { "Tunai" }
        var selectedDateMs = System.currentTimeMillis()

        val dateLabel = root.findViewById<TextView>(R.id.tvModalDateValue)
        root.findViewById<View>(R.id.tvModalDate).setOnClickListener {
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

        walletRow.setOnClickListener {
            lifecycleScope.launch {
                val wallets = repository.getWallets()
                if (wallets.isEmpty()) {
                    android.widget.Toast.makeText(
                        this@MainActivity, "Belum ada dompet. Tambah di Pengaturan, menu Dompet Saya.",
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
            // Kategori mengikuti jenis transaksi: pemasukan tidak boleh berkategori pengeluaran.
            if (categoryPicker != null) {
                val options = if (income) incomeCategoryOptions() else expenseCategoryOptions()
                categoryPicker.setOptions(options)
                categoryPicker.setSelected(options.first().name, animate = false)
            }
            applyAmountColors()
        }

        val saveButton = root.findViewById<MaterialButton>(R.id.btnModalSave)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(saveButton) { v, insets ->
            val bottomInset = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars()
            ).bottom
            (v.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
                it.bottomMargin = (18 * resources.displayMetrics.density).toInt() + bottomInset
                v.layoutParams = it
            }
            insets
        }
        saveButton.setOnClickListener {
            val amount = parseFormattedNumber(amountInput.text.toString())
            if (amount <= 0) {
                amountInput.error = "Masukkan jumlah"
                return@setOnClickListener
            }
            val title = titleInput?.text?.toString()?.takeIf { it.isNotEmpty() } ?: "Tanpa keterangan"
            val note = noteInput?.text?.toString()?.takeIf { it.isNotEmpty() } ?: ""
            val category = selectedCategory.ifEmpty { if (incomeMode) "Gaji" else "Makanan" }

            lifecycleScope.launch {
                repository.addTransaction(title, amount, incomeMode, category, note, selectedWallet, selectedDateMs)
                sheet.dismiss()
                refreshAll()
                Toast.makeText(applicationContext, R.string.toast_saved, Toast.LENGTH_SHORT).show()
            }
        }

        sheet.show()
    }
}
