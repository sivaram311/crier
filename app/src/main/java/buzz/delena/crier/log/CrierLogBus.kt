package buzz.delena.crier.log

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

data class LogEntry(
    val id: Long = idGen.incrementAndGet(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val payload: String? = null,
    val response: String? = null,
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))

    companion object {
        private val idGen = AtomicLong(0)
    }
}

/**
 * In-memory thread-safe live log buffer and bus for real-time diagnostics.
 */
object CrierLogBus {
    private const val MAX_LOGS = 500
    private val lock = Any()
    private val buffer = ArrayDeque<LogEntry>(MAX_LOGS)

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun log(level: LogLevel, tag: String, message: String, payload: String? = null, response: String? = null) {
        val entry = LogEntry(
            level = level,
            tag = tag,
            message = message,
            payload = payload,
            response = response,
        )

        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
        }

        synchronized(lock) {
            if (buffer.size >= MAX_LOGS) {
                buffer.removeFirst()
            }
            buffer.addLast(entry)
            _logs.value = buffer.toList()
        }
    }

    fun d(tag: String, message: String, payload: String? = null, response: String? = null) =
        log(LogLevel.DEBUG, tag, message, payload, response)

    fun i(tag: String, message: String, payload: String? = null, response: String? = null) =
        log(LogLevel.INFO, tag, message, payload, response)

    fun w(tag: String, message: String, payload: String? = null, response: String? = null) =
        log(LogLevel.WARN, tag, message, payload, response)

    fun e(tag: String, message: String, payload: String? = null, response: String? = null) =
        log(LogLevel.ERROR, tag, message, payload, response)

    fun clear() {
        synchronized(lock) {
            buffer.clear()
            _logs.value = emptyList()
        }
    }
}
