# Feature List + Core Topics They Illustrate
## Designed to map directly onto the required topics

1. Daily Water Intake Tracking (Core Feature)
   Users can add water they drink throughout the day. Each entry is added using a Composable function with simple Material UI components.

2. Scrollable Daily Log (LazyColumn + Cards)
   A section shows all water entries in a scrollable list using LazyColumn with Material 3 Cards, displaying amount, time, and an icon.

3. Animated Daily Progress Indicator (Animation)
   A linear progress bar that smoothly animates as the user adds more water toward their daily goal.

4. Multi-Screen Navigation
   App contains multiple screens:

- Home Screen (today’s progress + add buttons)
- Hydration Tips
- Settings

Navigation handled using Jetpack Navigation Compose.

5. Water Goal Customisation (DataStore)
   Users can change their daily water goal (e.g., 2000ml).
   Value is stored persistently using DataStore Preferences.

6. Local Database Storage (Room DB)
   All water logs (amount + timestamp) are saved in a local Room database.
   Users can tap on any record to open a dialog to **edit** or **delete** it.
   Allows viewing data even when offline.

7. Sync or Fetch Hydration Tips from API (Retrofit)
   A "Hydration Tips" screen fetches a list of simple tips from a mock/real REST API using Retrofit, then displays them in a list.

8. Load Images with Coil
   Illustrations for hydration tips are loaded from URLs using Coil.

9. Adaptive UI for Different Screen Sizes

10. Full App Logging (Logcat)
    Important actions (navigation, add/modify/delete intake, set goal, fetch API data) generate log messages for debugging.

11. Clean Architecture (MVVM)
    Uses ViewModel and Repository to cleanly separate UI from data logic.

12. Select Intake by Date
    Users can view their water intake by the date they select
