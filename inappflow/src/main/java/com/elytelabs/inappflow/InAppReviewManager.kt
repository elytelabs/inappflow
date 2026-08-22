package com.elytelabs.inappflow

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import java.lang.ref.WeakReference
import java.util.Date

/**
 * Manages in-app review prompts using Google Play Core Library.
 *
 * This singleton class handles timing conditions for showing the review dialog
 * based on install date, launch count, and remind intervals.
 *
 * **Memory Leak Prevention:**
 * Uses ApplicationContext internally and WeakReference for delayed Activity presentation.
 *
 * Usage:
 * ```kotlin
 * // In Application class or MainActivity onCreate
 * InAppReviewManager.with(applicationContext)
 *     .setInstallDays(3)
 *     .setLaunchTimes(5)
 *     .setRemindInterval(7)
 *     .monitor()
 *
 * // In appropriate Activity lifecycle (e.g., onResume)
 * InAppReviewManager.showRateDialogIfNeeded(this)
 * ```
 */
class InAppReviewManager private constructor(context: Context) {

    /**
     * Minimum number of days since app installation before showing review prompt.
     * Default: 2 days
     */
    private var installDaysThreshold = DEFAULT_INSTALL_DAYS

    /**
     * Minimum number of app launches before showing review prompt.
     * Default: 3 launches
     */
    private var launchTimesThreshold = DEFAULT_LAUNCH_TIMES

    /**
     * Number of days to wait before showing review prompt again after dismissal.
     * Default: 2 days
     */
    private var remindIntervalDays = DEFAULT_REMIND_INTERVAL

    /**
     * Application context to prevent memory leaks.
     */
    private val appContext: Context = context.applicationContext

    companion object {
        private const val TAG = "InAppReviewManager"

        // Default configuration values
        private const val DEFAULT_INSTALL_DAYS = 2
        private const val DEFAULT_LAUNCH_TIMES = 3
        private const val DEFAULT_REMIND_INTERVAL = 2

        /**
         * Delay before showing review dialog (in milliseconds).
         * Gives user time to settle into the Activity.
         */
        private const val REVIEW_DIALOG_DELAY_MS = 5000L

        /**
         * Singleton instance holder.
         */
        @Volatile
        private var instance: InAppReviewManager? = null

        /**
         * Initializes or retrieves the singleton instance.
         *
         * @param context Any context (converted to ApplicationContext internally)
         * @return Singleton instance of InAppReviewManager
         */
        fun with(context: Context): InAppReviewManager {
            return instance ?: synchronized(this) {
                instance ?: InAppReviewManager(context).also { instance = it }
            }
        }

        /**
         * Checks if conditions are met and shows the review dialog if needed.
         *
         * @param activity The Activity to show the review dialog in (must not be finishing)
         */
        fun showRateDialogIfNeeded(activity: Activity) {
            val manager = instance ?: with(activity)

            if (activity.isFinishing || activity.isDestroyed) {
                Log.d(TAG, "Activity is finishing/destroyed. Skipping review dialog.")
                return
            }

            val shouldShow = manager.shouldShowRateDialog()
            Log.d(TAG, "Should show review dialog: $shouldShow")

            if (shouldShow) {
                manager.showRateDialog(activity)
            }
        }

        /**
         * Instantly requests and shows the in-app review flow bypassing
         * launch count and time threshold checks.
         *
         * Ideal for developer testing or manual "Rate Us" button clicks.
         */
        fun forceShowRateDialog(activity: Activity) {
            val manager = with(activity)
            manager.showDialogInternal(activity)
        }

        /**
         * Resets tracking preferences (install date, launch count, remind interval).
         */
        fun reset(context: Context) {
            ReviewPrefs.reset(context.applicationContext)
            Log.d(TAG, "InAppReview preferences reset.")
        }

        /**
         * Checks if the current date is past the threshold from a target date.
         */
        private fun isOverDate(targetDate: Long, thresholdDays: Int): Boolean {
            val currentTime = Date().time
            val thresholdMillis = thresholdDays * 24L * 60L * 60L * 1000L
            return (currentTime - targetDate) >= thresholdMillis
        }
    }

    /**
     * Sets the minimum number of app launches required before showing review.
     */
    fun setLaunchTimes(launchTimes: Int): InAppReviewManager {
        require(launchTimes > 0) { "Launch times must be positive" }
        this.launchTimesThreshold = launchTimes
        Log.d(TAG, "Launch times threshold set to: $launchTimes")
        return this
    }

