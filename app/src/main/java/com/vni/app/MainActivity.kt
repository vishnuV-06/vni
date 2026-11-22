package com.vni.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.vni.app.notifications.NotificationHelper
import com.vni.app.ui.home.HomeScreen
import com.vni.app.ui.theme.VniTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SimpleHomeViewModel by viewModels()

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Notifications enabled
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channels
        NotificationHelper.createNotificationChannels(this)

        // Request notification permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Initialize and log Firebase Analytics on launch
        try {
            val analytics = com.google.firebase.analytics.FirebaseAnalytics.getInstance(this)
            val bundle = android.os.Bundle().apply {
                putString(com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_NAME, "app_launch")
            }
            analytics.logEvent(com.google.firebase.analytics.FirebaseAnalytics.Event.APP_OPEN, bundle)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            VniTheme {
                HomeScreen(
                    viewModel = viewModel,
                    modifier  = Modifier.fillMaxSize()
                )
            }
        }
    }
}
