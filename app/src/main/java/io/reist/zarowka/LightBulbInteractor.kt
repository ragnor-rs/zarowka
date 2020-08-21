package io.reist.zarowka

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.graphics.Color
import android.util.Log
import java.util.*


/**
 * Created by reist on 03.01.2018.
 */
class LightBulbInteractor(
        private val context: Context
): ColorDeviceInteractor, ScanCallback() {

    private var defaultColor: Int = 0

    override fun getColor(device: String): Int = colorMap[device] ?: 0

    // todo get real state from light bulbs
    private val colorMap = HashMap<String, Int>()

    override fun start(initialColor: Int) {

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)

        adapter = bluetoothManager.adapter

        if (adapter == null) {
            return
        }

        this.defaultColor = initialColor

        connectedDevices.forEach { tryToConnect(it) }

        adapter!!.bluetoothLeScanner.startScan(scanCallback)

    }

    override fun stop() {

        gattMap.clear()

        if (adapter != null) {
            val scanner = adapter!!.bluetoothLeScanner
            scanner?.stopScan(scanCallback)
            adapter = null
        }

    }

    private var adapter: BluetoothAdapter? = null

    private val gattMap = HashMap<String, BluetoothGatt>()

    private val scanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult?) {

            super.onScanResult(callbackType, result)

            if (result == null) {
                return
            }

            tryToConnect(result.device)

        }

    }

    private fun tryToConnect(device: BluetoothDevice) {
        val address = device.address
        if (address.startsWith("FC:58:FA:")) {
            Log.i(TAG, "Connecting to $address...")
            device.connectGatt(context, false, gattCallback)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {

            super.onConnectionStateChange(gatt, status, newState)

            if (gatt == null) {
                return
            }

            gattMap[gatt.device.address] = gatt

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
            }

        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {

            super.onServicesDiscovered(gatt, status)

            if (gatt == null) {
                return
            }

            val device = gatt.device.address

            setColor(device, colorMap[device] ?: defaultColor)

        }

    }

    override fun listDevices(): Set<String> = gattMap.keys

    override fun setColor(device: String, color: Int) {

        colorMap[device] = color

//        Log.i(TAG, device + " = " + color)

        setColorInternal(
                device,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        )

    }

    private fun setColorInternal(device: String, red: Int, green: Int, blue: Int) {
        val gatt = gattMap[device] ?: return
        val service = gatt.getService(UUID_SERVICE) ?: return
        val characteristic = service.getCharacteristic(UUID_CHARACTERISTIC)
        characteristic.value = byteArrayOf(0x01, green.toByte(), 0x00, 0x00, 0x01, blue.toByte(), 0x01, red.toByte(), 0x00, 0x00)
        gatt.writeCharacteristic(characteristic)
    }

    override fun isStarted(): Boolean = if (adapter == null) false else adapter!!.isEnabled

    companion object {

        const val TAG = "LightBulbInteractor"

        val UUID_SERVICE = UUID.fromString("0000CC02-0000-1000-8000-00805F9B34FB")!!
        val UUID_CHARACTERISTIC = UUID.fromString("0000EE03-0000-1000-8000-00805F9B34FB")!!

    }

}