    /**
     * Sets the minimum number of days since installation before showing review.
     */
    fun setInstallDays(installDays: Int): InAppReviewManager {
        require(installDays >= 0) { "Install days must be non-negative" }
        this.installDaysThreshold = installDays
        Log.d(TAG, "Install days threshold set to: $installDays")
        return this
    }

    /**
     * Sets the interval in days before showing the review prompt again.
     */
    fun setRemindInterval(remindInterval: Int): InAppReviewManager {
        require(remindInterval > 0) { "Remind interval must be positive" }
        this.remindIntervalDays = remindInterval
        Log.d(TAG, "Remind interval set to: $remindInterval days")
        return this
    }

    /**
     * Sets whether the user has opted out of review prompts.
     */
    fun setOptOut(optOut: Boolean): InAppReviewManager {
        ReviewPrefs.setOptOut(appContext, optOut)
        Log.d(TAG, "Opt-out set to: $optOut")
        return this
    }

    /**
     * Monitors app launches and updates stored preferences.
     */
    fun monitor() {
        if (ReviewPrefs.isFirstLaunch(appContext)) {
            ReviewPrefs.setInstallDate(appContext)
            Log.d(TAG, "First launch detected. Install date saved.")
        }

        val currentCount = ReviewPrefs.getLaunchCount(appContext)
        val newCount = currentCount + 1
        ReviewPrefs.setLaunchCount(appContext, newCount)
        Log.d(TAG, "Launch count incremented to: $newCount")
    }

    /**
     * Shows the in-app review dialog with a delay.
     */
    fun showRateDialog(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) {
            Log.w(TAG, "Cannot show review dialog. Activity is finishing or destroyed.")
            return
        }

        val activityRef = WeakReference(activity)
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            val currentActivity = activityRef.get()
            if (currentActivity != null && !currentActivity.isFinishing && !currentActivity.isDestroyed) {
                showDialogInternal(currentActivity)
            } else {
                Log.d(TAG, "Activity finished or reclaimed during delay. Review dialog cancelled.")
            }
        }, REVIEW_DIALOG_DELAY_MS)

        Log.d(TAG, "Review dialog scheduled to show in ${REVIEW_DIALOG_DELAY_MS}ms")
    }

    /**
     * Checks if all conditions are met to show the review dialog.
     */
    fun shouldShowRateDialog(): Boolean {
        if (ReviewPrefs.isOptedOut(appContext)) {
            Log.d(TAG, "User has opted out of review prompts.")
            return false
        }

        val overLaunchTimes = isOverLaunchTimes()
        val overInstallDate = isOverInstallDate()
        val overRemindDate = isOverRemindDate()

        Log.d(TAG, "Review conditions - Launches: $overLaunchTimes, Install: $overInstallDate, Remind: $overRemindDate")

        return overLaunchTimes && overInstallDate && overRemindDate
    }

    private fun isOverLaunchTimes(): Boolean {
        val count = ReviewPrefs.getLaunchCount(appContext)
        return count >= launchTimesThreshold
    }

    private fun isOverInstallDate(): Boolean {
        val installDate = ReviewPrefs.getInstallDate(appContext)
        if (installDate == 0L) return false
        return isOverDate(installDate, installDaysThreshold)
    }

    private fun isOverRemindDate(): Boolean {
        val remindDate = ReviewPrefs.getRemindInterval(appContext)
        if (remindDate == 0L) return true
        return isOverDate(remindDate, remindIntervalDays)
    }

    /**
     * Internal method to request and show the review dialog.
     */
    private fun showDialogInternal(activity: Activity) {
        val reviewManager = ReviewManagerFactory.create(appContext)
        val requestReviewFlow = reviewManager.requestReviewFlow()

        Log.d(TAG, "Requesting review flow...")

        requestReviewFlow.addOnCompleteListener { request ->
            if (request.isSuccessful) {
                val reviewInfo = request.result
                Log.d(TAG, "Review flow request successful. Launching review dialog.")
                startReview(activity, reviewManager, reviewInfo)
            } else {
                Log.e(TAG, "Review flow request failed", request.exception)
                ReviewPrefs.setRemindIntervalDate(appContext)
            }
        }
    }

    private fun startReview(
        activity: Activity,
        reviewManager: ReviewManager,
        reviewInfo: ReviewInfo
    ) {
        val flow = reviewManager.launchReviewFlow(activity, reviewInfo)

        flow.addOnCompleteListener { task ->
            ReviewPrefs.setRemindIntervalDate(appContext)

            if (task.isSuccessful) {
                Log.d(TAG, "Review flow completed successfully.")
            } else {
                Log.e(TAG, "Review flow failed", task.exception)
            }
        }
    }
}