package com.elytelabs.inappflow

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private val inAppUpdateManager by lazy { InAppUpdateManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Show Rating Dialog With Default Params
        showDefaultRatingDialog(this)

        // or customise the dialog

//        InAppReviewManager.with(this)
//            .setInstallDays(2)
//            .setLaunchTimes(3)
//            .setRemindInterval(2)
//            .monitor()
//        // Show a dialog if meets conditions
//        InAppReviewManager.showRateDialogIfNeeded(this)

        inAppUpdateManager.setupInAppUpdate()
    }

    override fun onDestroy() {
        super.onDestroy()
        inAppUpdateManager.onDestroy()
    }
}