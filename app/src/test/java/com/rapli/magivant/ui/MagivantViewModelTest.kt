package com.rapli.magivant.ui

import android.app.Application
import android.content.Context
import android.hardware.usb.UsbDevice
import android.net.Uri
import android.os.Binder
import android.os.Parcel
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.rapli.magivant.usb.DacConnection
import com.rapli.magivant.usb.DndControl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class MagivantViewModelTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()
    private val dispatcher = StandardTestDispatcher()
    private val transport = FakeConnection()
    private val dnd = FakeDndControl()
    private val store = ViewModelStore()
    private lateinit var application: Application
    private lateinit var presets: PresetManager
    private lateinit var model: MagivantViewModel
    private val savedState = SavedStateHandle()
    private val preset = DacPreset("IEM", 36, -2, true, 4, 1, 47, 2)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        application = RuntimeEnvironment.getApplication()
        application.getSharedPreferences("MagivantSettings", Context.MODE_PRIVATE).edit().clear().commit()
        presets = PresetManager(temporaryFolder.newFolder())
        model = MagivantViewModel(application, transport, presets, dnd, savedState, dispatcher)
        store.put("model", model)
    }

    @After
    fun tearDown() {
        store.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    @Test
    fun disconnectDuringInitialSync_cancelsReadsAndPolling() = runModelTest {
        transport.readGate = CompletableDeferred()
        model.connectToDevice(device("dac"))
        runCurrent()
        val reads = transport.reads
        model.onDeviceDisconnected()
        runCurrent()
        advanceTimeBy(5000)
        runCurrent()
        assertFalse(model.uiState.value.isConnected)
        assertFalse(model.uiState.value.isSyncing)
        assertEquals(reads, transport.reads)
        assertEquals(null, transport.activeSession)
    }

    @Test
    fun disconnectingAnotherDevice_keepsTheDacConnected() = runModelTest {
        model.connectToDevice(device("dac"))
        advanceTimeBy(300)
        model.onDeviceDetached(device("keyboard"))
        assertTrue(model.uiState.value.isConnected)
        assertTrue(model.uiState.value.hasDeviceState)
    }

    @Test
    fun repeatedConnectionNotification_doesNotRestartTheSession() = runModelTest {
        model.connectToDevice(device("dac"))
        advanceTimeBy(300)
        model.connectToDevice(device("dac"))
        runCurrent()
        assertEquals(1, transport.opens)
    }

    @Test
    fun reconnect_doesNotResumeAnOldBlockedSync() = runModelTest {
        transport.readGate = CompletableDeferred()
        model.connectToDevice(device("dac"))
        runCurrent()
        model.onDeviceDisconnected()
        transport.readGate = null
        model.connectToDevice(device("dac"))
        advanceTimeBy(400)
        assertTrue(model.uiState.value.hasDeviceState)
        assertEquals(2, transport.opens)
        assertEquals(0, transport.staleRequests)
    }

    @Test
    fun initialSync_enforcesSavedRegisterLimit() = runModelTest {
        transport.rawVolume = 0
        model.setMaxVolumeLimit(36)
        model.connectToDevice(device("dac"))
        advanceTimeBy(400)
        assertEquals(48, transport.rawVolume)
        assertEquals(48, model.uiState.value.rawVolumeReg)
        assertTrue(model.uiState.value.hasDeviceState)
    }

    @Test
    fun loweringLimit_correctsHalfStepAboveTheRoundedIndex() = runModelTest {
        transport.rawVolume = 47
        model.connectToDevice(device("dac"))
        advanceTimeBy(300)
        model.setMaxVolumeLimit(36)
        advanceTimeBy(200)
        assertEquals(48, transport.rawVolume)
        assertEquals(48, model.uiState.value.rawVolumeReg)
    }

    @Test
    fun applyingPreset_mutesFirstAndConfirmsEverySetting() = runModelTest {
        model.connectToDevice(device("dac"))
        advanceTimeBy(300)
        model.applyPreset(preset)
        advanceTimeBy(550)
        assertEquals(listOf(4, 5, 2, 1, 6, 4), transport.commands.map { it.first })
        assertEquals(255, transport.commands.first().second)
        assertEquals(47, transport.commands.last().second)
        assertEquals("IEM", model.uiState.value.activePresetName)
        assertEquals(-2, model.uiState.value.balanceBaseValue)
        assertFalse(model.uiState.value.isSyncing)
    }

    @Test
    fun failedPreset_stopsBeforeUnmutingAndDoesNotReportSuccess() = runModelTest {
        model.connectToDevice(device("dac"))
        advanceTimeBy(300)
        transport.failCommand = 2
        model.applyPreset(preset)
        advanceTimeBy(500)
        assertEquals(listOf(4, 5), transport.commands.map { it.first })
        assertEquals(255, transport.rawVolume)
        assertEquals("", model.uiState.value.activePresetName)
        assertNotNull(model.uiState.value.errorMessage)
        assertFalse(model.uiState.value.isSyncing)
    }

    @Test
    fun failedVolumeWrite_keepsTheLastConfirmedState() = runModelTest {
        model.connectToDevice(device("dac"))
        advanceTimeBy(300)
        val oldVolume = model.uiState.value.rawVolumeReg
        transport.failCommand = 4
        model.setVolume(60)
        advanceTimeBy(100)
        assertEquals(oldVolume, model.uiState.value.rawVolumeReg)
        assertNotNull(model.uiState.value.errorMessage)
    }

    @Test
    fun autoDndPreference_survivesResumeWhileDisconnected() = runModelTest {
        model.toggleDndMode(true)
        model.syncDndState()
        assertTrue(model.uiState.value.isDndEnabled)
        assertFalse(dnd.active)
        model.connectToDevice(device("dac"))
        advanceTimeBy(300)
        assertTrue(dnd.active)
        model.onDeviceDisconnected()
        assertFalse(dnd.active)
        assertTrue(model.uiState.value.isDndEnabled)
    }

    @Test
    fun requestingDndPermission_preservesThePendingPreference() = runModelTest {
        dnd.permission = false
        model.toggleDndMode(true)
        assertTrue(dnd.permissionRequested)
        assertTrue(model.uiState.value.isDndEnabled)
        dnd.permission = true
        model.connectToDevice(device("dac"))
        advanceTimeBy(300)
        assertTrue(dnd.active)
    }

    @Test
    fun presetLabel_isClearedAfterManualChange() = runModelTest {
        presets.savePreset(preset)
        model.connectToDevice(device("dac"))
        advanceTimeBy(300)
        model.applyPreset(preset)
        advanceTimeBy(550)
        model.setBalance(3)
        advanceTimeBy(100)
        assertEquals("", model.uiState.value.activePresetName)
    }

    @Test
    fun exportAfterRecreation_retainsTheSelectedPresetSnapshot() = runModelTest {
        assertTrue(model.preparePresetExport(preset))
        val restored = SavedStateHandle(mapOf("pending_preset_export" to savedState.get<String>("pending_preset_export")))
        val recreated = MagivantViewModel(application, transport, presets, dnd, restored, dispatcher)
        store.put("recreated", recreated)
        val output = temporaryFolder.newFile("export.json")
        recreated.exportPreparedPreset(application, Uri.fromFile(output))
        runCurrent()
        assertEquals(preset, PresetCodec.decode(output.readText()))
    }

    @Test
    fun clearingViewModel_cancelsTheSessionAndClosesItsConnection() = runModelTest {
        model.connectToDevice(device("dac"))
        advanceTimeBy(300)
        store.clear()
        runCurrent()
        val reads = transport.reads
        advanceTimeBy(5000)
        assertEquals(reads, transport.reads)
        assertEquals(null, transport.activeSession)
        assertFalse(dnd.active)
    }

    private fun runModelTest(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        try {
            block()
        } finally {
            store.clear()
            runCurrent()
        }
    }

    private fun device(name: String): UsbDevice {
        val parcel = Parcel.obtain()
        try {
            parcel.writeString(name)
            listOf(0x2FC6, 0xF13B, 0, 0, 0).forEach { parcel.writeInt(it) }
            listOf("Leteciel", "Magivant", "1.0").forEach { parcel.writeString(it) }
            parcel.writeStrongBinder(Binder())
            parcel.writeParcelableArray(emptyArray<Parcelable>(), 0)
            repeat(5) { parcel.writeInt(0) }
            parcel.setDataPosition(0)
            return UsbDevice.CREATOR.createFromParcel(parcel)
        } finally {
            parcel.recycle()
        }
    }

    private class FakeDndControl : DndControl {
        var permission = true
        var permissionRequested = false
        var active = false
        override fun hasDndPermission() = permission
        override fun requestDndPermission(): Boolean {
            permissionRequested = true
            return true
        }
        override fun setDndActive(activate: Boolean): Boolean {
            active = activate && permission
            return !activate || permission
        }
    }

    private class FakeConnection : DacConnection {
        var activeSession: Long? = null
        var opens = 0
        var reads = 0
        var staleRequests = 0
        var rawVolume = 48
        var balance = 0
        var gain = 0
        var filter = 0
        var led = 0
        var failCommand: Int? = null
        var readGate: CompletableDeferred<Unit>? = null
        val commands = mutableListOf<Pair<Int, Int>>()

        override suspend fun openConnection(device: UsbDevice, sessionId: Long) {
            activeSession = sessionId
            opens++
        }

        override suspend fun closeConnection(sessionId: Long) {
            if (activeSession == sessionId) activeSession = null
        }

        private fun checkSession(sessionId: Long) {
            if (sessionId != activeSession) {
                staleRequests++
                throw IOException("Stale session")
            }
        }

        override suspend fun sendCommand(sessionId: Long, commandId: Byte, value1: Byte, value2: Byte, value3: Byte) {
            checkSession(sessionId)
            val command = commandId.toInt() and 0xFF
            if (command == failCommand) throw IOException("Transfer failed")
            val value = value1.toInt() and 0xFF
            commands += command to value
            when (command) {
                4 -> rawVolume = value
                5 -> balance = if (value > 0) value else -(value2.toInt() and 0xFF)
                2 -> gain = value
                1 -> filter = value
                6 -> led = value
            }
        }

        override suspend fun readData(sessionId: Long, commandId: Byte): ByteArray {
            checkSession(sessionId)
            reads++
            readGate?.await()
            return ByteArray(7).apply {
                when (commandId.toInt() and 0xFF) {
                    0xA0 -> { this[3] = 3; this[4] = 1 }
                    0xA1 -> this[3] = 0x11
                    0xA2 -> {
                        this[4] = rawVolume.toByte()
                        this[5] = balance.coerceAtLeast(0).toByte()
                        this[6] = (-balance).coerceAtLeast(0).toByte()
                    }
                    0xA3 -> { this[3] = filter.toByte(); this[4] = gain.toByte(); this[5] = led.toByte() }
                    0xA4 -> this[4] = 1
                }
            }
        }
    }
}
