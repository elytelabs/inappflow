package com.elytelabs.inappflow

import android.app.Activity.RESULT_OK
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.InstallException
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.install.model.InstallErrorCode

/**
 * Manages in-app updates using Google Play Core Library.
 *
 * This class handles immediate app updates and properly manages
 * listener lifecycle to prevent memory leaks.
 *
 * Usage:
 * ```
 * private lateinit var updateManager: InAppUpdateManager
 *
 * override fun onCreate(savedInstanceState: Bundle?) {
 * super.onCreate(savedInstanceState)
 * updateManager = InAppUpdateManager(this)
 * updateManager.setupInAppUpdate()
 * }
 *
 * override fun onDestroy() {
 * updateManager.onDestroy()
 * super.onDestroy()
 * }
 * ```
 */
class InAppUpdateManager(private val activity: AppCompatActivity) {

    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(activity) }

    /**
     * Listener for tracking update installation state changes.
     * Stored as a property to ensure proper unregistration in onDestroy().
     */
    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytesToDownload = state.totalBytesToDownload()
                Log.d("App Update", "Downloading: $bytesDownloaded / $totalBytesToDownload")
            }
            InstallStatus.DOWNLOADED -> {
                Log.d("App Update", "Download complete. Ready to install.")
            }
            InstallStatus.INSTALLED -> {
                Log.d("App Update", "Update installed successfully.")
            }
            InstallStatus.FAILED -> {
                Log.e("App Update", "Update failed with error code: ${state.installErrorCode()}")
            }
            InstallStatus.CANCELED -> {
                Log.d("App Update", "Update canceled by user.")
            }
            else -> {
                Log.d("App Update", "Update status: ${state.installStatus()}")
            }
        }
    }

    /**
     * Activity result launcher for handling update flow results.
     */
    private val activityResultLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
            if (result.resultCode != RESULT_OK) {
                Log.d("App Update", "Update flow failed! Result code: ${result.resultCode}")
            } else {
                Log.d("App Update", "Update flow completed successfully.")
            }
        }

    /**
     * Checks for available updates and initiates an IMMEDIATE update flow if available.
     *
     * This method should be called in onCreate() or onResume() of your Activity.
     * Registers the install state listener to track update progress.
     */
    fun setupInAppUpdate() {
        // Register the listener before checking for updates
        appUpdateManager.registerListener(installStateUpdatedListener)

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                Log.d("App Update", "Update available. Initiating immediate update flow.")
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    activityResultLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                )
            } else {
                Log.d("App Update", "No update available or update type not allowed.")
            }
        }.addOnFailureListener { exception ->
            if (exception is InstallException) {
                when (exception.errorCode) {
                    InstallErrorCode.ERROR_INSTALL_NOT_ALLOWED -> {
                        Log.e("App Update", "Update not allowed: Check Play Store login and app testing status.")
                    }
                    InstallErrorCode.ERROR_PLAY_STORE_NOT_FOUND -> {
                        Log.e("App Update", "Play Store not found on this device.")
                    }
                    else -> {
                        Log.e("App Update", "Install error: ${exception.errorCode}")
                    }
                }
            }
            Log.e("App Update", "Failed to check for updates: ${exception.message}", exception)
        }
    }

    /**
     * Checks if an update is already in progress and resumes it if necessary.
     */
    fun resumeUpdateIfNeeded() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                Log.d("App Update", "Resuming in-progress update.")
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    activityResultLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                )
            }
        }
    }

    /**
     * Cleans up resources and unregisters listeners.
     */
    fun onDestroy() {
        appUpdateManager.unregisterListener(installStateUpdatedListener)
        Log.d("App Update", "Listener unregistered successfully.")
    }
}