package com.elytelabs.inappflow

import android.app.Activity.RESULT_OK
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallException
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallErrorCode
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Manages in-app updates using Google Play Core Library.
 *
 * This class handles immediate and flexible app updates, properly manages
 * listener lifecycle, and safely handles activity result launchers.
 */
class InAppUpdateManager {

    private val activity: AppCompatActivity
    private val appUpdateManager: AppUpdateManager
    private var launcher: ActivityResultLauncher<IntentSenderRequest>? = null
    private var onUpdateDownloadedListener: (() -> Unit)? = null

    companion object {
        private const val TAG = "InAppUpdate"
    }

    /**
     * Standard constructor. Automatically registers a lifecycle-safe ActivityResultLauncher.
     * MUST be called during Activity initialization or onCreate.
     */
    constructor(activity: AppCompatActivity) {
        this.activity = activity
        this.appUpdateManager = AppUpdateManagerFactory.create(activity)
        this.launcher = try {
            activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
                if (result.resultCode != RESULT_OK) {
                    Log.d(TAG, "Update flow failed! Result code: ${result.resultCode}")
                } else {
                    Log.d(TAG, "Update flow completed successfully.")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not register ActivityResultLauncher automatically: ${e.message}")
            null
        }
    }

    /**
     * Constructor that accepts a pre-registered ActivityResultLauncher.
     * Use this if you initialize InAppUpdateManager lazily or after onCreate().
     */
    constructor(
        activity: AppCompatActivity,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        this.activity = activity
        this.appUpdateManager = AppUpdateManagerFactory.create(activity)
        this.launcher = launcher
    }

    /**
     * Listener for tracking update installation state changes.
     */
    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytesToDownload = state.totalBytesToDownload()
                Log.d(TAG, "Downloading: $bytesDownloaded / $totalBytesToDownload")
            }
            InstallStatus.DOWNLOADED -> {
                Log.d(TAG, "Download complete. Ready to install.")
                onUpdateDownloadedListener?.invoke()
            }
            InstallStatus.INSTALLED -> {
                Log.d(TAG, "Update installed successfully.")
            }
            InstallStatus.FAILED -> {
                Log.e(TAG, "Update failed with error code: ${state.installErrorCode()}")
            }
            InstallStatus.CANCELED -> {
                Log.d(TAG, "Update canceled by user.")
            }
            else -> {
                Log.d(TAG, "Update status: ${state.installStatus()}")
            }
        }
    }

    /**
     * Checks if an update is available without immediately launching the update flow.
     *
     * @param onResult Callback returning boolean isAvailable and optional [AppUpdateInfo].
     */
    fun checkUpdateAvailability(onResult: (isAvailable: Boolean, updateInfo: AppUpdateInfo?) -> Unit) {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            val isAvailable = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            onResult(isAvailable, appUpdateInfo)
        }.addOnFailureListener { exception ->
            Log.w(TAG, "Check update availability failed: ${exception.message}")
            onResult(false, null)
        }
    }

    /**
     * Checks for available updates and initiates an update flow (IMMEDIATE or FLEXIBLE).
     */
    fun setupInAppUpdate(
        updateType: Int = AppUpdateType.IMMEDIATE,
        onDownloaded: (() -> Unit)? = null
    ) {
        this.onUpdateDownloadedListener = onDownloaded
        try {
            appUpdateManager.registerListener(installStateUpdatedListener)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register install listener: ${e.message}")
        }

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(updateType)
            ) {
                Log.d(TAG, "Update available. Initiating update flow (type: $updateType).")
                startUpdate(appUpdateInfo, updateType)
            } else {
                Log.d(TAG, "No update available or update type not allowed.")
            }
        }.addOnFailureListener { exception ->
            if (exception is InstallException) {
                when (exception.errorCode) {
                    InstallErrorCode.ERROR_INSTALL_NOT_ALLOWED -> {
                        Log.e(TAG, "Update not allowed: Check Play Store login and app testing status.")
                    }
                    InstallErrorCode.ERROR_PLAY_STORE_NOT_FOUND -> {
                        Log.e(TAG, "Play Store not found on this device.")
                    }
                    else -> {
                        Log.e(TAG, "Install error code: ${exception.errorCode}")
                    }
                }
            }
            Log.e(TAG, "Failed to check for updates: ${exception.message}", exception)
        }
    }

    /**
     * Starts the in-app update flow with the specified [appUpdateInfo] and [updateType].
     */
    fun startUpdate(appUpdateInfo: AppUpdateInfo, updateType: Int = AppUpdateType.IMMEDIATE) {
        val currentLauncher = launcher
        if (currentLauncher != null) {
            try {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    currentLauncher,
                    AppUpdateOptions.newBuilder(updateType).build()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start update flow with launcher: ${e.message}", e)
            }
        } else {
            Log.w(TAG, "No ActivityResultLauncher available to launch in-app update.")
        }
    }

    /**
     * Checks if an update is already in progress and resumes it if necessary.
     */
    fun resumeUpdateIfNeeded(updateType: Int = AppUpdateType.IMMEDIATE) {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                Log.d(TAG, "Resuming in-progress update.")
                startUpdate(appUpdateInfo, updateType)
            } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                onUpdateDownloadedListener?.invoke()
            }
        }
    }

    /**
     * Completes a flexible update by restarting the app.
     */
    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    /**
     * Cleans up resources and unregisters listeners.
     */
    fun onDestroy() {
        try {
            appUpdateManager.unregisterListener(installStateUpdatedListener)
            Log.d(TAG, "Listener unregistered successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering listener: ${e.message}")
        }
    }
}