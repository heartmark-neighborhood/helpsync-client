package com.example.helpsync.blescanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class BLEScanReceiver(
    private val onResult: (Boolean) -> Unit
): BroadcastReceiver() {
    companion object {
        private const val TAG = "BLEScanReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: action=${'$'}{intent.action} package=${'$'}{intent.`package`} extras=${'$'}{intent.extras}")
        val result = intent.getBooleanExtra("result", false)
        val device = intent.getStringExtra("device")
        val rssi = intent.getIntExtra("rssi", Int.MIN_VALUE)
        val msgUtf8 = intent.getStringExtra("msgUtf8")
        val msgHex = intent.getStringExtra("msgHex")
        Log.d(TAG, "onReceive: result=$result device=$device rssi=$rssi msgUtf8=$msgUtf8 msgHex=$msgHex")
        onResult(result)
    }
}