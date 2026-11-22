package com.vni.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.vni.app.SimpleHomeViewModel
import com.vni.app.notifications.NotificationHelper

/**
 * Manages offline upload queuing and auto-upload with notification when internet reconnects (Requirement 5).
 */
object CloudUploadQueueManager {
    private const val PREFS_NAME = "vni_cloud_upload_queue"
    private const val KEY_PENDING = "pending_cloud_upload"
    private const val KEY_EMAIL = "pending_upload_email"
    private const val KEY_NAME = "pending_upload_name"
    private const val KEY_SECURITY_KEY = "pending_upload_security_key"

    @Volatile
    private var isNetworkCallbackRegistered = false

    fun isOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    fun queueUpload(context: Context, email: String, name: String, securityKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_PENDING, true)
            .putString(KEY_EMAIL, email)
            .putString(KEY_NAME, name)
            .putString(KEY_SECURITY_KEY, securityKey)
            .apply()

        registerNetworkListener(context.applicationContext)
    }

    fun hasPendingUpload(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PENDING, false)
    }

    fun clearPendingUpload(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_PENDING).remove(KEY_EMAIL).remove(KEY_NAME).remove(KEY_SECURITY_KEY).apply()
    }

    fun registerNetworkListener(context: Context) {
        if (isNetworkCallbackRegistered) return
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    processPendingUploadIfAny(context)
                }
            })
            isNetworkCallbackRegistered = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun processPendingUploadIfAny(context: Context, viewModel: SimpleHomeViewModel? = null) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_PENDING, false)) return

        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val name = prefs.getString(KEY_NAME, "") ?: ""
        val securityKey = prefs.getString(KEY_SECURITY_KEY, "") ?: ""

        if (email.isBlank()) {
            clearPendingUpload(context)
            return
        }

        try {
            val vm = viewModel ?: SimpleHomeViewModel()
            vm.uploadToAccountCloud(email, name, securityKey) { success, _ ->
                if (success) {
                    clearPendingUpload(context)
                    try {
                        NotificationHelper.sendNotification(
                            context = context,
                            title = "Vni Cloud Backup",
                            message = "Your data has been automatically uploaded to the cloud.",
                            notificationId = 8888,
                            channelId = NotificationHelper.CHANNEL_NOTICES_ID
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
