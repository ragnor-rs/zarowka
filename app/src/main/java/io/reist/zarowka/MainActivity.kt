package io.reist.zarowka

import android.Manifest.permission
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity(), AnimatorClient {

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

    private lateinit var seekBarColorRed: SeekBar
    private lateinit var seekBarColorGreen: SeekBar
    private lateinit var seekBarColorBlue: SeekBar
    private lateinit var checkBoxVisualizer: CheckBox

    private lateinit var zarowkaPrefs: ZarowkaPrefs

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

    }

    override fun onStart() {

        super.onStart()

        val zarowkaApp = application as ZarowkaApp

        val colorDeviceInteractor = zarowkaApp.colorDeviceInteractor
        if (colorDeviceInteractor is WindowInteractor) {
            colorDeviceInteractor.attach(window)
        }

        zarowkaApp.animator.attach(this)

        requestFeatures()

    }

    override fun onStop() {

        super.onStop()

        zarowkaPrefs.red = seekBarColorRed.progress
        zarowkaPrefs.green = seekBarColorGreen.progress
        zarowkaPrefs.blue = seekBarColorBlue.progress
        zarowkaPrefs.visuals = checkBoxVisualizer.isChecked

        val zarowkaApp = application as ZarowkaApp

        zarowkaApp.animator.detach()

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

    override fun setVisualizerCheckbox(v: Boolean) {
        checkBoxVisualizer.post {
            checkBoxVisualizer.setOnCheckedChangeListener(null)
            checkBoxVisualizer.isChecked = v
            checkBoxVisualizer.setOnCheckedChangeListener(checkBoxListener)
        }
    }

    companion object {
        const val REQUEST_ENABLE_BT = 1033
        const val REQUEST_PERMISSIONS = 1034
    }

}
