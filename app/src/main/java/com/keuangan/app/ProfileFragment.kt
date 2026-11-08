package com.keuangan.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButtonToggleGroup
import com.keuangan.app.ui.ThemeManager
import java.io.File

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvAppVersion).text = "v1.0.0"

        val themeToggle = view.findViewById<MaterialButtonToggleGroup>(R.id.themeToggle)
        when (ThemeManager.mode(requireContext())) {
            ThemeManager.LIGHT -> themeToggle.check(R.id.btnThemeLight)
            ThemeManager.DARK -> themeToggle.check(R.id.btnThemeDark)
            else -> themeToggle.check(R.id.btnThemeSystem)
        }
        themeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                R.id.btnThemeLight -> ThemeManager.LIGHT
                R.id.btnThemeDark -> ThemeManager.DARK
                else -> ThemeManager.SYSTEM
            }
            if (mode == ThemeManager.mode(requireContext())) return@addOnButtonCheckedListener
            ThemeManager.setMode(requireContext(), mode)
            requireActivity().recreate()
        }

        view.findViewById<View>(R.id.btnExportCsv).setOnClickListener { exportCsv() }
    }

    private fun exportCsv() {
        val transactions = TransactionDb(requireContext()).getAll()
        if (transactions.isEmpty()) {
            Toast.makeText(requireContext(), "Belum ada transaksi untuk diekspor", Toast.LENGTH_SHORT).show()
            return
        }

        val csv = buildString {
            appendLine("Tanggal,Judul,Kategori,Jenis,Jumlah,Dompet,Catatan")
            transactions.forEach {
                appendLine(
                    "\"${it.date}\",\"${it.title}\",\"${it.category}\"," +
                        "\"${if (it.isIncome) "Pemasukan" else "Pengeluaran"}\"," +
                        "${it.amount.toInt()},\"${it.wallet}\",\"${it.note}\""
                )
            }
        }

        val file = File(requireContext().filesDir, "catat-keuangan.csv")
        file.writeText(csv)

        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Ekspor CSV"))
    }
}
