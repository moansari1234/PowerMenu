# 3. Project Standards

## Folder Structure
- `/app/src/main/java/com/example/`: Main Kotlin codebase containing screens, UI theme styles, and services.
  - `MainActivity.kt`: Contains UI layouts, Compose states, and launch logic.
  - `PowerMenuAccessibilityService.kt`: Accessibility service interface and registration.
- `/app/src/main/res/`: App asset resources.
  - `drawable/`: Foreground/background custom vector XML assets.
  - `xml/accessibility_service_config.xml`: System accessibility capabilities and description bindings.
- `/.github/workflows/build-apk.yml`: Automated CI/CD build actions.

## Coding Standards
- **Naming Conventions**: Kotlin classes use `PascalCase`, functions and variables use `camelCase`, layout resource files use `snake_case`. Compose `testTag` IDs use `snake_case` (e.g. `power_trigger_button`).
- **Error Handling Patterns**: Surround setting intents in `try-catch` blocks to prevent crashes on non-standard Android distributions or customized vendor overlays.
- **State Management**: Use Compose `@Composable` states linked with Activity lifecycle overrides (`onResume`) to automatically fetch service connectivity values and refresh the user screen seamlessly.

## Security Requirements
- **Strict Privacy**: The Accessibility Service must not collect, parse, or log any user screen interactions. `canRetrieveWindowContent` is explicitly set to `false` in `accessibility_service_config.xml`.
- **Minimal Permissions**: The app requires no internet access (`android.permission.INTERNET` is absent), ensuring complete offline safety and sandboxed operation.
