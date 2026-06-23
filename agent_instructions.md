# 1. Agent Instructions

## Objective
Build, maintain, and enhance the "Power Menu" Android application while preserving existing accessibility actions, theme consistency, and lightweight native execution.

## General Rules
- Keep the application extremely lightweight and secure. Since it utilizes the Accessibility Service API, do not introduce any network requests, third-party analytics, or external trackers to ensure maximum user trust.
- Ensure Jetpack Compose UI changes maintain the space-inspired modern cosmic slate aesthetic with consistent Material 3 styling.
- Verify that any modified accessibility actions remain bound to native, high-performance APIs.
- Keep local builds fast and free of unnecessary dependencies.

## Development Workflow
1. Verify system settings integration via `PowerMenuAccessibilityService` helpers.
2. Maintain high-density, accessible layout structures with minimum 48dp touch targets.
3. Verify compilation clean of warnings and errors using `compile_applet` / Gradle build tasks.
4. Record and update architecture, tracking, and logs in the respective project guide files.
