package com.rapli.magivant.usb

import android.app.NotificationManager
import android.content.Context
import android.service.notification.Condition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class DndManagerTest {
    private lateinit var notifications: NotificationManager
    private lateinit var manager: DndManager

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("MagivantSettings", Context.MODE_PRIVATE).edit().clear().commit()
        notifications = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowOf(notifications).setNotificationPolicyAccessGranted(true)
        manager = DndManager(context)
    }

    @Test
    fun togglingDacDnd_doesNotReplaceTheGlobalInterruptionFilter() {
        notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        assertTrue(manager.setDndActive(true))
        assertEquals(1, notifications.automaticZenRules.size)
        val rule = notifications.automaticZenRules.values.single()
        assertTrue(Condition.isValidId(rule.conditionId, RuntimeEnvironment.getApplication().packageName))
        assertTrue(manager.setDndActive(false))
        assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY, notifications.currentInterruptionFilter)
    }

    @Test
    fun repeatedActivation_reusesTheExistingRule() {
        assertTrue(manager.setDndActive(true))
        assertTrue(manager.setDndActive(true))
        assertTrue(manager.setDndActive(false))
        assertTrue(manager.setDndActive(true))
        assertEquals(1, notifications.automaticZenRules.size)
    }

    @Test
    fun disabledRule_isNotSilentlyReenabled() {
        assertTrue(manager.setDndActive(true))
        val (id, rule) = notifications.automaticZenRules.entries.single()
        rule.isEnabled = false
        notifications.updateAutomaticZenRule(id, rule)
        assertFalse(manager.setDndActive(true))
        assertFalse(notifications.getAutomaticZenRule(id)!!.isEnabled)
    }

    @Test
    fun missingPermission_doesNotChangeGlobalDndOrCreateARule() {
        shadowOf(notifications).setNotificationPolicyAccessGranted(false)
        assertFalse(manager.setDndActive(true))
        assertTrue(manager.setDndActive(false))
    }
}
