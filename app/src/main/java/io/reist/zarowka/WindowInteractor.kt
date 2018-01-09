package io.reist.zarowka

import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Window

/**
 * Created by reist on 05.01.2018.
 */
class WindowInteractor : ColorDeviceInteractor {

    private var started: Boolean = false

    override fun listDevices(): Set<String> = setOf(DEVICE_ID)

    override fun isStarted(): Boolean = started

    override fun start(initialColor: Int) {
        started = true
        setColor(DEVICE_ID, initialColor)
    }

    fun attach(window: Window) {
        window.setBackgroundDrawable(colorDrawable)
    }

    fun detach() {
        handler.removeCallbacksAndMessages(null)
    }

    override fun stop() {
        started = false
    }

    private var color: Int = 0

    override fun setColor(device: String, color: Int) {
        if (DEVICE_ID == device) {
            this.color = color
            setColorInternal(color)
        }
    }

    override fun getColor(device: String): Int = color

    companion object {
        const val DEVICE_ID = "DEVICE_ID"
    }

    private val handler = Handler(Looper.getMainLooper())
    private val colorDrawable = ColorDrawable()

    private fun setColorInternal(color: Int) {
        handler.post {
            colorDrawable.color = color
        }
    }

}