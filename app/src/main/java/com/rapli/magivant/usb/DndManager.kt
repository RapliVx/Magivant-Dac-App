package com.rapli.magivant.usb

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

class DndManager(context: Context) {
    private val appContext = context.applicationContext
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun hasDndPermission(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun requestDndPermission(context: Context = appContext) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("DndManager", "DND settings activity unavailable", e)
        }
    }

    fun isDndActive(): Boolean {
        return try {
            val filter = notificationManager.currentInterruptionFilter
            filter != NotificationManager.INTERRUPTION_FILTER_ALL && 
            filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        } catch (e: Exception) {
            Log.e("DndManager", "Failed to read interruption filter", e)
            false
        }
    }

    fun setDndActive(activate: Boolean) {
        if (!hasDndPermission()) {
            Log.w("DndManager", "DND permission missing")
            return
        }

        try {
            val targetFilter = if (activate) {
                NotificationManager.INTERRUPTION_FILTER_ALARMS
            } else {
                NotificationManager.INTERRUPTION_FILTER_ALL
            }

            if (notificationManager.currentInterruptionFilter != targetFilter) {
                notificationManager.setInterruptionFilter(targetFilter)
                Log.i("DndManager", "Interruption filter set to $targetFilter")
            }
        } catch (e: SecurityException) {
            Log.e("DndManager", "DND access denied", e)
        } catch (e: Exception) {
            Log.e("DndManager", "Failed to update interruption filter", e)
        }
    }
}