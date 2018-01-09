package io.reist.zarowka

/**
 * Created by reist on 04.01.2018.
 */
interface RmsMeasurer {

    fun addListener(listener: Listener)
    fun trackRms()
    fun release()
    fun isStarted(): Boolean

    interface Listener {
        fun onRmsTrackingStarted()
        fun onRmsTrackingStopped()
        fun onRmsValuesUpdated(rms: DoubleArray)
    }

}