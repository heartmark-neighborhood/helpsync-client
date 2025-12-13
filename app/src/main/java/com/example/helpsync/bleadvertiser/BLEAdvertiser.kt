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
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.UUID

class BLEAdvertiser(private val context: Context, private val uuid_string: String) {
    companion object {
        private const val TAG = "BLEAdvertiser"
    }
    
    private val serviceUuid = UUID.fromString(uuid_string)
    private val advertiser: BluetoothLeAdvertiser by lazy {
        Log.d(TAG, "initializing BluetoothLeAdvertiser")
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        Log.d(TAG, "Bluetooth adapter: enabled=${adapter?.isEnabled}, address=${adapter?.address}")
        bluetoothManager.adapter.bluetoothLeAdvertiser
    }
    private var advertiseCallback: AdvertiseCallback? = null
    
    init {
        Log.d(TAG, "BLEAdvertiser created with uuid_string=$uuid_string, serviceUuid=$serviceUuid")
    }

    fun startAdvertise(message: String, onStatusUpdate: (String) -> Unit) {
        Log.d(TAG, "startAdvertise: called with message='$message'")
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "startAdvertise: permission BLUETOOTH_ADVERTISE denied")
            onStatusUpdate("Permission DENIED")
            return
        }

        // BLE Legacy advertising limit: 31 bytes total
        // Service Data only (no separate UUID list):
        // - Flags: 3 bytes
        // - Service Data: 2 byte header + 16 byte UUID + data = 18 + data
        // Total: 21 + data bytes, leaving ~10 bytes for data (with safety margin)
        val maxServiceDataBytes = 8
        
        val originalData = message.toByteArray(Charsets.UTF_8)
        val (serviceData, trimmedMessage) = if (originalData.size > maxServiceDataBytes) {
            Log.w(TAG, "startAdvertise: message too large (${originalData.size} bytes), trimming to $maxServiceDataBytes bytes")
            
            // Trim safely: avoid breaking UTF-8 multi-byte characters
            var trimmed = message
            var trimmedBytes = trimmed.toByteArray(Charsets.UTF_8)
            while (trimmedBytes.size > maxServiceDataBytes && trimmed.isNotEmpty()) {
                trimmed = trimmed.dropLast(1)
                trimmedBytes = trimmed.toByteArray(Charsets.UTF_8)
            }
            
            Pair(trimmedBytes, trimmed)
        } else {
            Pair(originalData, message)
        }
        
        val serviceDataHex = serviceData.joinToString(separator = " ") { String.format("%02X", it) }
        Log.d(TAG, "startAdvertise: serviceData UTF-8='$trimmedMessage' hex=$serviceDataHex length=${serviceData.size} bytes")
        
        if (originalData.size > maxServiceDataBytes) {
            Log.w(TAG, "startAdvertise: original message was trimmed from ${originalData.size} to ${serviceData.size} bytes")
            onStatusUpdate("Message trimmed: ${originalData.size} -> ${serviceData.size} bytes")
        }

        // Use Service Data only (includes UUID implicitly)
        // Scanner will scan all devices and filter by checking service data presence
        val data = AdvertiseData.Builder()
            .addServiceData(ParcelUuid(serviceUuid), serviceData)
            .setIncludeDeviceName(false)
            .build()
        Log.d(TAG, "startAdvertise: AdvertiseData built with serviceData (UUID=$serviceUuid), includeDeviceName=false")

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(45000)
            .build()
        
        Log.d(TAG, "startAdvertise: AdvertiseSettings built mode=LOW_LATENCY txPower=HIGH connectable=false timeout=45000ms")

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                val mode = settingsInEffect.mode
                val txPower = settingsInEffect.txPowerLevel
                val timeout = settingsInEffect.timeout
                Log.d(TAG, "onStartSuccess: advertising started successfully. settingsInEffect: mode=$mode txPower=$txPower timeout=$timeout")
                onStatusUpdate("Advertise in progress: \"$trimmedMessage\"")
            }

            override fun onStartFailure(errorCode: Int) {
                val errorMsg = when (errorCode) {
                    ADVERTISE_FAILED_DATA_TOO_LARGE -> "DATA_TOO_LARGE"
                    ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "TOO_MANY_ADVERTISERS"
                    ADVERTISE_FAILED_ALREADY_STARTED -> "ALREADY_STARTED"
                    ADVERTISE_FAILED_INTERNAL_ERROR -> "INTERNAL_ERROR"
                    ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "FEATURE_UNSUPPORTED"
                    else -> "UNKNOWN($errorCode)"
                }
                Log.e(TAG, "onStartFailure: advertising failed with errorCode=$errorCode ($errorMsg)")
                onStatusUpdate("Advertise failure: $errorCode")
            }
        }

        Log.d(TAG, "startAdvertise: calling advertiser.startAdvertising()")
        try {
            advertiser.startAdvertising(settings, data, advertiseCallback)
        } catch (e: Exception) {
            Log.e(TAG, "startAdvertise: exception when starting advertising", e)
            onStatusUpdate("Advertise exception: ${e.message}")
        }
    }

    fun stopAdvertise() {
        Log.d(TAG, "stopAdvertise: called")
        advertiseCallback?.let {
            if(ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "stopAdvertise: stopping advertising")
                try {
                    advertiser.stopAdvertising(it)
                    Log.d(TAG, "stopAdvertise: advertising stopped successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "stopAdvertise: exception when stopping advertising", e)
                }
            } else {
                Log.w(TAG, "stopAdvertise: permission BLUETOOTH_ADVERTISE not granted")
            }
        } ?: Log.w(TAG, "stopAdvertise: advertiseCallback is null, nothing to stop")
    }
}