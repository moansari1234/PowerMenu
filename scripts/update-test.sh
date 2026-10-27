#!/bin/bash
# Emulator test for the in-app updater: an old debug build must find the latest GitHub release,
# download its debug APK and update itself through the system installer.
# usage: update-test.sh <old version the APK was built with>
source scripts/emulator-lib.sh
OLD=$1
PKG=$(grep -o 'applicationId = "[^"]*"' app/build.gradle.kts | cut -d'"' -f2).debug
LATEST=$(curl -s https://api.github.com/repos/$GITHUB_REPOSITORY/releases/latest | python3 -c 'import json,sys;print(json.load(sys.stdin)["tag_name"].lstrip("v"))')
ver(){ adb shell dumpsys package $PKG | grep -m1 versionName | cut -d= -f2 | tr -d '\r'; }
adb install -r app/build/outputs/apk/debug/app-debug.apk
check "old build $OLD installed" "[ \"\$(ver)\" = '$OLD' ]" "got $(ver)"
adb shell appops set $PKG REQUEST_INSTALL_PACKAGES allow
adb shell am start -W -n $PKG/com.example.MainActivity; sleep 6; shot launch-prompt
check "auto-check on launch offers v$LATEST" "screen_has 'Update available'" "saw: \$(texts)"
tap package=$PKG text=Update; sleep 2; shot downloading
# Wait for the system installer's confirmation, then approve it
for i in $(seq 1 45); do dump; grep -q 'packageinstaller' ui.xml && break; sleep 2; done
shot installer-confirm
check "system installer asks to confirm the update" "grep -q packageinstaller ui.xml" "saw: \$(texts)"
tap package='.*packageinstaller.*' text='(?i)update|install'; 
for i in $(seq 1 30); do [ "$(ver)" = "$LATEST" ] && break; sleep 2; done
check "app updated itself $OLD -> $LATEST" "[ \"\$(ver)\" = '$LATEST' ]" "version now $(ver)"
adb shell am start -W -n $PKG/com.example.MainActivity; sleep 3
tap content-desc=Settings; sleep 2
tap text='Check for updates'; sleep 6; shot settings-latest
check "Settings reports latest version after update" "screen_has 'latest version'" "saw: \$(texts)"
check "app crashed: none" "! adb logcat -d -b crash | grep -q $PKG" "$(adb logcat -d -b crash | grep -m3 -A3 FATAL | tr '\n' ' ')"
summary
