package me.osku.freeshiamy

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import me.osku.freeshiamy.settings.SettingsActivity

class TestPanelActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_panel)

        val singleInput = findViewById<TextInputEditText>(R.id.test_input_single)
        val privateInput = findViewById<TextInputEditText>(R.id.test_input_private)
        val multiInput = findViewById<TextInputEditText>(R.id.test_input_multi)
        val status = findViewById<TextView>(R.id.ime_status)

        fun focus(view: TextInputEditText) {
            view.requestFocus()
            view.post {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(view, 0)
            }
        }

        fun refreshStatus() {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            val enabled = imm.enabledInputMethodList.any { it.packageName == packageName }
            val defaultIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            status.text = buildString {
                append("Enabled: ").append(if (enabled) "Yes" else "No")
                append("\nDefault IME: ").append(defaultIme ?: "(unknown)")
            }
        }

        refreshStatus()

        singleInput.setOnEditorActionListener { _, actionId, event ->
            val isEnterKey =
                event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
            val shouldJump =
                isEnterKey || actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEND
            if (!shouldJump) return@setOnEditorActionListener false

            focus(multiInput)
            true
        }

        findViewById<MaterialButton>(R.id.btn_open_ime_settings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        findViewById<MaterialButton>(R.id.btn_show_ime_picker).setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        findViewById<MaterialButton>(R.id.btn_open_app_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btn_focus).setOnClickListener {
            focus(singleInput)
        }

        findViewById<MaterialButton>(R.id.btn_clear).setOnClickListener {
            singleInput.setText("")
            privateInput.setText("")
            multiInput.setText("")
            focus(singleInput)
        }

        findViewById<MaterialButton>(R.id.btn_refresh).setOnClickListener {
            refreshStatus()
        }

        // Improve tap-to-focus on the whole panel.
        findViewById<View>(R.id.root).setOnClickListener {
            singleInput.requestFocus()
        }
    }
}
