package com.example.factcheckapp

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    private lateinit var switchDarkMode: Switch
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflar el layout directamente sin data binding
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Encontrar las vistas manualmente
        switchDarkMode = view.findViewById(R.id.switchDarkMode)

        sharedPreferences = requireContext().getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
        setupDarkModeSwitch()
    }

    private fun setupDarkModeSwitch() {
        val darkModeEnabled = sharedPreferences.getBoolean("dark_mode", false)
        switchDarkMode.isChecked = darkModeEnabled

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply()
            applyDarkMode(isChecked)
        }
    }

    private fun applyDarkMode(enabled: Boolean) {
        val mode = if (enabled) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}