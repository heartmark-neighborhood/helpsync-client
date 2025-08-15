package com.example.helpsync.bleadvertiser

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import androidx.core.app.ActivityCompat
import java.util.UUID

class BLEAdvertiser(private val context: Context, private val uuid_string: String) {
    private val serviceUuid = UUID.fromString(uuid_string)
    private val advertiser: BluetoothLeAdvertiser by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter.bluetoothLeAdvertiser
    }
    private var advertiseCallback: AdvertiseCallback? = null

    fun startAdvertise(message: String, onStatusUpdate: (String) -> Unit) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            onStatusUpdate("Permission DENIED")
            return
        }

        val serviceData = message.toByteArray(Charsets.UTF_8)

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(serviceUuid))
            .addServiceData(ParcelUuid(serviceUuid), serviceData)
            .setIncludeDeviceName(false)
            .build()

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(45000)
            .build()

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(setingsInEffect: AdvertiseSettings) {
                onStatusUpdate("Advertise in progress: \"$message\"")
            }

            override fun onStartFailure(errorCode: Int) {
                onStatusUpdate("Advertise failuer: $errorCode")
            }
        }

        advertiser.startAdvertising(settings, data, advertiseCallback)
    }

    fun stopAdvertise() {
        advertiseCallback?.let {
            if(ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                advertiser.stopAdvertising(it)
            }
        }
    }
}