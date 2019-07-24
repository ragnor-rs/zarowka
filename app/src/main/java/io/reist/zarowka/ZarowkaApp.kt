package io.reist.zarowka

import android.app.Application

/**
 * Created by reist on 04.01.2018.
 */
class ZarowkaApp: Application() {

    val colorDeviceInteractor: ColorDeviceInteractor = LightBulbInteractor(this)

    val rmsMeasurer: RmsMeasurer = VisualizerRmsMeasurer()

    val animator = Animator(this)

}