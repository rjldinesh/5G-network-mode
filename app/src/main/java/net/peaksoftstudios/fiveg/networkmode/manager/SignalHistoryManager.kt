package net.peaksoftstudios.fiveg.networkmode.manager

data class SignalEntry(val timestampMs: Long, val dbm: Int)

/**
 * In-memory ring buffer for signal strength readings.
 * Keeps the last [capacity] samples. Thread-safe via synchronized.
 */
object SignalHistoryManager {

    private const val capacity = 60
    private val entries = ArrayDeque<SignalEntry>(capacity)

    @Synchronized
    fun record(dbm: Int) {
        if (entries.size >= capacity) entries.removeFirst()
        entries.addLast(SignalEntry(System.currentTimeMillis(), dbm))
    }

    @Synchronized
    fun snapshot(): List<SignalEntry> = entries.toList()
}
