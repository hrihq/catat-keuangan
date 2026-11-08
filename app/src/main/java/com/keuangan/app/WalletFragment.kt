package com.keuangan.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.keuangan.app.R
import java.text.NumberFormat
import java.util.Locale

class WalletFragment : Fragment() {

    private val currencyFormat: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = TransactionDb(requireContext())
        val (_, _, balance) = db.getSummary()

        val list = view.findViewById<LinearLayout>(R.id.walletsList)
        val card = TextView(requireContext()).apply {
            text = "Dompet: " + currencyFormat.format(balance)
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.brand_text_primary))
            setPadding(0, 20, 0, 20)
        }
        list.addView(card)

        view.findViewById<View>(R.id.btnAddWallet).setOnClickListener {
            android.widget.Toast.makeText(context, "Tambah dompet (fitur pending)", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
