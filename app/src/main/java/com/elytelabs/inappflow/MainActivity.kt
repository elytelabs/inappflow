package com.elytelabs.inappflow

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.elytelabs.inappflow.demo.R

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

        // Initialize monitoring for In-App Review
        InAppReviewManager.with(this)
            .setInstallDays(2)
            .setLaunchTimes(3)
            .setRemindInterval(2)
            .monitor()

        // Check if conditions are met to show the review dialog
        InAppReviewManager.showRateDialogIfNeeded(this)

        // Check for and initiate in-app updates
        inAppUpdateManager.setupInAppUpdate()
    }

    override fun onResume() {
        super.onResume()
        // Ensures that an immediate update in progress is resumed
        inAppUpdateManager.resumeUpdateIfNeeded()
    }

    override fun onDestroy() {
        inAppUpdateManager.onDestroy()
        super.onDestroy()
    }
}