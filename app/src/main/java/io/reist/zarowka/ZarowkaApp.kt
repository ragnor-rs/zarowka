package io.reist.zarowka

import android.app.Application

/**
 * Created by reist on 04.01.2018.
 */
class ZarowkaApp: Application() {

    val colorDeviceInteractor: ColorDeviceInteractor = WindowInteractor() // LightBulbInteractor(this)

    val rmsMeasurer: RmsMeasurer = VisualizerRmsMeasurer()

}