package me.osku.freeshiamy.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import me.osku.freeshiamy.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.ime_preferences, rootKey)
    }
}

