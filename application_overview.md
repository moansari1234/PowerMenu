# 2. Application Overview

## Application Name
Power Menu

## Purpose
The primary purpose of the application is to provide a safe, simple, and elegant software utility that opens the native Android power options menu (shut down, power off, restart, emergency mode) programmatically. This removes the necessity of using physical hardware keys, protecting devices with fragile or broken power buttons and increasing physical accessibility for users.

## Target Users
- Users with physical difficulties or motor limitations who find pressing hard mechanical side buttons challenging.
- Users with damaged, unresponsive, or fragile physical power buttons on their Android devices.
- Android enthusiasts looking for convenient soft-key or drawer shortcuts to trigger system controls.

## Technology Stack
- **OS Platform:** Android (minSdk 24, targetSdk 36)
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose with Material Design 3 (M3)
- **Architecture:** MVVM (Model-View-ViewModel) with lightweight reactive state checking
- **CI/CD Build System:** Gradle (Kotlin DSL) integrated with GitHub Actions for automatic APK compilation

## Architecture Overview
The application uses a highly decoupled, simple native architecture:
1. **Activity Layer (`MainActivity`)**: Coordinates runtime UI rendering, detects service status transitions in the foreground life cycle, and handles explicit intents to system setting panels.
2. **Accessibility Layer (`PowerMenuAccessibilityService`)**: A standard Android `AccessibilityService` that acts as the privileged broker. When enabled, it maintains a static bound reference allowing safe execution of `performGlobalAction(6)` (corresponds to `GLOBAL_ACTION_POWER_MENU`).

## Core Modules
### Power Menu Trigger
Handles programmatic requests to display the system shut down dialog securely, checking connectivity and warning the user if the underlying service is currently inactive.

### Accessibility Setup Manager
Verifies whether the app's custom service is enabled in the secure system settings and provides direct visual checklists and launch buttons to guide the user through the Android setup flow.
