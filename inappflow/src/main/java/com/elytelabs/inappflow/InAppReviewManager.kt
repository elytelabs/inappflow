package com.elytelabs.inappflow

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import java.util.Date

/**
 * Manages in-app review prompts using Google Play Core Library.
 *
 * This singleton class handles timing conditions for showing the review dialog
 * based on install date, launch count, and remind intervals.
 *
 * **Memory Leak Prevention:**
 * Uses ApplicationContext only to prevent Activity/Fragment context leaks.
 *
 * Usage:
 * ```
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
     * Always use applicationContext, never Activity context.
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
         * Holds ApplicationContext only - safe from memory leaks.
         */
        @Volatile
        private var instance: InAppReviewManager? = null

        /**
         * Initializes or retrieves the singleton instance.
         *
         * @param context Any context (will be converted to ApplicationContext internally)
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
         * This is the main entry point for triggering the review flow.
         * Call this in onResume() or after a positive user interaction.
         *
         * @param activity The Activity to show the review dialog in (must not be finishing)
         */
        fun showRateDialogIfNeeded(activity: Activity) {
            val manager = instance
            if (manager == null) {
                Log.w(TAG, "InAppReviewManager not initialized. Call with() first.")
                return
            }

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
         * Checks if the current date is past the threshold from a target date.
         *
         * @param targetDate The starting date in milliseconds
         * @param thresholdDays Number of days that must pass
         * @return true if threshold days have passed
         */
        private fun isOverDate(targetDate: Long, thresholdDays: Int): Boolean {
            val currentTime = Date().time
            val thresholdMillis = thresholdDays * 24L * 60L * 60L * 1000L
            return (currentTime - targetDate) >= thresholdMillis
        }
    }

    /**
     * Sets the minimum number of app launches required before showing review.
     *
     * @param launchTimes Minimum launch count (must be positive)
     * @return This instance for method chaining
     */
    fun setLaunchTimes(launchTimes: Int): InAppReviewManager {
        require(launchTimes > 0) { "Launch times must be positive" }
        this.launchTimesThreshold = launchTimes
        Log.d(TAG, "Launch times threshold set to: $launchTimes")
        return this
    }

    /**
     * Sets the minimum number of days since installation before showing review.
     *
     * @param installDays Minimum days since install (must be non-negative)
     * @return This instance for method chaining
     */
    fun setInstallDays(installDays: Int): InAppReviewManager {
        require(installDays >= 0) { "Install days must be non-negative" }
        this.installDaysThreshold = installDays
        Log.d(TAG, "Install days threshold set to: $installDays")
        return this
    }

    /**
     * Sets the interval in days before showing the review prompt again.
     *
     * @param remindInterval Days to wait before next prompt (must be positive)
     * @return This instance for method chaining
     */
    fun setRemindInterval(remindInterval: Int): InAppReviewManager {
        require(remindInterval > 0) { "Remind interval must be positive" }
        this.remindIntervalDays = remindInterval
        Log.d(TAG, "Remind interval set to: $remindInterval days")
        return this
    }

    /**
     * Monitors app launches and updates stored preferences.
     *
     * Call this in onCreate() of your main Activity or Application class.
     * It tracks:
     * - First launch date (install date)
     * - Launch count
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
     *
     * The delay allows the user to settle into the Activity before
     * being interrupted with a review prompt.
     *
     * @param activity The Activity to show the dialog in
     */
    fun showRateDialog(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) {
            Log.w(TAG, "Cannot show review dialog. Activity is finishing or destroyed.")
            return
        }

        // Use main looper to ensure we're on the UI thread
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            // Double-check activity state after delay
            if (!activity.isFinishing && !activity.isDestroyed) {
                showDialogInternal(activity)
            } else {
                Log.d(TAG, "Activity finished during delay. Review dialog cancelled.")
            }
        }, REVIEW_DIALOG_DELAY_MS)

        Log.d(TAG, "Review dialog scheduled to show in ${REVIEW_DIALOG_DELAY_MS}ms")
    }

    /**
     * Checks if all conditions are met to show the review dialog.
     *
     * Conditions:
     * 1. Minimum launch count reached
     * 2. Minimum days since install passed
     * 3. Minimum days since last remind passed
     *
     * @return true if all conditions are met
     */
    fun shouldShowRateDialog(): Boolean {
        val overLaunchTimes = isOverLaunchTimes()
        val overInstallDate = isOverInstallDate()
        val overRemindDate = isOverRemindDate()

        Log.d(TAG, "Review conditions - Launches: $overLaunchTimes, Install: $overInstallDate, Remind: $overRemindDate")

        return overLaunchTimes && overInstallDate && overRemindDate
    }

    /**
     * Checks if the app has been launched enough times.
     */
    private fun isOverLaunchTimes(): Boolean {
        val count = ReviewPrefs.getLaunchCount(appContext)
        return count >= launchTimesThreshold
    }

    /**
     * Checks if enough days have passed since app installation.
     */
    private fun isOverInstallDate(): Boolean {
        val installDate = ReviewPrefs.getInstallDate(appContext)
        return isOverDate(installDate, installDaysThreshold)
    }

    /**
     * Checks if enough days have passed since the last review reminder.
     */
    private fun isOverRemindDate(): Boolean {
        val remindDate = ReviewPrefs.getRemindInterval(appContext)
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
                // Request failed - update remind date to avoid showing too frequently
                Log.e(TAG, "Review flow request failed", request.exception)
                ReviewPrefs.setRemindIntervalDate(appContext)
            }
        }
    }

    /**
     * Launches the actual review dialog UI.
     */
    private fun startReview(
        activity: Activity,
        reviewManager: ReviewManager,
        reviewInfo: ReviewInfo
    ) {
        val flow = reviewManager.launchReviewFlow(activity, reviewInfo)

        flow.addOnCompleteListener { task ->
            // The API doesn't indicate whether the user reviewed or dismissed
            // Update remind date regardless of result
            ReviewPrefs.setRemindIntervalDate(appContext)

            if (task.isSuccessful) {
                Log.d(TAG, "Review flow completed successfully.")
            } else {
                Log.e(TAG, "Review flow failed", task.exception)
            }
        }
    }
}