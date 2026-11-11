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
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.keuangan.app.ui.ThemeManager
import com.keuangan.app.util.Haptics
import com.keuangan.app.util.Motion
import java.io.File

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    private var entrancePlayed = false

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
            Haptics.tap(themeToggle)
            applyTheme(mode)
        }

        view.findViewById<View>(R.id.btnExportCsv).setOnClickListener { button ->
            Haptics.tap(button)
            press(button)
            exportCsv(button as MaterialButton)
        }
        view.findViewById<View>(R.id.rowWallet).setOnClickListener {
            (activity as? MainActivity)?.openWallet()
        }
        view.findViewById<View>(R.id.rowHelp).setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Bantuan")
                .setMessage(
                    "Tambah transaksi: tekan tombol + di tengah bar bawah.\n" +
                        "Riwayat: \"Lihat semua\" di Beranda.\n" +
                        "Dompet: Pengaturan → Dompet Saya."
                )
                .setPositiveButton("Mengerti", null)
                .show()
        }
        view.findViewById<View>(R.id.rowAbout).setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Tentang aplikasi")
                .setMessage("Catat Keuangan v1.0.0\nPencatat keuangan pribadi, data tersimpan lokal di perangkat.")
                .setPositiveButton("Tutup", null)
                .show()
        }
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

    /** Ganti tema dengan fade halus sebelum layar dibangun ulang. */
    private fun applyTheme(mode: Int) {
        val root = view
        if (root == null || !Motion.shouldAnimate(requireContext())) {
            ThemeManager.setMode(requireContext(), mode)
            requireActivity().recreate()
            return
        }
        root.animate().cancel()
        root.animate()
            .alpha(0f)
            .setDuration(160)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                ThemeManager.setMode(requireContext(), mode)
                requireActivity().recreate()
            }
            .start()
    }

    /** Umpan balik tekan ringan. */
    private fun press(view: View) {
        if (!Motion.shouldAnimate(requireContext())) return
        view.animate().cancel()
        view.animate()
            .scaleX(0.97f)
            .scaleY(0.97f)
            .setDuration(80)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(160)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun exportCsv(button: MaterialButton) {
        if (button.tag == true) return
        button.tag = true
        val originalText = button.text
        button.isEnabled = false
        button.text = "Menyiapkan…"

        button.postDelayed({
            val transactions = TransactionDb(requireContext()).getAll()
            if (transactions.isEmpty()) {
                Toast.makeText(requireContext(), "Belum ada transaksi untuk diekspor", Toast.LENGTH_SHORT).show()
                button.text = originalText
                button.isEnabled = true
                button.tag = false
                return@postDelayed
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
            button.text = originalText
            button.isEnabled = true
            button.tag = false
            startActivity(Intent.createChooser(intent, "Ekspor CSV"))
        }, 220)
    }
}
