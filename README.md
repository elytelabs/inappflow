# InAppFlow

[![Release](https://jitpack.io/v/elytelabs/inappflow.svg)](https://jitpack.io/#elytelabs/inappflow)
[![API](https://img.shields.io/badge/API-25%2B-brightgreen.svg)](https://android-arsenal.com/api?level=25)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

A lightweight Kotlin Android library that encapsulates Google Play **In-App Updates** (Immediate & Flexible) and **In-App Review dialogs** with clean, dependency-free APIs and smart usage tracking.

---

## Features

| Feature | Description |
|---------|-------------|
| **Immediate In-App Updates** | Full-screen blocking update flow for critical releases |
| **Flexible In-App Updates** | Background downloading with completion callbacks and restart handling |
| **Update Availability Check** | Non-intrusive update checks returning `AppUpdateResult` (version code, priority) |
| **Complete Encapsulation** | No Google Play Core dependencies leaked to consuming apps |
| **Lifecycle-Safe Registration** | Auto-registers or accepts pre-registered `ActivityResultLauncher` |
| **Smart In-App Review** | Prompts based on install duration, launch count, and remind intervals |
| **Debug Review Trigger** | `forceShowRateDialog(activity)` for instant testing |
| **Memory Safe** | Uses `ApplicationContext` and `WeakReference` to eliminate context leaks |

---

## Installation

### Step 1: Add JitPack repository

In your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### Step 2: Add the dependency

In your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.elytelabs:inappflow:1.2.1")
}
```

---

## Usage Guide

### 1. In-App Updates (`InAppUpdateManager`)

#### A. Immediate Update (Critical / Blocking)

```kotlin
class MainActivity : AppCompatActivity() {

    private val inAppUpdateManager by lazy { InAppUpdateManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Checks for updates and starts immediate update flow if available
        inAppUpdateManager.setupInAppUpdate(UpdateType.IMMEDIATE)
    }

    override fun onResume() {
        super.onResume()
        // Resumes the immediate update if interrupted
        inAppUpdateManager.resumeUpdateIfNeeded()
    }

    override fun onDestroy() {
        inAppUpdateManager.onDestroy()
        super.onDestroy()
    }
}
```

#### B. Flexible Update (Background Download)

```kotlin
class MainActivity : AppCompatActivity() {

    private val inAppUpdateManager by lazy { InAppUpdateManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inAppUpdateManager.setupInAppUpdate(
            updateType = UpdateType.FLEXIBLE,
            onDownloaded = {
                // Background download completed! Prompt user to complete & restart
                showSnackbarWithAction("Update downloaded!", "Restart") {
                    inAppUpdateManager.completeUpdate()
                }
            }
        )
    }

    override fun onDestroy() {
        inAppUpdateManager.onDestroy()
        super.onDestroy()
    }
}
```

#### C. Checking Update Availability (Non-Intrusive)

```kotlin
// Check update status without opening any dialogs (ideal for settings / badges)
inAppUpdateManager.checkUpdateAvailability { result ->
    if (result.isAvailable) {
        val versionCode = result.availableVersionCode
        val priority = result.updatePriority // 0 to 5
        // Prompt user or display an "Update Available" badge
    }
}
```

---

### 2. In-App Reviews (`InAppReviewManager`)

#### A. Quick Start (Default Criteria)

```kotlin
// Default: 2 days since install, 3 launches, 2 days between reminders
showDefaultRatingDialog(this)
```

#### B. Custom Threshold Configuration

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configure thresholds & track launch
        InAppReviewManager.with(this)
            .setInstallDays(3)      // Minimum days since install
            .setLaunchTimes(5)      // Minimum app launches
            .setRemindInterval(7)   // Days to wait before showing again if dismissed
            .monitor()              // Tracks current launch

        // Trigger check (shows dialog only if all conditions are satisfied)
        InAppReviewManager.showRateDialogIfNeeded(this)
    }
}
```

#### C. Debug / Testing Mode

```kotlin
// Force-launch the Play Store review modal immediately (bypasses threshold conditions)
InAppReviewManager.forceShowRateDialog(this)

// Reset all tracking metrics (useful for testing)
InAppReviewManager.reset(this)
```

---

## Requirements

- **Min SDK**: 25 (Android 7.1)
- **Compile/Target SDK**: 37
- **Language**: Kotlin 2.x
- **Build System**: Android Gradle Plugin (AGP) 9.x

---

## License

```
Copyright 2026 Elyte Labs

Licensed under the Apache License, Version 2.0
```
