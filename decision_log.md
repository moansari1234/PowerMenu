# 5. Decision Log

## Architectural Decisions

### Decision: Using Accessibility Services instead of Root/Shell
- **Date**: 2026-06-23
- **Reason**: Triggering the system power options menu (`GLOBAL_ACTION_POWER_MENU`) programmatically without root permissions is strictly blocked by Android security layers. The only safe, official, and non-destructive API to accomplish this is via the Accessibility API.
- **Impact**: Requires the user to grant one-time Accessibility permission in system settings. Highly secure and completely safe for non-rooted, standard consumer devices.

### Decision: Native Vector Graphics over External PNG Images
- **Date**: 2026-06-23
- **Reason**: To maintain an extremely small application footprint and guarantee crisp scaling on any screen density (from compact mobile screens to tablets), all visuals (including the glowing power icon and app adaptive launcher icon) are custom-drawn via Compose canvas and Android vector XMLs instead of external raster files.
- **Impact**: Keeps the compiled APK size under 3MB while maintaining flawless render quality.
