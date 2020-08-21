package io.reist.zarowka

import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.Message

/**
 * Created by reist on 11.01.2018.
 */
class Animator(
        private val app: ZarowkaApp
): RmsMeasurer.Listener {

    init {
        app.rmsMeasurer.addListener(this)
    }

    private val lightBulbInteractor = app.colorDeviceInteractor

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message?) {

            super.handleMessage(msg)

            msg?.let {
                if (msg.what == MSG_DO_FRAME) {
                    doFrame()
                }
            }

            sendMessageDelayed(
                    obtainMessage(MSG_DO_FRAME), 
                    UPDATE_INTERVAL
            )

        }
    }

    private var previousRms: DoubleArray? = null

    private var currentX: DoubleArray? = null

    private var lastBeatTime = 0L

    private val initialColor = HashMap<String, Int>()
    
    private var client: AnimatorClient? = null

    private fun doFrame() {

        if (currentX == null) {
            return
        }
        
        val frameTime = System.nanoTime()

        for (i in 0 until currentX!!.size) {

            val dt = (frameTime - lastBeatTime) / NANOSECONDS_IN_MILLISECONDS

            if (dt <= BEAT_WARM_UP_TIME) {
                currentX!![i] = dt / BEAT_WARM_UP_TIME
            } else if (dt <= BEAT_COOL_DOWN_TIME) {
                currentX!![i] = (1 - (dt - BEAT_WARM_UP_TIME) / BEAT_COOL_DOWN_TIME)
            } else {
                currentX!![i] = 0.0
            }

        }

        val x = currentX!![BEAT_BAND]

        lightBulbInteractor.listDevices().forEach {

            val maxRed = Color.red(initialColor[it] ?: 0)
            val maxGreen = Color.green(initialColor[it] ?: 0)
            val maxBlue = Color.blue(initialColor[it] ?: 0)

            val color = Color.rgb(
                    (x * maxRed).toInt(),
                    (x * maxGreen).toInt(),
                    (x * maxBlue).toInt()
            )

            lightBulbInteractor.setColor(it, color)

        }
        
    }

    override fun onRmsTrackingStarted() {
        saveInitialColors()
        handler.post { client?.setVisualizerCheckbox(true) }
        startAnimation()
    }

    override fun onRmsValuesUpdated(rms: DoubleArray) {

        if (currentX == null) {
            currentX = DoubleArray(rms.size)
        }

        if (isBeatDetected(rms)) {
            lastBeatTime = System.nanoTime()
        }

        if (previousRms == null) {
            previousRms = DoubleArray(rms.size)
        }

        System.arraycopy(rms, 0, previousRms, 0, rms.size)

    }

    override fun onRmsTrackingStopped() {
        finishAnimation()
        handler.post { client?.setVisualizerCheckbox(false) }
        restoreInitialColors()
    }

    private fun isBeatDetected(rms: DoubleArray): Boolean {
        val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toDouble()
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toDouble()
        val threshold = DETECTOR_THRESHOLD * vol / maxVol
        return rms[BEAT_BAND] >= threshold && previousRms?.get(BEAT_BAND) ?: threshold < threshold
    }

    private fun saveInitialColors() {
        initialColor.clear()
        lightBulbInteractor.listDevices().forEach {
            initialColor[it] = lightBulbInteractor.getColor(it)
        }
    }

    private fun restoreInitialColors() {
        lightBulbInteractor.listDevices().forEach {
            val color = initialColor[it]
            if (color != null) {
                lightBulbInteractor.setColor(it, color)
            }
        }
    }

    private fun startAnimation() {
        handler.sendMessage(handler.obtainMessage(MSG_DO_FRAME))
    }

    private fun finishAnimation() {
        handler.removeMessages(MSG_DO_FRAME)
    }

    fun attach(client: AnimatorClient) {
        this.client = client
    }

    fun detach() {
//        app.rmsMeasurer.removeListener(this)
        this.client = null
    }
    
    companion object {
        
        const val DETECTOR_THRESHOLD = 0.2
        const val UPDATE_INTERVAL = 50L
        const val BEAT_WARM_UP_TIME = 200.0
        const val BEAT_COOL_DOWN_TIME = 1000.0
        const val BEAT_BAND = 1

        const val MSG_DO_FRAME = 1

        const val NANOSECONDS_IN_MILLISECONDS = 1000.0 * 1000.0

    }
    
}