package net.peaksoftstudios.fiveg.networkmode.ui.screen.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

data class SpeedTestResult(
    val pingMs: Long = 0,
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0
)

sealed class SpeedTestState {
    object Idle : SpeedTestState()
    data class Running(val phase: String, val progress: Float) : SpeedTestState()
    data class Success(val result: SpeedTestResult) : SpeedTestState()
    data class Error(val message: String) : SpeedTestState()
}

class SpeedTestViewModel : ViewModel() {

    private val _state = mutableStateOf<SpeedTestState>(SpeedTestState.Idle)
    val state: androidx.compose.runtime.State<SpeedTestState> get() = _state

    // 10 MB test file from Cloudflare speed test CDN
    private val downloadUrl = "https://speed.cloudflare.com/__down?bytes=10000000"
    // Upload endpoint
    private val uploadUrl = "https://speed.cloudflare.com/__up"
    // Ping host
    private val pingHost = "speed.cloudflare.com"
    private val pingPort = 443

    fun startTest() {
        if (_state.value is SpeedTestState.Running) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // ---- Phase 1: Ping ----
                withContext(Dispatchers.Main) {
                    _state.value = SpeedTestState.Running("Measuring Ping...", 0.1f)
                }
                val pingMs = measurePing()

                // ---- Phase 2: Download ----
                withContext(Dispatchers.Main) {
                    _state.value = SpeedTestState.Running("Testing Download Speed...", 0.4f)
                }
                val downloadMbps = measureDownload()

                // ---- Phase 3: Upload ----
                withContext(Dispatchers.Main) {
                    _state.value = SpeedTestState.Running("Testing Upload Speed...", 0.75f)
                }
                val uploadMbps = measureUpload()

                withContext(Dispatchers.Main) {
                    _state.value = SpeedTestState.Success(
                        SpeedTestResult(pingMs, downloadMbps, uploadMbps)
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.value = SpeedTestState.Error(e.message ?: "Speed test failed")
                }
            }
        }
    }

    fun reset() {
        _state.value = SpeedTestState.Idle
    }

    private fun measurePing(): Long {
        var total = 0L
        val attempts = 4
        repeat(attempts) {
            val start = System.currentTimeMillis()
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(pingHost, pingPort), 3000)
                }
                total += System.currentTimeMillis() - start
            } catch (_: Exception) {
                total += 999
            }
        }
        return total / attempts
    }

    private fun measureDownload(): Double {
        val conn = URL(downloadUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 30_000
        conn.requestMethod = "GET"
        conn.connect()

        val startTime = System.currentTimeMillis()
        var bytesRead = 0L
        val buffer = ByteArray(8192)
        val stream: InputStream = conn.inputStream
        try {
            var n: Int
            while (stream.read(buffer).also { n = it } != -1) {
                bytesRead += n
            }
        } finally {
            stream.close()
            conn.disconnect()
        }
        val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
        return if (elapsedSec > 0) (bytesRead * 8.0 / 1_000_000.0) / elapsedSec else 0.0
    }

    private fun measureUpload(): Double {
        // Upload 2 MB of random data
        val uploadBytes = ByteArray(2_000_000) { it.toByte() }
        val conn = URL(uploadUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 30_000
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/octet-stream")
        conn.setFixedLengthStreamingMode(uploadBytes.size)
        conn.connect()

        val startTime = System.currentTimeMillis()
        try {
            conn.outputStream.use { it.write(uploadBytes) }
            conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
        val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
        return if (elapsedSec > 0) (uploadBytes.size * 8.0 / 1_000_000.0) / elapsedSec else 0.0
    }
}
