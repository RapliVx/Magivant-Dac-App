package com.rapli.magivant.usb

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.Condition
import android.service.notification.ZenPolicy
import android.util.Log
import com.rapli.magivant.MainActivity

interface DndControl {
    fun hasDndPermission(): Boolean
    fun requestDndPermission(): Boolean
    fun setDndActive(activate: Boolean): Boolean
}

class DndManager(context: Context) : DndControl {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("MagivantSettings", Context.MODE_PRIVATE)
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val conditionId = Condition.newId(appContext).appendPath("dac").appendPath("connected").build()

    override fun hasDndPermission(): Boolean = notificationManager.isNotificationPolicyAccessGranted

    override fun requestDndPermission(): Boolean {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            appContext.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("DndManager", "DND settings activity unavailable", e)
            false
        }
    }

    // Own a rule instead of reading or replacing the user's global DND setting.
    override fun setDndActive(activate: Boolean): Boolean {
        if (!hasDndPermission()) return !activate
        return try {
            var ruleId = prefs.getString("dac_dnd_rule_id", null)
            var rule = ruleId?.let { notificationManager.getAutomaticZenRule(it) }
            if (rule == null) {
                if (!activate) return true
                rule = AutomaticZenRule(
                    "Magivant DAC",
                    null,
                    ComponentName(appContext, MainActivity::class.java),
                    conditionId,
                    ZenPolicy.Builder().allowAlarms(true).allowMedia(true).build(),
                    NotificationManager.INTERRUPTION_FILTER_ALARMS,
                    true
                )
                ruleId = notificationManager.addAutomaticZenRule(rule)
                prefs.edit().putString("dac_dnd_rule_id", ruleId).apply()
            }
            if (activate && !rule.isEnabled) return false
            notificationManager.setAutomaticZenRuleState(
                requireNotNull(ruleId),
                Condition(conditionId, "DAC connected", if (activate) Condition.STATE_TRUE else Condition.STATE_FALSE)
            )
            true
        } catch (e: Exception) {
            Log.e("DndManager", "Failed to update the DAC DND rule", e)
            false
        }
    }
}
