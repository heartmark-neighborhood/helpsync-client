package com.example.helpsync.blescanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class BLEScanReceiver(
    private val onResult: (Boolean) -> Unit
): BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = intent.getBooleanExtra("result", false)
        onResult(result)
    }
}