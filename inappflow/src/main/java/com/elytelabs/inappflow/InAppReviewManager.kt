package com.elytelabs.inappflow

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import java.util.Date

class InAppReviewManager private constructor(context: Context) {

    private var installDate = 2
    private var launchTimes = 3
    private var remindInterval = 2

    private val context = context.applicationContext

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: InAppReviewManager? = null

        fun with(context: Context): InAppReviewManager {
            return instance ?: synchronized(this) {
                instance ?: InAppReviewManager(context).also { instance = it }
            }
        }

        fun showRateDialogIfNeeded(activity: Activity) {
            val isMeetsConditions = instance?.shouldShowRateDialog() ?: false
            if (isMeetsConditions) {
                instance?.showRateDialog(activity)
            }
        }

        private fun isOverDate(targetDate: Long, threshold: Int): Boolean {
            return Date().time - targetDate >= threshold * 24 * 60 * 60 * 1000
        }
    }

    fun setLaunchTimes(launchTimes: Int): InAppReviewManager {
        this.launchTimes = launchTimes
        return this
    }

    fun setInstallDays(installDate: Int): InAppReviewManager {
        this.installDate = installDate
        return this
    }

    fun setRemindInterval(remindInterval: Int): InAppReviewManager {
        this.remindInterval = remindInterval
        return this
    }

    fun monitor() {
        if (ReviewPrefs.isFirstLaunch(context)) {
            ReviewPrefs.setInstallDate(context)
        }
        ReviewPrefs.setLaunchCount(context, ReviewPrefs.getLaunchCount(context) + 1)
    }

    fun showRateDialog(activity: Activity) {
        if (!activity.isFinishing) {
            val handler = Handler(Looper.myLooper()!!)
            handler.postDelayed({ showDialog(activity) }, 5000)
        }
    }

    fun shouldShowRateDialog(): Boolean {
        return isOverLaunchTimes() &&
                isOverInstallDate() &&
                isOverRemindDate()
    }

    private fun isOverLaunchTimes(): Boolean {
        return ReviewPrefs.getLaunchCount(context) >= launchTimes
    }

    private fun isOverInstallDate(): Boolean {
        return isOverDate(ReviewPrefs.getInstallDate(context), installDate)
    }

    private fun isOverRemindDate(): Boolean {
        return isOverDate(ReviewPrefs.getRemindInterval(context), remindInterval)
    }

    private fun showDialog(activity: Activity) {
        val reviewManager = ReviewManagerFactory.create(context)
        val requestReviewFlow  = reviewManager.requestReviewFlow()
        requestReviewFlow.addOnCompleteListener { request ->
            if (request.isSuccessful) {
                // We can get the ReviewInfo object
                val reviewInfo = request.result
                startReview(activity, reviewManager, reviewInfo)
            } else {
                // There was some problem, continue regardless of the result.
                ReviewPrefs.setRemindIntervalDate(context)
            }
        }
    }

    private fun startReview(activity: Activity, reviewManager: ReviewManager, reviewInfo: ReviewInfo) {
        val flow  = reviewManager.launchReviewFlow(activity , reviewInfo)
        flow .addOnCompleteListener {
            // The flow has finished. The API does not indicate whether the user
            // reviewed or not, or even whether the review dialog was shown. Thus, no
            // matter the result, we continue our app flow.
            ReviewPrefs.setRemindIntervalDate(activity)
        }
    }

}