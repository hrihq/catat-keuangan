package com.keuangan.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.keuangan.app.R

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvAppVersion).text = "v1.0.0"
        view.findViewById<View>(R.id.btnThemeLight).setOnClickListener {
            android.widget.Toast.makeText(context, "Mode terang (pending)", android.widget.Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.btnThemeDark).setOnClickListener {
            android.widget.Toast.makeText(context, "Mode gelap (pending)", android.widget.Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.btnThemeSystem).setOnClickListener {
            android.widget.Toast.makeText(context, "Mode sistem (pending)", android.widget.Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.btnExportCsv).setOnClickListener {
            android.widget.Toast.makeText(context, "Ekspor CSV (pending)", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
