package buzz.delena.crier.notify

import android.content.Context
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Watches device call state so the notification-speech pipeline can pause
 * while a call is ringing/active and resume once it ends. `RINGING` and
 * `OFFHOOK` both count as "wait" — a queued notification shouldn't start
 * talking over an incoming-call ring either.
 */
class CallStateGate(context: Context) {
    private val appContext = context.applicationContext
    private val telephonyManager =
        appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private val _isCallActive = MutableStateFlow(false)
    val isCallActive: StateFlow<Boolean> = _isCallActive

    private var legacyListener: android.telephony.PhoneStateListener? = null
    private var modernCallback: TelephonyCallback? = null

    fun start() {
        val manager = telephonyManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    _isCallActive.value = state != TelephonyManager.CALL_STATE_IDLE
                }
            }
            modernCallback = callback
            runCatching {
                manager.registerTelephonyCallback(appContext.mainExecutor, callback)
            }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : android.telephony.PhoneStateListener() {
                @Deprecated("Legacy path for API < 31")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    _isCallActive.value = state != TelephonyManager.CALL_STATE_IDLE
                }
            }
            legacyListener = listener
            @Suppress("DEPRECATION")
            runCatching {
                manager.listen(listener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
            }
        }
    }

    fun stop() {
        val manager = telephonyManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            modernCallback?.let { runCatching { manager.unregisterTelephonyCallback(it) } }
            modernCallback = null
        } else {
            @Suppress("DEPRECATION")
            legacyListener?.let {
                runCatching { manager.listen(it, android.telephony.PhoneStateListener.LISTEN_NONE) }
            }
            legacyListener = null
        }
    }
}
