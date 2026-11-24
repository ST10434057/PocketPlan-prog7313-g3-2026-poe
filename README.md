# PocketPlan - Personal Finance Manager

PocketPlan is a comprehensive Android application designed to help users track their expenses, manage budgets, and achieve savings goals. With a clean Material Design interface and cloud-backed storage powered by Firebase, it provides an intuitive and secure way to take control of your personal finances from any device.

## Video Link: https://drive.google.com/drive/folders/14iGr3z94_g4FznctWz8fcVh2I9_Z_xjX?usp=sharing 

## App Purpose

Many people — particularly students — struggle to keep track of where their money goes each month. Spreadsheets are tedious, generic banking apps lack budgeting tools, and most dedicated finance apps are paid or overly complex.

PocketPlan solves this by giving users a simple, cloud-backed mobile tool to:

- **Log every expense** as it happens, with a category, date, time, and optional receipt photo.
- **Set monthly spending goals** (a minimum target and a maximum cap) plus per-category limits so overspending is caught early.
- **See the big picture** at a glance — the Dashboard shows remaining budget, a goal status message, and the five most recent transactions without any extra navigation.
- **Review and filter history** — the Transaction History screen lets users search, filter by category, and narrow by date range to answer "where did my money go last week?".
- **Stay motivated** — the Rewards system unlocks achievements for consistent logging and staying within budget, turning good habits into progress milestones.
- **Access data anywhere** — because everything is stored in Firebase under the user's account, data survives uninstalls, device changes, and app updates.

## Features

- **User Authentication**:
  - Register and log in with email and password via Firebase Authentication.
  - Sign in with Google using the Android Credential Manager API.
  - Each user gets a unique UID that keeps their data completely private.
  - Auto-login: users already signed in are taken straight to the Dashboard on app launch.
- **Dashboard Overview**:
  - Real-time visualisation of monthly budget vs. actual spending.
  - Progress bar showing how much of the total budget has been used.
  - Goal status message indicating whether you are below, within, or over your spending goals.
  - Overspending warnings when category limits are exceeded.
  - Recent expenses list showing the last 5 transactions (title, amount, category, date).
  - Automatically refreshes when returning from the Add Expense screen.
- **Expense Tracking**:
  - Add detailed expenses with titles, amounts, categories, dates, and times.
  - Attach receipt photos — images are uploaded to Firebase Storage and linked to the expense.
  - All data is saved online and accessible after logging back in.
- **Category Management**:
  - Create custom spending categories saved to Firebase.
  - Three default categories are created automatically on registration: Food, Transport, Book/Study.
- **Budget & Goal Management**:
  - Set a monthly minimum goal, maximum goal, and total budget.
  - Define per-category spending limits.
  - Goals are stored per month in Firebase.
- **Transaction History**:
  - Browse all expenses loaded from Firebase.
  - Filter by category (Food, Transport, Book/Study).
  - Filter by date range using a start and end date picker.
  - Search by expense title or description.
  - The summary card shows total count, total spent, and the current month.
- **Analytics & Graphs**:
  - Bar charts of spending by category for a selected period.
  - Daily spending line graph.
  - Min/max goal lines overlaid on graphs.
- **Rewards**:
  - Unlock achievements based on spending behaviour (e.g., staying under budget, logging expenses consistently).
  - Reward progress and unlock status saved to Firebase.
- **User Profile**:
  - View account email address loaded from Firebase.
  - Quick reward summary showing how many badges have been unlocked.
  - Toggle Night Mode.
  - Sign out via Firebase Authentication (clears local session and returns to login).
- **Offline Support**:
  - Firestore reads attempt the server first and fall back to the local cache automatically.
  - Clear error messages are shown when the device is offline and no cache is available.
