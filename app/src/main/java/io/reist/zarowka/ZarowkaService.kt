package io.reist.zarowka

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ZarowkaService : Service() {

    private lateinit var colorDeviceInteractor: ColorDeviceInteractor
    private lateinit var rmsMeasurer: RmsMeasurer

    override fun onCreate() {

        super.onCreate()

        val zarowkaApp = application as ZarowkaApp
        this.colorDeviceInteractor = zarowkaApp.colorDeviceInteractor
        this.rmsMeasurer = zarowkaApp.rmsMeasurer

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action != null) {
            when(action) {
                ACTION_START -> start(
                        intent.getIntExtra(EXTRA_COLOR, 0),
                        intent.getBooleanExtra(EXTRA_VISUALS, false)
                )
                ACTION_STOP -> stop()
                ACTION_VISUALS_ON -> visualsOn()
                ACTION_VISUALS_OFF -> visualsOff()
                ACTION_SET_COLOR -> setColor(
                        intent.getIntExtra(EXTRA_COLOR, 0)
                )
            }
        }
        return START_NOT_STICKY
    }

    private fun setColor(color: Int) {
        if (colorDeviceInteractor.isStarted()) {
            colorDeviceInteractor.listDevices().forEach {
                colorDeviceInteractor.setColor(it, color)
            }
        }
    }

    private fun visualsOn() {
        startForeground()
        rmsMeasurer.trackRms()
    }

    private fun stop() {

        stopForeground()

        rmsMeasurer.release()
        if (!colorDeviceInteractor.isStarted()) {
            return
        }
        colorDeviceInteractor.stop()

    }

    private fun stopForeground() {
        stopForeground(true)
    }

    private fun visualsOff() {
        rmsMeasurer.release()
        stopForeground()
    }

    private fun start(color: Int, visuals: Boolean) {

        if (colorDeviceInteractor.isStarted()) {
            return
        }

        colorDeviceInteractor.start(color)

        if (visuals) {
            rmsMeasurer.trackRms()
        } else {
            rmsMeasurer.release()
        }

    }

    private fun startForeground() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val pendingIntent = PendingIntent.getService(
                this,
                0,
                getStopIntent(this),
                PendingIntent.FLAG_ONE_SHOT
        )

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(getString(R.string.app_name))
                .setContentIntent(pendingIntent)

        startForeground(NOTIFICATION_ID, builder.build())

    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val name = getString(R.string.app_name)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel(NOTIFICATION_CHANNEL, name, importance)
        mChannel.enableLights(false)
        mChannel.enableVibration(false)
        mChannel.setSound(null, null)
        notificationManager.createNotificationChannel(mChannel)
    }

    override fun onDestroy() {
        super.onDestroy()
        stop()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_VISUALS_ON = "ACTION_VISUALS_ON"
        const val ACTION_VISUALS_OFF = "ACTION_VISUALS_OFF"
        const val ACTION_SET_COLOR = "ACTION_SET_COLOR"

        const val EXTRA_COLOR = "EXTRA_COLOR"
        const val EXTRA_VISUALS = "EXTRA_VISUALS"

        fun getStartIntent(context: Context, color: Int, visuals: Boolean): Intent {
            val intent = Intent(context, ZarowkaService::class.java)
            intent.action = ACTION_START
            intent.putExtra(EXTRA_COLOR, color)
            intent.putExtra(EXTRA_VISUALS, visuals)
            return intent
        }

        fun getStopIntent(context: Context): Intent {
            val intent = Intent(context, ZarowkaService::class.java)
            intent.action = ACTION_STOP
            return intent
        }

        fun getVisualsOnIntent(context: Context): Intent {
            val intent = Intent(context, ZarowkaService::class.java)
            intent.action = ACTION_VISUALS_ON
            return intent
        }

        fun getVisualsOffIntent(context: Context): Intent {
            val intent = Intent(context, ZarowkaService::class.java)
            intent.action = ACTION_VISUALS_OFF
            return intent
        }

        fun getSetColorIntent(context: Context, color: Int): Intent {
            val intent = Intent(context, ZarowkaService::class.java)
            intent.action = ACTION_SET_COLOR
            intent.putExtra(EXTRA_COLOR, color)
            return intent
        }

        const val NOTIFICATION_ID = 1

        const val NOTIFICATION_CHANNEL = "zarowka"

    }

}
