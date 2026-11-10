package com.keuangan.app

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.keuangan.app.data.TransactionRepository
import com.keuangan.app.ui.components.EmptyState
import com.keuangan.app.ui.components.IconTile
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/** Target keuangan: daftar goal + tambah + hapus. */
class TargetFragment : Fragment() {

    private val repository by lazy { TransactionRepository(requireContext().applicationContext) }
    private val currencyFormat: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }

    private lateinit var goalList: LinearLayout
    private lateinit var emptyGoals: EmptyState

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
            val goals = repository.getGoals()
            if (!isAdded) return@launch
            render(goals)
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
        goals.forEach { goal ->
            goalList.addView(goalRow(goal))
        }
    }

    private fun goalRow(goal: Goal): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_transaction_row)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
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

        column.addView(TextView(requireContext()).apply {
            text = goal.name
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.brand_text_primary))
        })

        val progress = ProgressBar(
            requireContext(), null, android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progress = goal.progress
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

        rightColumn.addView(TextView(requireContext()).apply {
            text = "${goal.progress}%"
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.brand_blue))
        })

        rightColumn.addView(TextView(requireContext()).apply {
            text = "Hapus"
            textSize = 12f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.danger_text))
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_close, 0, 0, 0)
            compoundDrawablePadding = dp(4)
            setPadding(dp(10), dp(4), dp(4), dp(4))
            setOnClickListener { confirmDelete(goal) }
        })

        row.addView(tile)
        row.addView(column)
        row.addView(rightColumn)
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
            viewLifecycleOwner.lifecycleScope.launch {
                repository.addGoal(name, target, saved, "target")
                sheet.dismiss()
                loadData()
            }
        }
        sheet.show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
