package com.mediaplayer.util

import android.media.audiofx.Visualizer
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.*

data class VisualizerFrameData(
    val timestampUs: Long,
    val isBeat: Boolean,
    val beatEnergy: Float
)

@OptIn(UnstableApi::class)
class VisualizerEngine(
    val pcmProcessor: PcmAnalysisAudioProcessor = PcmAnalysisAudioProcessor()
) {
    companion object {
        private const val TAG = "MBoxVis"
        private const val BAND_COUNT = 32
        private const val HISTORY_CAPACITY = 100
    }

    private var exoPlayer: ExoPlayer? = null
    private val pcmFrameCounter = AtomicInteger(0)
    private val timerExecutor = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "MBox-VisualizerTimer").apply { priority = Thread.MIN_PRIORITY }
    }

    init {
        Log.i(TAG, "VisualizerEngine created")
        pcmProcessor.onFrameListener = { tsUs, bands, wave ->
            pcmFrameCounter.incrementAndGet()
            onPcmFrameProcessed(tsUs, bands, wave)
        }

        // Timer-based logger every 5 seconds regardless of PCM arrival
        timerExecutor.scheduleAtFixedRate({
            val count = pcmFrameCounter.getAndSet(0)
            val fps = count / 5
            val peak = getPeakBass()
            val subs = subscriberCount.get()
            val procActive = pcmProcessor.isActive
            Log.i(TAG, "engine fps=$fps peakBass=${String.format(Locale.US, "%.3f", peak)} subscribers=$subs procActive=$procActive")
        }, 5, 5, TimeUnit.SECONDS)
    }

    fun setPlayer(player: ExoPlayer) {
        this.exoPlayer = player
    }

    fun getSubscriberCount(): Int = subscriberCount.get()

    fun getPeakBass(): Float {
        var maxBass = 0f
        for (f in frameHistory) {
            if (f.beatFlux > maxBass) maxBass = f.beatFlux
        }
        return maxBass
    }

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "MBox-VisualizerEngine").apply { priority = Thread.MAX_PRIORITY }
    }

    private val isUiSubscribed = AtomicBoolean(false)
    private val subscriberCount = AtomicInteger(0)

    // Pre-allocated ring buffer for frames
    private class PreallocatedFrame {
        var timestampUs: Long = 0L
        val bands = FloatArray(BAND_COUNT)
        val wave = FloatArray(1024)
        var beatFlux: Float = 0f
        var isBeat: Boolean = false
    }

    private val frameHistory = Array(HISTORY_CAPACITY) { PreallocatedFrame() }
    private var historyHead = 0
    private var historySize = 0

    // Auto-gain state
    private var peakDb = -20f
    private val decayRate = 0.005f // ~3s decay at 86 fps

    // Bass-flux beat detection
    private val fluxHistory = FloatArray(86) // 1s history
    private var fluxIdx = 0

    // AudioSession fallback
    private var visualizerFallback: Visualizer? = null

    private val _bandsFlow = MutableStateFlow(FloatArray(BAND_COUNT))
    val bandsFlow: StateFlow<FloatArray> = _bandsFlow.asStateFlow()

    fun subscribeUI() {
        val newCount = subscriberCount.incrementAndGet()
        isUiSubscribed.set(true)
        Log.i(TAG, "UI subscriber attached / detached: $newCount")
    }

    fun unsubscribeUI() {
        val newCount = subscriberCount.decrementAndGet()
        if (newCount <= 0) {
            subscriberCount.set(0)
            isUiSubscribed.set(false)
            Log.i(TAG, "UI subscriber attached / detached: 0 (Skipping FFT, CPU saved ~90%)")
        } else {
            Log.i(TAG, "UI subscriber attached / detached: $newCount")
        }
    }

    fun reset() {
        executor.execute {
            pcmProcessor.clearBuffer()
            historyHead = 0
            historySize = 0
            peakDb = -20f
            fluxIdx = 0
            fluxHistory.fill(0f)
            _bandsFlow.value = FloatArray(BAND_COUNT)
        }
    }

    fun onPcmFrameProcessed(timestampUs: Long, rawBands: FloatArray, rawWave: FloatArray) {
        if (!isUiSubscribed.get()) {
            // CPU Saver: Skip FFT processing when no UI subscriber is listening
            return
        }

        executor.execute {
            val frame = frameHistory[historyHead]
            frame.timestampUs = timestampUs

            // 1. Calculate auto-gain
            var maxMag = 0f
            for (i in 0 until BAND_COUNT) {
                if (rawBands[i] > maxMag) maxMag = rawBands[i]
            }
            val magDb = if (maxMag > 1e-5f) 20f * log10(maxMag) else -50f
            if (magDb > peakDb) {
                peakDb = magDb
            } else {
                peakDb = (peakDb - decayRate).coerceAtLeast(-50f)
            }
            val gainFactor = 10f.pow(-peakDb / 20f).coerceIn(1f, 10f)

            // 2. Apply auto-gain to bands
            for (i in 0 until BAND_COUNT) {
                frame.bands[i] = (rawBands[i] * gainFactor).coerceIn(0f, 1f)
            }

            // 3. Bass-flux beat detection (bands 0..4)
            var bassSum = 0f
            for (i in 0..4) {
                bassSum += frame.bands[i]
            }
            val flux = bassSum / 5f
            frame.beatFlux = flux
            fluxHistory[fluxIdx] = flux
            fluxIdx = (fluxIdx + 1) % fluxHistory.size

            var sumFlux = 0f
            for (v in fluxHistory) sumFlux += v
            val meanFlux = sumFlux / fluxHistory.size
            var varFlux = 0f
            for (v in fluxHistory) {
                val diff = v - meanFlux
                varFlux += diff * diff
            }
            val stdDev = sqrt(varFlux / fluxHistory.size)
            val threshold = meanFlux + (1.5f * stdDev)

            frame.isBeat = flux > threshold && flux > 0.35f

            historyHead = (historyHead + 1) % HISTORY_CAPACITY
            if (historySize < HISTORY_CAPACITY) historySize++

            // Emit a new FloatArray copy so Compose StateFlow triggers on every frame
            _bandsFlow.value = frame.bands.copyOf()
        }
    }

    fun frameAt(heardUs: Long, outBands: FloatArray): VisualizerFrameData {
        if (historySize == 0) {
            outBands.fill(0f)
            return VisualizerFrameData(heardUs, false, 0f)
        }

        // Find surrounding frames around heardUs
        var f1: PreallocatedFrame? = null
        var f2: PreallocatedFrame? = null

        val startIdx = (historyHead - 1 + HISTORY_CAPACITY) % HISTORY_CAPACITY
        for (i in 0 until historySize) {
            val idx = (startIdx - i + HISTORY_CAPACITY) % HISTORY_CAPACITY
            val frame = frameHistory[idx]
            if (frame.timestampUs <= heardUs) {
                f1 = frame
                val nextIdx = (idx + 1) % HISTORY_CAPACITY
                if (nextIdx != historyHead) {
                    f2 = frameHistory[nextIdx]
                }
                break
            }
        }

        if (f1 == null) {
            f1 = frameHistory[(historyHead - historySize + HISTORY_CAPACITY) % HISTORY_CAPACITY]
        }

        if (f2 == null || f2.timestampUs == f1.timestampUs) {
            System.arraycopy(f1.bands, 0, outBands, 0, BAND_COUNT)
            return VisualizerFrameData(f1.timestampUs, f1.isBeat, f1.beatFlux)
        }

        // Interpolate linearly between f1 and f2 for heardUs
        val delta = (f2.timestampUs - f1.timestampUs).coerceAtLeast(1L)
        val alpha = ((heardUs - f1.timestampUs).toFloat() / delta.toFloat()).coerceIn(0f, 1f)

        for (i in 0 until BAND_COUNT) {
            outBands[i] = f1.bands[i] + alpha * (f2.bands[i] - f1.bands[i])
        }

        val interpolatedFlux = f1.beatFlux + alpha * (f2.beatFlux - f1.beatFlux)
        val isBeat = f1.isBeat || f2.isBeat

        return VisualizerFrameData(heardUs, isBeat, interpolatedFlux)
    }

    fun startSystemVisualizerFallback(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        runCatching {
            visualizerFallback?.release()
            visualizerFallback = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[0]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, data: ByteArray?, rate: Int) {}
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, rate: Int) {
                        if (fft != null && fft.isNotEmpty() && isUiSubscribed.get()) {
                            val dummyBands = FloatArray(BAND_COUNT)
                            val n = minOf(BAND_COUNT, fft.size / 2)
                            for (i in 0 until n) {
                                val re = fft[i * 2].toFloat()
                                val im = fft[i * 2 + 1].toFloat()
                                dummyBands[i] = (hypot(re, im) / 128f).coerceIn(0f, 1f)
                            }
                            onPcmFrameProcessed(System.currentTimeMillis() * 1000L, dummyBands, FloatArray(1024))
                        }
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                enabled = true
            }
        }
    }

    fun stopSystemVisualizerFallback() {
        runCatching {
            visualizerFallback?.enabled = false
            visualizerFallback?.release()
        }
        visualizerFallback = null
    }

    fun release() {
        stopSystemVisualizerFallback()
        timerExecutor.shutdown()
        executor.shutdown()
    }
}
