package com.elytelabs.inappflow

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.elytelabs.inappflow.demo.R
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private val inAppUpdateManager by lazy { InAppUpdateManager(this) }
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvStatus = findViewById(R.id.tvStatus)

        // Configure monitoring for In-App Review
        InAppReviewManager.with(this)
            .setInstallDays(2)
            .setLaunchTimes(3)
            .setRemindInterval(2)
            .monitor()

        setupUpdateButtons()
        setupReviewButtons()
    }

    private fun setupUpdateButtons() {
        // 1. Check Update Availability without triggering UI
        findViewById<Button>(R.id.btnCheckUpdate).setOnClickListener {
            tvStatus.text = "Checking Google Play for updates..."
            inAppUpdateManager.checkUpdateAvailability { result ->
                if (result.isAvailable) {
                    val msg = "Update available! Version code: ${result.availableVersionCode}"
                    tvStatus.text = msg
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                } else {
                    val msg = "No update available (or app is running locally in debug mode)."
                    tvStatus.text = msg
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 2. Start Immediate Update Flow
        findViewById<Button>(R.id.btnImmediateUpdate).setOnClickListener {
            tvStatus.text = "Initiating immediate update flow..."
            inAppUpdateManager.setupInAppUpdate(UpdateType.IMMEDIATE)
        }

        // 3. Start Flexible Update Flow
        findViewById<Button>(R.id.btnFlexibleUpdate).setOnClickListener {
            tvStatus.text = "Initiating flexible update flow..."
            inAppUpdateManager.setupInAppUpdate(
                updateType = UpdateType.FLEXIBLE,
                onDownloaded = {
                    tvStatus.text = "Flexible update downloaded! Ready to install."
                    Snackbar.make(findViewById(R.id.main), "An update has downloaded!", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Restart & Install") {
                            inAppUpdateManager.completeUpdate()
                        }.show()
                }
            )
        }
    }

    private fun setupReviewButtons() {
        // 1. Check & Show Review (Condition-based)
        findViewById<Button>(R.id.btnCheckReview).setOnClickListener {
            val shouldShow = InAppReviewManager.with(this).shouldShowRateDialog()
            tvStatus.text = "Should show review dialog: $shouldShow"
            Toast.makeText(this, "Conditions met: $shouldShow", Toast.LENGTH_SHORT).show()
            InAppReviewManager.showRateDialogIfNeeded(this)
        }

        // 2. Force Launch Review (Debug / Instant)
        findViewById<Button>(R.id.btnForceReview).setOnClickListener {
            tvStatus.text = "Force requesting Google Play review flow..."
            Toast.makeText(this, "Requesting review flow...", Toast.LENGTH_SHORT).show()
            InAppReviewManager.forceShowRateDialog(this)
        }

        // 3. Reset Review Counters
        findViewById<Button>(R.id.btnResetReview).setOnClickListener {
            InAppReviewManager.reset(this)
            tvStatus.text = "In-App review counters & preferences reset."
            Toast.makeText(this, "Counters reset", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        inAppUpdateManager.resumeUpdateIfNeeded()
    }

    override fun onDestroy() {
        inAppUpdateManager.onDestroy()
        super.onDestroy()
    }
}