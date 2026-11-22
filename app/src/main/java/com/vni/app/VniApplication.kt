package com.vni.app

import android.app.Application
import com.vni.app.notifications.NotificationHelper

class VniApplication : Application() {
    companion object {
        lateinit var instance: VniApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.createNotificationChannels(this)
    }
}
