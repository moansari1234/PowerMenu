#!/bin/bash
# Emulator test for PowerMenu: install, enable the accessibility service, open the power dialog.
source scripts/emulator-lib.sh
PKG=com.aistudio.powermenu.qzpxrt.debug  # debug builds carry the .debug suffix
SVC=$PKG/com.example.PowerMenuAccessibilityService
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n $PKG/com.example.MainActivity; sleep 3; shot main-disabled
check "setup screen shown before service is enabled" "screen_has 'SERVICE SETUP REQUIRED'" "saw: $(texts)"
adb shell settings put secure enabled_accessibility_services $SVC
adb shell settings put secure accessibility_enabled 1; sleep 3
adb shell am start -W -n $PKG/com.example.MainActivity; sleep 2
adb shell input keyevent KEYCODE_HOME; sleep 1; adb shell am start -W -n $PKG/com.example.MainActivity; sleep 3; shot main-enabled
check "app sees service connected" "screen_has 'TAP TO OPEN POWER MENU'" "saw: $(texts)"
tap text='Open Power Options'; sleep 2; shot power-dialog
dump; cp ui.xml power.xml
check "power dialog opens (Power off / Restart shown)" "grep -qi 'power off\|restart\|Emergency' power.xml" "$(grep -o 'text=\"[^\"]\+\"' power.xml | head -8 | tr '\n' ' ')"
adb shell input keyevent KEYCODE_BACK; sleep 1
check "app crashed: none" "! adb logcat -d -b crash | grep -q $PKG" "$(adb logcat -d -b crash | grep -m3 -A3 FATAL | tr '\n' ' ')"
check "updater permissions declared (INTERNET + install)" "adb shell dumpsys package $PKG | grep -q 'android.permission.INTERNET' && adb shell dumpsys package $PKG | grep -q 'android.permission.REQUEST_INSTALL_PACKAGES'" "updater permission missing"
summary
