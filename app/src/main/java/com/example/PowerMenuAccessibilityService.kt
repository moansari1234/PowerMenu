package com.example

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.content.Context
import android.content.ComponentName
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent

class PowerMenuAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No events to handle
    }

    override fun onInterrupt() {
        // No interrupt action needed
    }

    private var buttonCallback: Any? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        // Without this, the accessibility button / shortcut the app tells users to set up
        // does nothing: tapping it must open the power dialog.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val callback = object : AccessibilityButtonController.AccessibilityButtonCallback() {
                override fun onClicked(controller: AccessibilityButtonController) {
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
                }
            }
            accessibilityButtonController.registerAccessibilityButtonCallback(callback)
            buttonCallback = callback
        }
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (buttonCallback as? AccessibilityButtonController.AccessibilityButtonCallback)?.let {
                accessibilityButtonController.unregisterAccessibilityButtonCallback(it)
            }
        }
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    companion object {
        private var instance: PowerMenuAccessibilityService? = null

        /**
         * Checks if the service is currently running and bound.
         */
        fun isServiceRunning(): Boolean = instance != null

        /**
         * Triggers the system power menu.
         * Returns true if successful, false otherwise.
         */
        fun showPowerMenu(): Boolean {
            return instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG) ?: false
        }

        /**
         * Helper to check if the accessibility service is enabled in the system settings.
         */
        fun isServiceEnabled(context: Context): Boolean {
            val expectedComponentName = ComponentName(context, PowerMenuAccessibilityService::class.java)
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)
            while (colonSplitter.hasNext()) {
                val componentNameString = colonSplitter.next()
                val enabledService = ComponentName.unflattenFromString(componentNameString)
                if (enabledService != null && enabledService == expectedComponentName) {
                    return true
                }
            }
            return false
        }
    }
}
