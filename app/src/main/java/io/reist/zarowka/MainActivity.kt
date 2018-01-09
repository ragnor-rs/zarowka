package io.reist.zarowka

import android.Manifest.permission
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.SeekBar


class MainActivity : AppCompatActivity(), RmsMeasurer.Listener {

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message?) {

            super.handleMessage(msg)

            msg?.let {

                if (msg.what == MSG_DO_FRAME && targetRms != null) {

                    val frameTime = System.nanoTime()

                    if (currentRms == null) {
                        currentRms = DoubleArray(targetRms!!.size)
                    }

                    for (i in 0 until currentRms!!.size) {
                        val dt = (frameTime - lastBeatTime) / NANOSECONDS_IN_MILLISECONDS
                        if (dt < 0) {
                            Log.e(TAG, "dt = $dt")
                        }
                        if (dt <= BEAT_WARM_UP_TIME) {
                            currentRms!![i] = dt / BEAT_WARM_UP_TIME * targetRms!![i]
                        } else if (dt <= BEAT_COOL_DOWN_TIME) {
                            currentRms!![i] = (1 - (dt - BEAT_WARM_UP_TIME) / BEAT_COOL_DOWN_TIME) * targetRms!![i]
                        } else {
                            currentRms!![i] = 0.0
                        }
                    }

                    val lightBulbInteractor = (application as ZarowkaApp).colorDeviceInteractor
                    lightBulbInteractor.listDevices().forEach {

                        val maxRed = Color.red(initialColor[it] ?: 0)
                        val maxGreen = Color.green(initialColor[it] ?: 0)
                        val maxBlue = Color.blue(initialColor[it] ?: 0)

                        val color = Color.rgb(
                                (currentRms!![0] * maxRed).toInt(),
                                (currentRms!![1] * maxGreen).toInt(),
                                (currentRms!![2] * maxBlue).toInt()
                        )

                        lightBulbInteractor.setColor(it, color)

                    }

                }

            }

            val message = obtainMessage(MSG_DO_FRAME/*, frameTime*/)
            sendMessageDelayed(message, UPDATE_INTERVAL)

        }
    }

    private var previousRms: DoubleArray? = null
    private var targetRms: DoubleArray? = null
    private var currentRms: DoubleArray? = null

    private var lastBeatTime = 0L

    override fun onRmsValuesUpdated(rms: DoubleArray) {

//        val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//        val vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toDouble()
//        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toDouble()
        val threshold = DETECTOR_THRESHOLD // * vol / maxVol

        if (
            rms[0] >= threshold && previousRms?.get(0) ?: threshold < threshold
        ) {

            Log.i(TAG, "RMS = ${rms[0]}")

            if (targetRms == null) {
                targetRms = DoubleArray(rms.size)
            }
            System.arraycopy(rms, 0, targetRms, 0, rms.size)

            lastBeatTime = System.nanoTime()

        }

        if (previousRms == null) {
            previousRms = DoubleArray(rms.size)
        }
        System.arraycopy(rms, 0, previousRms, 0, rms.size)

    }

    private var initialColor = HashMap<String, Int>()

    override fun onRmsTrackingStarted() {

        initialColor.clear()

        val lightBulbInteractor = (application as ZarowkaApp).colorDeviceInteractor
        lightBulbInteractor.listDevices().forEach {
            initialColor[it] = lightBulbInteractor.getColor(it)
        }

        checkBoxVisualizer.setOnCheckedChangeListener(null)
        checkBoxVisualizer.isChecked = true
        checkBoxVisualizer.setOnCheckedChangeListener(checkBoxListener)

        handler.sendMessage(handler.obtainMessage(MSG_DO_FRAME))

    }

    override fun onRmsTrackingStopped() {

        handler.removeMessages(MSG_DO_FRAME)

        checkBoxVisualizer.setOnCheckedChangeListener(null)
        checkBoxVisualizer.isChecked = false
        checkBoxVisualizer.setOnCheckedChangeListener(checkBoxListener)

        val lightBulbInteractor = (application as ZarowkaApp).colorDeviceInteractor
        lightBulbInteractor.listDevices().forEach {
            val color = initialColor[it]
            if (color != null) {
                lightBulbInteractor.setColor(it, color)
            }
        }

    }

    private lateinit var seekBarColorRed: SeekBar
    private lateinit var seekBarColorGreen: SeekBar
    private lateinit var seekBarColorBlue: SeekBar
    private lateinit var checkBoxVisualizer: CheckBox

    private val seekBarListener = object : SeekBar.OnSeekBarChangeListener {

        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
            startService(
                    ZarowkaService.getSetColorIntent(
                            this@MainActivity,
                            Color.rgb(
                                    seekBarColorRed.progress,
                                    seekBarColorGreen.progress,
                                    seekBarColorBlue.progress
                            )
                    )
            )
        }

        override fun onStartTrackingTouch(p0: SeekBar?) {}

        override fun onStopTrackingTouch(p0: SeekBar?) {}

    }

    private val checkBoxListener = CompoundButton.OnCheckedChangeListener { _, p1 ->
        if (p1) {
            startService(ZarowkaService.getVisualsOnIntent(this@MainActivity))
        } else {
            startService(ZarowkaService.getVisualsOffIntent(this@MainActivity))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        this.zarowkaPrefs = ZarowkaPrefs(this)

        setContentView(R.layout.activity_main)

        seekBarColorRed = findViewById(R.id.color_red)
        seekBarColorGreen = findViewById(R.id.color_green)
        seekBarColorBlue = findViewById(R.id.color_blue)
        checkBoxVisualizer  = findViewById(R.id.visualizer)

        seekBarColorRed.progress = zarowkaPrefs.red
        seekBarColorGreen.progress = zarowkaPrefs.green
        seekBarColorBlue.progress = zarowkaPrefs.blue
        checkBoxVisualizer.isChecked = zarowkaPrefs.visuals

        seekBarColorRed.setOnSeekBarChangeListener(seekBarListener)
        seekBarColorGreen.setOnSeekBarChangeListener(seekBarListener)
        seekBarColorBlue.setOnSeekBarChangeListener(seekBarListener)
        checkBoxVisualizer.setOnCheckedChangeListener(checkBoxListener)

        val zarowkaApp = application as ZarowkaApp
        zarowkaApp.rmsMeasurer.addListener(this)

    }

    override fun onStart() {

        super.onStart()

        val zarowkaApp = application as ZarowkaApp
        val colorDeviceInteractor = zarowkaApp.colorDeviceInteractor
        if (colorDeviceInteractor is WindowInteractor) {
            colorDeviceInteractor.attach(window)
        }

        requestFeatures()

    }

    override fun onStop() {

        super.onStop()

        zarowkaPrefs.red = seekBarColorRed.progress
        zarowkaPrefs.green = seekBarColorGreen.progress
        zarowkaPrefs.blue = seekBarColorBlue.progress
        zarowkaPrefs.visuals = checkBoxVisualizer.isChecked

        val zarowkaApp = application as ZarowkaApp
        val colorDeviceInteractor = zarowkaApp.colorDeviceInteractor
        if (colorDeviceInteractor is WindowInteractor) {
            colorDeviceInteractor.detach()
        }

    }

    private fun requestFeatures() {

        val permissionsToRequest = ArrayList<String>()

        val locationPermissionGranted = ContextCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!locationPermissionGranted) {
            permissionsToRequest.add(permission.ACCESS_COARSE_LOCATION)
        }

        val settingsPermissionGranted = ContextCompat.checkSelfPermission(this, permission.MODIFY_AUDIO_SETTINGS) == PackageManager.PERMISSION_GRANTED
        if (!settingsPermissionGranted) {
            permissionsToRequest.add(permission.MODIFY_AUDIO_SETTINGS)
        }

        val recordPermissionGranted = ContextCompat.checkSelfPermission(this, permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!recordPermissionGranted) {
            permissionsToRequest.add(permission.RECORD_AUDIO)
        }

        if (permissionsToRequest.isEmpty()) {
            requestBluetooth()
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toTypedArray(),
                    REQUEST_PERMISSIONS
            )
        }

    }

    private fun requestBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                startService(
                        ZarowkaService.getStartIntent(
                                this,
                                Color.rgb(
                                        seekBarColorRed.progress,
                                        seekBarColorGreen.progress,
                                        seekBarColorBlue.progress
                                ),
                                checkBoxVisualizer.isChecked
                        )
                )
            } else {
                requestBluetooth()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        requestFeatures()
    }

    companion object {

        const val REQUEST_ENABLE_BT = 1033
        const val REQUEST_PERMISSIONS = 1034

        const val DETECTOR_THRESHOLD = 0.7
        const val UPDATE_INTERVAL = 20L
        const val BEAT_WARM_UP_TIME = 200.0
        const val BEAT_COOL_DOWN_TIME = 500.0

        const val TAG = "MainActivity"

        const val MSG_DO_FRAME = 1

        const val NANOSECONDS_IN_MILLISECONDS = 1000.0 * 1000.0

    }

    private lateinit var zarowkaPrefs: ZarowkaPrefs

}
