# InAppFlow

[![Release](https://jitpack.io/v/elytelabs/inappflow.svg)](https://jitpack.io/#elytelabs/inappflow)

InAppFlow is an Android library that provides a convenient way to handle in-app updates and display in-app review dialogs.

## Installation

### Step 1. Add the JitPack repository to your root build.gradle

    
    dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
        repositories {
            mavenCentral()
            maven { url 'https://jitpack.io' }
        }
    }

### Step 2. Add the dependency


`dependencies {
    implementation 'com.github.elytelabs:inappflow:Tag'
}` 

Replace `Tag` with the latest version from the Releases page.

## Usage

### InAppUpdateManager



    class MainActivity : AppCompatActivity() {
    
        private val inAppUpdateManager by lazy { InAppUpdateManager(this) }
    
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)
    
            inAppUpdateManager.setupInAppUpdate()
        }
    
        override fun onDestroy() {
            super.onDestroy()
            inAppUpdateManager.unregisterUpdateListener()
        }
    }

 

### InAppReviewManager

    class MainActivity : AppCompatActivity() {
    
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)
    
            // Show Rating Dialog With Default Params
            showDefaultRatingDialog(this)
    
            // or customize the dialog
             InAppReviewManager.with(this)
                 .setInstallDays(2)
                 .setLaunchTimes(3)
                .setRemindInterval(2)
                 .monitor()
            //  Show a dialog if meets conditions
            InAppReviewManager.showRateDialogIfNeeded(this)
        }
    }