- ## Features added to final poe:
  - Rewards System
    
  - The Rewards System was implemented to encourage users to develop positive budgeting habits and consistently track their expenses.
  -  Users earn rewards and achievements when they meet budgeting goals or regularly record expenses.
  -   This gamification feature makes the application more engaging and motivates users to actively manage their finances.
      
  -   Dark Mode
    
  -   Dark Mode was added to improve the overall user experience by allowing users to switch between light and dark themes.
  -    This feature enhances accessibility, reduces eye strain in low-light environments, and provides users with a more personalised experience.
  -    The selected theme is saved and automatically applied when the application is reopened.
  

## Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Material Design Components](https://material.io/develop/android)
- **Authentication**: [Firebase Authentication](https://firebase.google.com/docs/auth) (email/password + Google Sign-In)
- **Google Sign-In**: [Android Credential Manager](https://developer.android.com/identity/sign-in/credential-manager) with `androidx.credentials`
- **Database**: [Cloud Firestore](https://firebase.google.com/docs/firestore) (NoSQL, per-user document collections, with offline cache fallback)
- **File Storage**: [Firebase Storage](https://firebase.google.com/docs/storage) (receipt image uploads)
- **Concurrency**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) via `lifecycleScope.launch`
- **Local DB**: [Room Persistence Library](https://developer.android.com/training/data-storage/room) (offline caching)
- **Navigation**: Bottom Navigation for seamless switching between core features.
- **Min SDK**: 26 (Android 8.0 Oreo) | **Target SDK**: 35

## Firebase Data Structure

All user data is stored under the user's unique Firebase UID, ensuring complete isolation between accounts.

```
Firestore
└── users/
    └── {uid}/
        ├── categories/
        │   └── {categoryId}   → name, createdAt
        ├── expenses/
        │   └── {expenseId}    → title, amount, categoryId, categoryName,
        │                         date (yyyy-MM-dd), time, description,
        │                         receiptImageUrl, createdAt
        ├── goals/
        │   └── {yyyy-MM}      → minimumGoal, maximumGoal, totalBudgetGoal,
        │                         categoryLimits (map)
        └── rewards/
            └── {rewardId}     → name, description, unlocked, progress

Firebase Storage
└── users/
    └── {uid}/
        └── receipts/          → uploaded receipt images
```

## Design Considerations

### Architecture
- **Activity-per-screen**: Each major screen (Dashboard, Add Expense, Transaction History, Graphs, Budget, Profile, Rewards) is its own `AppCompatActivity`. This keeps responsibilities clearly separated and avoids the complexity of a fragment back-stack for a project of this size.
- **Firebase-first with Room as local cache**: All persistent data lives in Cloud Firestore. Room is used as a secondary offline cache so the app can still display data when the device has no connectivity. Reads always attempt the server first and fall back to the local Firestore cache, then Room.
- **Per-user UID isolation**: Every Firestore document path begins with `users/{uid}/...`. This means a single Firestore database serves all users with zero risk of data leaking between accounts.
- **Coroutines over callbacks**: All Firebase operations use `kotlinx.coroutines` with `lifecycleScope.launch` and `.await()`. This eliminates deeply-nested callback chains and keeps async code readable and cancellation-safe.

### UI & UX
- **Material Design 3 components**: `MaterialButton`, `SwitchMaterial`, `TextInputLayout`, and `BottomNavigationView` are used throughout to keep the interface consistent and accessible without writing custom widgets.
- **Bottom Navigation**: The four core sections (Home, Budget, Reports, Profile) are always one tap away. Each nav item launches its Activity with `FLAG_ACTIVITY_REORDER_TO_FRONT` to avoid creating duplicate instances in the back stack.
- **Dashboard auto-refresh**: `onResume()` reloads all Firebase data so the Dashboard is always up to date after adding an expense or changing a budget — no manual refresh button needed.
- **Programmatic recent-expenses list**: The five most recent transactions are rendered dynamically at runtime (title, amount, category, date with dividers) rather than with a fixed RecyclerView, keeping the implementation lightweight for a short list.
- **Month-keyed goals**: Budget goals are stored under the key `yyyy-MM` in Firestore. This means historical goal data is preserved automatically — users can always see what their budget was for any previous month.

### Security & Data Privacy
- Firebase Authentication ensures only authenticated users can read or write their own data. Firestore Security Rules (configured in the Firebase console) enforce this server-side regardless of client code.
- Receipt images are stored under `users/{uid}/receipts/` in Firebase Storage, keeping each user's files completely private.
- Google Sign-In is handled via the Android Credential Manager API rather than the deprecated Google Sign-In SDK, using short-lived ID tokens that are exchanged server-side by Firebase Auth.

## GitHub Actions

No automated CI/CD pipeline was configured for this project. The `.github/` folder contains only a placeholder `.keep` file.

**Branching and review workflow used instead:**

Each team member worked on a personal branch named after their student number (e.g. `ST10442123(Jival)`, `ST10440539(Julian)`). Changes were integrated into `Main` via **pull requests**, which required at least one review before merging. This gave the team a lightweight code-review gate without needing a full CI setup.

**GitHub Copilot automated reviews:**

The repository has a set of `copilot/` branches (e.g. `copilot/scheduled-hourly-bug-check`) created by GitHub Copilot's automated code-review feature. These scheduled Copilot runs analysed the codebase for potential bugs and posted findings as pull-request comments, serving as an automated quality check in the absence of a unit-test suite.

**Future CI recommendation:**

A GitHub Actions workflow running `./gradlew build` and `./gradlew lint` on every pull request would catch compilation errors and lint warnings before they reach `Main`. This is the natural next step if the project is extended.

## Team Contributions

| Student | Name | Features Implemented |
|---|---|---|
| ST10434057 | Matteo | Firebase foundation — all data model files (`FirebaseUserProfile`, `FirebaseExpense`, `FirebaseCategory`, `FirebaseGoal`, `FirebaseReward`, `FirebaseCategoryTotal`), `FirebaseRepository` skeleton, `SessionManager`, initial Firebase project configuration |
| ST10440539 | Julian | Firebase backend date-range filters, Transaction History activity, Google Sign-In via Credential Manager (initial implementation), dynamic Dashboard category totals and recent-transaction list, bottom-nav profile fix |
| ST10322057 | Josh | Add Expense screen (full form with category spinner, date/time pickers, receipt photo upload), Dashboard data refresh on resume, Graphs/Analytics screen (bar chart + daily line graph + goal lines), Budget Goal screen |
| ST10442123 | Jival | Budget UI enhancements, navigation state persistence across activities, recent expenses UI redesign on Dashboard (title/amount/category/date rows with dividers), Firebase config updates |
| ST10442063 | Nicholas | Google Sign-In auth integration with Firebase (`signInWithGoogle`), offline fallback strategy (`getServerThenCache`), sign-out improvements (session clear + activity stack reset), offline login fix, category consolidation to Food/Transport/Book-Study, app icon |

## Usage

1. **Register / Login**: Create an account with email and password, or tap **Sign in with Google**. Default categories are created automatically on first registration. Log back in at any time to restore all your data from Firebase.
2. **Add Categories**: Navigate to Add Category to create custom spending categories. Categories are saved online and available across sessions.
3. **Set Your Budget**: Go to the Budget tab to define your monthly minimum/maximum goals and per-category limits.
4. **Log Expenses**: Use the Add Expense screen to record spending. Select a category from the Firebase-loaded spinner, pick a date and time, optionally attach a receipt photo.
5. **Monitor**: Check the Dashboard for a live summary — including your last 5 expenses — or the Reports tab to browse and filter your full transaction history by category or date range.
6. **Analyse**: Visit the Graphs screen to see spending broken down by category and day, with goal lines for context.
7. **Achievements**: Open the Rewards screen to see which milestones you have unlocked based on your spending data.
8. **Personalize**: Visit the Profile tab to toggle Night Mode or sign out.

---
*Developed as part of the PROG7313 POE — Group 1.*
