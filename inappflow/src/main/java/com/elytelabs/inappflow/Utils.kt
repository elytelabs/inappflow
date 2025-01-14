package com.elytelabs.inappflow

import android.app.Activity


    fun showDefaultRatingDialog(activity: Activity) {
        InAppReviewManager.with(activity)
            .setInstallDays(2)
            .setLaunchTimes(3)
            .setRemindInterval(2)
            .monitor()
        // Show a dialog if meets conditions
        InAppReviewManager.showRateDialogIfNeeded(activity)
    }
