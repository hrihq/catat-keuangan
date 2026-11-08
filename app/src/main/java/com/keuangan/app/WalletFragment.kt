package com.keuangan.app

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.keuangan.app.data.TransactionRepository
import com.keuangan.app.ui.components.IconTile
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class WalletFragment : Fragment() {

    private val repository by lazy { TransactionRepository(requireContext().applicationContext) }
    private val currencyFormat: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }

    private lateinit var list: LinearLayout
    private var entrancePlayed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_wallet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list = view.findViewById(R.id.walletsList)
        view.findViewById<View>(R.id.btnAddWallet).setOnClickListener { showAddDialog() }
        view.findViewById<View>(R.id.btnWalletBack).setOnClickListener {
            requireActivity().onBackPressed()
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

    fun loadData() {
        if (!isAdded) return
        viewLifecycleOwner.lifecycleScope.launch {
            val wallets = repository.getWallets()
            if (!isAdded) return@launch
            list.removeAllViews()
            wallets.forEach { wallet ->
                list.addView(buildRow(wallet, repository.getWalletBalance(wallet.name)))
            }
        }
    }

    private fun buildRow(wallet: Wallet, balance: Double): View {
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.brand_surface))
            radius = dp(20).toFloat()
            cardElevation = dp(1).toFloat()
            strokeColor = ContextCompat.getColor(context, R.color.brand_outline)
            strokeWidth = dp(1)
        }

        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(14), dp(6), dp(14))
        }

        val iconTile = IconTile(requireContext()).apply {
            bind(R.drawable.ic_wallet, R.color.brand_blue, R.color.brand_light_blue)
        }

        val textColumn = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(12)
            }
        }
        val nameText = TextView(requireContext()).apply {
            text = wallet.name
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_text_primary))
        }
        val balanceText = TextView(requireContext()).apply {
            text = currencyFormat.format(balance)
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_text_secondary))
        }
        textColumn.addView(nameText)
        textColumn.addView(balanceText)

        val deleteButton = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            setImageResource(R.drawable.ic_close)
            setColorFilter(ContextCompat.getColor(requireContext(), R.color.brand_text_secondary))
            setPadding(dp(10), dp(10), dp(10), dp(10))
            contentDescription = "Hapus dompet ${wallet.name}"
            setOnClickListener { confirmDelete(wallet) }
        }

        row.addView(iconTile)
        row.addView(textColumn)
        row.addView(deleteButton)
        card.addView(row)
        return card
    }

    private fun showAddDialog() {
        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.sheet_add_wallet, null)
        sheet.setContentView(view)

        val nameInput = view.findViewById<EditText>(R.id.etWalletName)
        val balanceInput = view.findViewById<EditText>(R.id.etWalletBalance)
        val saveButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSaveWallet)

        saveButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                nameInput.error = "Nama wajib diisi"
                return@setOnClickListener
            }
            val balance = balanceInput.text.toString().toDoubleOrNull() ?: 0.0
            viewLifecycleOwner.lifecycleScope.launch {
                repository.addWallet(name, balance)
                sheet.dismiss()
                loadData()
            }
        }
        view.findViewById<View>(R.id.btnCancelWallet).setOnClickListener { sheet.dismiss() }
        sheet.show()
    }

    private fun confirmDelete(wallet: Wallet) {
        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.sheet_confirm_delete, null)
        sheet.setContentView(view)
        view.findViewById<TextView>(R.id.tvConfirmTitle).text = "Hapus dompet"
        view.findViewById<TextView>(R.id.tvConfirmMessage).text = "Hapus dompet \"${wallet.name}\"?"
        view.findViewById<View>(R.id.btnCancelConfirm).setOnClickListener { sheet.dismiss() }
        view.findViewById<View>(R.id.btnConfirmDelete).setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                repository.deleteWallet(wallet.id)
                sheet.dismiss()
                loadData()
            }
        }
        sheet.show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
