package io.reist.zarowka

import android.media.audiofx.Visualizer
import android.media.audiofx.Visualizer.SCALING_MODE_NORMALIZED

/**
 * Created by reist on 03.01.2018.
 */
class VisualizerRmsMeasurer : RmsMeasurer, Runnable {

//    private val maxRms = DoubleArray(NUM_BANDS)

    private val listeners: MutableList<RmsMeasurer.Listener> = ArrayList()

    override fun addListener(listener: RmsMeasurer.Listener) {
        this.listeners.add(listener)
    }

    override fun removeListener(listener: RmsMeasurer.Listener) {
        listeners.remove(listener)
    }

    private var running = false
    private var finishing = false

    private var thread: Thread? = null

    override fun trackRms() {

        if (isStarted()) {
            return
        }

        running = true
        finishing = false

        thread = Thread( this)
        thread!!.start()

        listeners.forEach { it.onRmsTrackingStarted() }

    }

    override fun release() {

        if (!isStarted()) {
            return
        }

        finishing = true

    }

    companion object {

        const val TAG = "VisualizerRmsMeasurer"

        const val UPDATE_PERIOD = 33 // in milliseconds

        const val NUM_BANDS = 3 // todo make it changeable

        const val NANOSECONDS_IN_MILLISECONDS = 1000.0 * 1000.0
        const val MILLIHERTZ_IN_HERTZ = 1000.0

    }

    override fun isStarted(): Boolean = running

    override fun run() {

        val visualizer = Visualizer(0)
        visualizer.scalingMode = SCALING_MODE_NORMALIZED
        visualizer.enabled = true

        val fftData = ByteArray(visualizer.captureSize)

        val samplingRate = visualizer.samplingRate / MILLIHERTZ_IN_HERTZ
        val numValues = fftData.size

        val topFreq = DoubleArray(NUM_BANDS)
        topFreq[topFreq.size - 1] = samplingRate / 2
        for (i in topFreq.size - 2 downTo 0) {
            topFreq[i] = topFreq[i + 1] / 10  // logarithmic scale
        }

        val rms = DoubleArray(NUM_BANDS)
        val num = IntArray(NUM_BANDS)

        while (!finishing) {

//            val startTime = System.nanoTime()

            visualizer.getFft(fftData)

            for (i in 0 until NUM_BANDS) {
                rms[i] = 0.0
                num[i] = 0
            }

            for (i in 2 until numValues step 2) {

                val freq = (i / 2.0 * samplingRate) / numValues
                val reSquared = (fftData[i] * fftData[i]).toDouble()
                val imSquared = (fftData[i + 1] * fftData[i + 1]).toDouble()
 //               val squaredAmp = Math.pow(Math.sqrt(reSquared + imSquared) + fftData[0], 2.0)
                val squaredAmp = reSquared + imSquared

                for (j in 0 until NUM_BANDS) {
                    if (freq < topFreq[j]) {
                        rms[j] = rms[j] + squaredAmp
                        num[j]++
                    }
                }

            }

            for (i in 0 until NUM_BANDS) {

                rms[i] /= (127.0 * 127.0 + 127.0 * 127.0)

                rms[i] = Math.sqrt(rms[i] / num[i])

//                if (rms[i] > maxRms[i]) {
//                    maxRms[i] = rms[i]
//                }
//
//                if (maxRms[i] > 0.0) {
//                    rms[i] /= maxRms[i]
//                }

            }

            listeners.forEach { it.onRmsValuesUpdated(rms) }

            sleep()

//            val endTime = System.nanoTime()
//            val dt = (endTime - startTime) / NANOSECONDS_IN_MILLISECONDS
//            Log.i(TAG, "Cycle duration = $dt ms")

        }

        visualizer.enabled = false
        visualizer.release()

        running = false
        finishing = false

        listeners.forEach { it.onRmsTrackingStopped() }

    }

    private fun sleep(): Double {
        val startTime = System.nanoTime()
        while (System.nanoTime() - startTime < UPDATE_PERIOD * NANOSECONDS_IN_MILLISECONDS) {
            Thread.yield()
        }
        return (System.nanoTime() - startTime) / NANOSECONDS_IN_MILLISECONDS
    }

}