package io.reist.zarowka

/**
 * Created by reist on 03.01.2018.
 */
interface ColorDeviceInteractor {
    fun listDevices(): Set<String>
    fun isStarted(): Boolean
    fun start(initialColor: Int)
    fun stop()
    fun setColor(device: String, color: Int)
    fun getColor(device: String): Int
}