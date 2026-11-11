package com.keuangan.app

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.keuangan.app.data.TransactionRepository
import com.keuangan.app.ui.components.ConfettiView
import com.keuangan.app.ui.components.EmptyState
import com.keuangan.app.ui.components.IconTile
import com.keuangan.app.util.Haptics
import com.keuangan.app.util.Motion
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/** Target keuangan: daftar goal + tambah tabungan + riwayat + hapus. */
class TargetFragment : Fragment() {

    private val repository by lazy { TransactionRepository(requireContext().applicationContext) }
    private val currencyFormat: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }

    private lateinit var goalList: LinearLayout
    private lateinit var emptyGoals: EmptyState
    private lateinit var confetti: ConfettiView

    private var entrancePlayed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_target, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        goalList = view.findViewById(R.id.goalList)
        emptyGoals = view.findViewById(R.id.emptyGoals)
        confetti = view.findViewById(R.id.confettiView)
        emptyGoals.onActionClick = { showAddGoal() }
        view.findViewById<View>(R.id.btnAddGoal).setOnClickListener { showAddGoal() }
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
        if (!Motion.shouldAnimate(requireContext())) {
            root.alpha = 1f
            root.translationY = 0f
            return
        }
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

    fun loadData(celebrate: Boolean = false) {
        if (!isAdded) return
        viewLifecycleOwner.lifecycleScope.launch {
            val goals = repository.getGoals()
            if (!isAdded) return@launch
            render(goals)
            if (celebrate) {
                confetti.burst()
                Haptics.confirm(confetti)
                android.widget.Toast.makeText(
                    requireContext(),
                    "Selamat! Target tercapai 🎉",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun render(goals: List<Goal>) {
        goalList.removeAllViews()
        if (goals.isEmpty()) {
            emptyGoals.bind("Belum ada target keuangan", "Tambah Target")
            emptyGoals.visibility = View.VISIBLE
        } else {
            emptyGoals.visibility = View.GONE
        }
        goals.forEachIndexed { index, goal ->
            val row = goalRow(goal)
            goalList.addView(row)
            Motion.staggerIn(row, index)
        }
    }

    private fun goalRow(goal: Goal): View {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_transaction_row)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
            isClickable = true
            isFocusable = true
        }

        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tile = IconTile(requireContext()).apply {
            bind(R.drawable.ic_target, R.color.brand_blue, R.color.brand_light_blue)
        }

        val column = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(12)
                marginEnd = dp(8)
            }
        }

        val nameRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        nameRow.addView(TextView(requireContext()).apply {
            text = goal.name
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.brand_text_primary))
        })

        if (goal.isReached) {
            nameRow.addView(TextView(requireContext()).apply {
                text = "Tercapai"
                textSize = 10f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.income_green))
                setPadding(dp(8), dp(2), dp(8), dp(2))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(ContextCompat.getColor(context, R.color.income_container))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = dp(8) }
            })
        }

        column.addView(nameRow)

        val progress = ProgressBar(
            requireContext(), null, android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progress = 0
            progressDrawable = requireContext().getDrawable(R.drawable.progress_horizontal)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(10)
            ).apply { topMargin = dp(8) }
        }
        column.addView(progress)

        column.addView(TextView(requireContext()).apply {
            text = "${currencyFormat.format(goal.savedAmount)} dari ${currencyFormat.format(goal.targetAmount)}"
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.brand_text_secondary))
            setPadding(0, dp(6), 0, 0)
        })

        val rightColumn = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val percentText = TextView(requireContext()).apply {
            text = "0%"
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.brand_blue))
            gravity = Gravity.END
        }
        rightColumn.addView(percentText)

        val actionRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
        }

        actionRow.addView(TextView(requireContext()).apply {
            text = "Tambah Tabungan"
            textSize = 11f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.brand_blue))
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_add, 0, 0, 0)
            compoundDrawablePadding = dp(3)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener {
                Haptics.tap(this)
                showAddSaving(goal)
            }
        })

        actionRow.addView(TextView(requireContext()).apply {
            text = "Hapus"
            textSize = 11f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.danger_text))
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_close, 0, 0, 0)
            compoundDrawablePadding = dp(3)
            setPadding(dp(8), dp(4), dp(4), dp(4))
            setOnClickListener {
                Haptics.tap(this)
                confirmDelete(goal)
            }
        })

        rightColumn.addView(actionRow)
        header.addView(tile)
        header.addView(column)
        header.addView(rightColumn)
        card.addView(header)

        // Riwayat tabungan (muncul saat kartu diklik).
        val historyContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, dp(10), 0, 0)
        }
        card.addView(historyContainer)

        card.setOnClickListener {
            Haptics.tap(card)
            if (Motion.shouldAnimate(requireContext())) {
                card.animate().cancel()
                card.animate().scaleX(0.98f).scaleY(0.98f).setDuration(70)
                    .withEndAction {
                        card.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    }.start()
            }
            toggleHistory(historyContainer, goal, isOpen = historyContainer.visibility != View.VISIBLE)
        }
        card.setOnLongClickListener {
            Haptics.tap(card)
            showEditGoal(goal)
            true
        }

        animateProgress(progress, percentText, goal.progress)
        return card
    }

    private fun animateProgress(progressBar: ProgressBar, percentText: TextView, target: Int) {
        if (!Motion.shouldAnimate(requireContext())) {
            progressBar.progress = target
            percentText.text = "$target%"
            return
        }
        android.animation.ValueAnimator.ofFloat(0f, target.toFloat()).apply {
            duration = 600
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Float
                progressBar.progress = value.toInt()
                percentText.text = "${value.toInt()}%"
            }
            start()
        }
    }

    private fun toggleHistory(
        container: LinearLayout,
        goal: Goal,
        isOpen: Boolean
    ) {
        if (Motion.shouldAnimate(requireContext())) {
            android.transition.TransitionManager.beginDelayedTransition(
                goalList,
                android.transition.AutoTransition().apply { duration = 220 }
            )
        }
        container.visibility = if (isOpen) View.VISIBLE else View.GONE
        if (isOpen && container.childCount == 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                val history = repository.getGoalHistory(goal.id)
                if (!isAdded) return@launch
                if (history.isEmpty()) {
                    container.addView(historyLabel("Belum ada tabungan. Tekan \"Tambah Tabungan\"."))
                } else {
                    history.forEachIndexed { index, saving ->
                        val row = historyRow(saving)
                        container.addView(row)
                        Motion.staggerIn(row, index, distanceDp = 6f)
                    }
                }
            }
        }
    }

    private fun historyLabel(text: String) = TextView(requireContext()).apply {
        this.text = text
        textSize = 12f
        setTextColor(ContextCompat.getColor(context, R.color.brand_text_secondary))
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun historyRow(saving: GoalSaving): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }
        val detail = (saving.note.ifEmpty { "Tabungan" }) + " · " + TransactionDb.formatDate(saving.dateMs)
        row.addView(TextView(requireContext()).apply {
            text = detail
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.brand_text_secondary))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(TextView(requireContext()).apply {
            text = "+${currencyFormat.format(saving.amount)}"
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.income_green))
        })
        return row
    }

    private fun confirmDelete(goal: Goal) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus target")
            .setMessage("Hapus \"${goal.name}\"?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.deleteGoal(goal.id)
                    loadData()
                }
            }
            .show()
    }

    private fun showEditGoal(goal: Goal) {
        val sheet = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.sheet_add_goal, null)
        sheet.setContentView(view)
        view.findViewById<TextView>(R.id.etGoalName).setText(goal.name)
        view.findViewById<TextView>(R.id.etGoalTarget).setText(goal.targetAmount.toInt().toString())
        view.findViewById<TextView>(R.id.etGoalSaved).apply {
            setText(goal.savedAmount.toInt().toString())
            isEnabled = false
        }
        view.findViewById<MaterialButton>(R.id.btnGoalSave).apply {
            text = "Simpan Perubahan"
            setOnClickListener {
                val name = view.findViewById<TextView>(R.id.etGoalName).text?.toString().orEmpty().trim()
                val target = view.findViewById<TextView>(R.id.etGoalTarget).text?.toString()
                    ?.toDoubleOrNull() ?: 0.0
                if (name.isEmpty() || target <= 0) {
                    view.findViewById<TextView>(R.id.etGoalName).error =
                        if (name.isEmpty()) "Isi nama target" else null
                    view.findViewById<TextView>(R.id.etGoalTarget).error =
                        if (target <= 0) "Isi target dana" else null
                    return@setOnClickListener
                }
                Haptics.confirm(this)
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.updateGoal(goal.id, name, target)
                    sheet.dismiss()
                    loadData()
                }
            }
        }
        sheet.show()
    }

    private fun showAddGoal() {
        val sheet = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.sheet_add_goal, null)
        sheet.setContentView(view)

        view.findViewById<View>(R.id.btnGoalSave).setOnClickListener {
            val name = view.findViewById<TextView>(R.id.etGoalName).text?.toString().orEmpty().trim()
            val target = view.findViewById<TextView>(R.id.etGoalTarget).text?.toString()
                ?.toDoubleOrNull() ?: 0.0
            val saved = view.findViewById<TextView>(R.id.etGoalSaved).text?.toString()
                ?.toDoubleOrNull() ?: 0.0
            if (name.isEmpty() || target <= 0) {
                view.findViewById<TextView>(R.id.etGoalName).error =
                    if (name.isEmpty()) "Isi nama target" else null
                view.findViewById<TextView>(R.id.etGoalTarget).error =
                    if (target <= 0) "Isi target dana" else null
                return@setOnClickListener
            }
            Haptics.confirm(it)
            viewLifecycleOwner.lifecycleScope.launch {
                repository.addGoal(name, target, saved, "target")
                sheet.dismiss()
                loadData()
            }
        }
        sheet.show()
    }

    private fun showAddSaving(goal: Goal) {
        val sheet = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.sheet_add_saving, null)
        sheet.setContentView(view)

        view.findViewById<TextView>(R.id.tvSavingGoal).text =
            "${goal.name} · ${currencyFormat.format(goal.savedAmount)} / ${currencyFormat.format(goal.targetAmount)}"

        val amountInput = view.findViewById<EditText>(R.id.etSavingAmount)
        val noteInput = view.findViewById<EditText>(R.id.etSavingNote)

        val quickChips = mapOf(
            R.id.chip10k to 10_000,
            R.id.chip25k to 25_000,
            R.id.chip50k to 50_000,
            R.id.chip100k to 100_000
        )
        quickChips.forEach { (id, value) ->
            view.findViewById<View>(id).setOnClickListener { chip ->
                Haptics.tap(chip)
                amountInput.setText(value.toString())
                amountInput.setSelection(amountInput.text?.length ?: 0)
            }
        }

        var selectedWallet = "Tunai"
        val walletLabel = view.findViewById<TextView>(R.id.tvSavingWalletValue)
        view.findViewById<View>(R.id.tvSavingWallet).setOnClickListener { row ->
            Haptics.tap(row)
            viewLifecycleOwner.lifecycleScope.launch {
                val wallets = repository.getWallets()
                if (wallets.isEmpty()) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Belum ada dompet. Tambah di Pengaturan, menu Dompet Saya.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                val picker = BottomSheetDialog(requireContext())
                val pickerView = layoutInflater.inflate(R.layout.sheet_wallet_picker, null)
                picker.setContentView(pickerView)
                val list = pickerView.findViewById<LinearLayout>(R.id.walletOptions)
                wallets.forEach { wallet ->
                    list.addView(TextView(requireContext()).apply {
                        text = wallet.name
                        textSize = 15f
                        setTextColor(ContextCompat.getColor(context, R.color.brand_text_primary))
                        setPadding(dp(16), dp(14), dp(16), dp(14))
                        setBackgroundResource(R.drawable.bg_transaction_row)
                        setOnClickListener {
                            selectedWallet = wallet.name
                            walletLabel.text = selectedWallet
                            picker.dismiss()
                        }
                    })
                }
                picker.show()
            }
        }

        var selectedDateMs = System.currentTimeMillis()
        val dateLabel = view.findViewById<TextView>(R.id.tvSavingDateValue)
        dateLabel.text = TransactionDb.formatDate(selectedDateMs)
        view.findViewById<View>(R.id.tvSavingDate).setOnClickListener { row ->
            Haptics.tap(row)
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMs }
            android.app.DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedDateMs = Calendar.getInstance().apply {
                        set(year, month, day, 12, 0, 0)
                    }.timeInMillis
                    dateLabel.text = TransactionDb.formatDate(selectedDateMs)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val saveButton = view.findViewById<MaterialButton>(R.id.btnSavingSave)
        saveButton.setOnClickListener {
            val amount = amountInput.text?.toString()?.toDoubleOrNull() ?: 0.0
            if (amount <= 0) {
                amountInput.error = "Masukkan nominal"
                return@setOnClickListener
            }
            Haptics.confirm(saveButton)
            saveButton.isEnabled = false
            saveButton.text = "Tersimpan"
            if (Motion.shouldAnimate(requireContext())) {
                saveButton.animate().cancel()
                saveButton.scaleX = 0.96f
                saveButton.scaleY = 0.96f
                saveButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(250)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.4f))
                    .start()
            }
            val note = noteInput.text?.toString().orEmpty().trim()
            val becomesReached = !goal.isReached && goal.targetAmount > 0 &&
                goal.savedAmount + amount >= goal.targetAmount
            viewLifecycleOwner.lifecycleScope.launch {
                repository.addGoalSaving(goal.id, amount, note, selectedDateMs)
                sheet.dismiss()
                loadData(celebrate = becomesReached)
            }
        }
        sheet.show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
