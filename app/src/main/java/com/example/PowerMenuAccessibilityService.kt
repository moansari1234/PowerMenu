package com.example

import android.accessibilityservice.AccessibilityService
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

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
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
            // GLOBAL_ACTION_POWER_MENU is 6 in Android AccessibilityService
            return instance?.performGlobalAction(6) ?: false
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
