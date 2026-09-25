# EdgeGate App UI Design (Claude Design reference)

This folder holds the source of the **EdgeGate App UI Design** canvas made in Claude Design. The Compose UI in `app/src/main/java/com/chinmay/edgegate/ui` was built from these mockups.

| Artboard | File | Implemented in |
| --- | --- | --- |
| Design system (colours, type, plate chip, badges) | `project/DesignSystem.dc.html` | `ui/theme/*`, `ui/components/Components.kt` |
| Dashboard | `project/Main.dc.html` | `ui/dashboard/DashboardScreen.kt` |
| Scan · allow | `project/Scan.dc.html` | `ui/scan/ScanScreen.kt` |
| Scan · blacklist alert | `project/ScanDeny.dc.html` | `ui/scan/ScanScreen.kt` (deny sheet) |
| Gate log | `project/Log.dc.html` | `ui/log/LogScreen.kt` |
| Vehicles | `project/Vehicles.dc.html` | `ui/vehicles/VehiclesScreen.kt` |
| Register vehicle | `project/AddVehicle.dc.html` | `ui/vehicles/AddVehicleScreen.kt` |
| Settings | `project/Settings.dc.html` | `ui/settings/SettingsScreen.kt` |
| Bottom nav component | `project/NavBar.dc.html` | `MainActivity.kt` |

`project/canvas.json` records where each artboard sits on the canvas and its size (390 px wide phone frames).

The `.dc.html` files are Claude Design components. They use the Claude Design runtime (`support.js`), so they won't render correctly if you open them straight from GitHub. Use them to look up exact colours, spacing, copy and layout. The names, plates and figures in the mockups are sample data.

Screenshots of the built app are in [`docs/img`](../../docs/img/).
