package me.lucky.red

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import java.lang.NumberFormatException

import me.lucky.red.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    companion object {
        private val PERMISSIONS = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
        )
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Preferences
    private lateinit var window: PopupWindow
    private var roleManager: RoleManager? = null

    private val registerForCallRedirectionRole =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    private val registerForGeneralPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    private val registerForDrawOverlays =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        setup()
    }

    override fun onDestroy() {
        super.onDestroy()
        window.cancel()
    }

    private fun init() {
        prefs = Preferences(this)
        window = PopupWindow(this, null)
        roleManager = getSystemService(RoleManager::class.java)
        binding.apply {
            redirectionDelay.value = (prefs.redirectionDelay / 1000).toFloat()
            popupPosition.editText?.setText(prefs.popupPosition.toString())
            fallback.isChecked = prefs.isFallbackChecked
            toggle.isChecked = prefs.isEnabled
        }
    }

    private fun setup() {
        binding.apply {
            redirectionDelay.setLabelFormatter {
                String.format("%.1f", it)
            }
            redirectionDelay.addOnChangeListener { _, value, _ ->
                prefs.redirectionDelay = (value * 1000).toLong()
            }
            popupPosition.setEndIconOnClickListener {
                window.preview()
            }
            popupPosition.editText?.doAfterTextChanged {
                try {
                    prefs.popupPosition = it?.toString()?.toInt() ?: return@doAfterTextChanged
                } catch (exc: NumberFormatException) {}
            }
            fallback.setOnCheckedChangeListener { _, isChecked ->
                prefs.isFallbackChecked = isChecked
            }
            toggle.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked && !hasPermissions()) {
                    toggle.isChecked = false
                    requestPermissions()
                    return@setOnCheckedChangeListener
                }
                prefs.isEnabled = isChecked
            }
        }
    }

    private fun requestPermissions() {
        when {
            !hasGeneralPermissions() -> requestGeneralPermissions()
            !hasDrawOverlays() -> requestDrawOverlays()
            !hasCallRedirectionRole() -> requestCallRedirectionRole()
        }
    }

    private fun hasPermissions(): Boolean {
        return hasGeneralPermissions() && hasDrawOverlays() && hasCallRedirectionRole()
    }

    private fun requestDrawOverlays() {
        registerForDrawOverlays.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
    }

    private fun requestGeneralPermissions() {
        registerForGeneralPermissions.launch(PERMISSIONS)
    }

    private fun hasGeneralPermissions(): Boolean {
        return !PERMISSIONS.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
    }

    private fun hasDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun requestCallRedirectionRole() {
        registerForCallRedirectionRole
            .launch(roleManager?.createRequestRoleIntent(RoleManager.ROLE_CALL_REDIRECTION))
    }

    private fun hasCallRedirectionRole(): Boolean {
        return roleManager?.isRoleHeld(RoleManager.ROLE_CALL_REDIRECTION) ?: false
    }
}
