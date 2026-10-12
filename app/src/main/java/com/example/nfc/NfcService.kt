package com.example.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hardware and lifecycle status for NFC.
 */
enum class NfcHardwareStatus {
    NOT_SUPPORTED,
    DISABLED,
    ENABLED
}

/**
 * NfcService manages:
 * 1. NFC adapter initialization and hardware capability detection.
 * 2. NFC hardware status checking (Hardware presence, Enabled/Disabled states).
 * 3. Reader Mode configuration for contactless EMV / smart card reading (ISO-DEP, NFC-A, NFC-B).
 * 4. Basic Foreground Dispatch IntentFilter setup for contactless payment detection (ACTION_TECH_DISCOVERED / ACTION_TAG_DISCOVERED).
 * 5. Tag and Intent parsing for contactless payment transactions.
 */
class NfcService(private val context: Context) {

    companion object {
        private const val TAG = "NfcService"

        @Volatile
        private var instance: NfcService? = null

        fun getInstance(context: Context): NfcService {
            return instance ?: synchronized(this) {
                instance ?: NfcService(context.applicationContext).also { instance = it }
            }
        }
    }

    private var nfcAdapter: NfcAdapter? = null

    private val _hardwareStatus = MutableStateFlow(NfcHardwareStatus.NOT_SUPPORTED)
    val hardwareStatus: StateFlow<NfcHardwareStatus> = _hardwareStatus.asStateFlow()

    private val _isReaderModeActive = MutableStateFlow(false)
    val isReaderModeActive: StateFlow<Boolean> = _isReaderModeActive.asStateFlow()

    init {
        initializeAdapter()
    }

    /**
     * Initializes the NFC Adapter and detects hardware support.
     */
    fun initializeAdapter(): NfcAdapter? {
        nfcAdapter = try {
            NfcAdapter.getDefaultAdapter(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining default NfcAdapter", e)
            null
        }
        checkHardwareSupport()
        return nfcAdapter
    }

    /**
     * Checks whether NFC hardware is present and enabled.
     */
    fun checkHardwareSupport(): NfcHardwareStatus {
        val adapter = nfcAdapter ?: run {
            _hardwareStatus.value = NfcHardwareStatus.NOT_SUPPORTED
            return NfcHardwareStatus.NOT_SUPPORTED
        }

        val status = if (adapter.isEnabled) {
            NfcHardwareStatus.ENABLED
        } else {
            NfcHardwareStatus.DISABLED
        }
        _hardwareStatus.value = status
        return status
    }

    /**
     * Returns true if NFC hardware is supported on this device.
     */
    fun isNfcSupported(): Boolean = nfcAdapter != null

    /**
     * Returns true if NFC is supported and currently turned on in device settings.
     */
    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true

    /**
     * Creates an array of IntentFilters tailored for contactless payment card detection.
     * Includes ACTION_TECH_DISCOVERED and ACTION_TAG_DISCOVERED for ISO-DEP (ISO 14443-4)
     * and Mifare/NFC-A / NFC-B used in Visa, Mastercard, and Meeza contactless standards.
     */
    fun createContactlessPaymentIntentFilters(): Array<IntentFilter> {
        val techFilter = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED).apply {
            try {
                addDataType("*/*")
            } catch (e: IntentFilter.MalformedMimeTypeException) {
                Log.w(TAG, "Malformed MIME type in contactless IntentFilter", e)
            }
        }

        val tagFilter = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)

        return arrayOf(techFilter, tagFilter)
    }

    /**
     * Returns the TechList 2D array for IsoDep, NfcA, and NfcB tech discovery.
     */
    fun createPaymentTechLists(): Array<Array<String>> {
        return arrayOf(
            arrayOf(IsoDep::class.java.name),
            arrayOf(NfcA::class.java.name),
            arrayOf(NfcB::class.java.name)
        )
    }

    /**
     * Creates a PendingIntent targeting the specified Activity for NFC Foreground Dispatch.
     */
    fun createNfcPendingIntent(activity: Activity): PendingIntent {
        val intent = Intent(activity, activity.javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(activity, 0, intent, flags)
    }

    /**
     * Enables traditional NFC foreground dispatch with contactless payment intent filters.
     */
    fun enableForegroundDispatch(activity: Activity) {
        val adapter = nfcAdapter ?: return
        if (!adapter.isEnabled) return

        try {
            val pendingIntent = createNfcPendingIntent(activity)
            val filters = createContactlessPaymentIntentFilters()
            val techLists = createPaymentTechLists()
            adapter.enableForegroundDispatch(activity, pendingIntent, filters, techLists)
            Log.d(TAG, "Foreground dispatch enabled for contactless payments")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable foreground dispatch", e)
        }
    }

    /**
     * Disables NFC foreground dispatch.
     */
    fun disableForegroundDispatch(activity: Activity) {
        try {
            nfcAdapter?.disableForegroundDispatch(activity)
            Log.d(TAG, "Foreground dispatch disabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disable foreground dispatch", e)
        }
    }

    /**
     * Enables modern NFC Reader Mode for contactless ISO 14443 cards (Visa/Mastercard/Meeza).
     */
    fun enableReaderMode(
        activity: Activity,
        onTagDiscovered: (Tag) -> Unit
    ) {
        val adapter = nfcAdapter ?: return
        if (!adapter.isEnabled) return

        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

        try {
            adapter.enableReaderMode(
                activity,
                { tag ->
                    Log.d(TAG, "Contactless card tag discovered via Reader Mode")
                    onTagDiscovered(tag)
                },
                flags,
                null
            )
            _isReaderModeActive.value = true
            Log.d(TAG, "NFC Reader Mode enabled with ISO-DEP/NFC-A/NFC-B flags")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable NFC reader mode", e)
        }
    }

    /**
     * Disables NFC Reader Mode.
     */
    fun disableReaderMode(activity: Activity) {
        try {
            nfcAdapter?.disableReaderMode(activity)
            _isReaderModeActive.value = false
            Log.d(TAG, "NFC Reader Mode disabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disable NFC reader mode", e)
        }
    }

    /**
     * Inspects an incoming Intent (e.g. from onNewIntent) to extract a detected NFC Tag.
     */
    fun extractTagFromIntent(intent: Intent?): Tag? {
        if (intent == null) return null
        val action = intent.action ?: return null
        if (action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            action == NfcAdapter.ACTION_NDEF_DISCOVERED
        ) {
            @Suppress("DEPRECATION")
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
        }
        return null
    }
}
