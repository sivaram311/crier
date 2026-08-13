package buzz.delena.crier.notify

/** Pure quiet-hours math; handles wrap across midnight. */
object QuietHours {
    fun isQuiet(nowMinutes: Int, startMinutes: Int, endMinutes: Int): Boolean {
        val now = ((nowMinutes % (24 * 60)) + (24 * 60)) % (24 * 60)
        val start = ((startMinutes % (24 * 60)) + (24 * 60)) % (24 * 60)
        val end = ((endMinutes % (24 * 60)) + (24 * 60)) % (24 * 60)
        return if (start == end) {
            false
        } else if (start < end) {
            now in start until end
        } else {
            now >= start || now < end
        }
    }
}
