package com.example.helpsync.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DeviceIdDataSource(private val dataStore: DataStore<Preferences>) {

    companion object {
        private const val TAG = "DeviceIdDataSource"
    }

    private val DEVICE_ID_KEY = stringPreferencesKey("device_id")

    val deviceIdFlow = dataStore.data
        .map { preferences ->
            preferences[DEVICE_ID_KEY]
        }

    suspend fun saveDeviceId(deviceId: String?) {
        Log.d(TAG, "saveDeviceId called: deviceId=$deviceId")
        dataStore.edit { preferences ->
            if(deviceId != null) {
                preferences[DEVICE_ID_KEY] = deviceId
                Log.d(TAG, "✅ DeviceId saved to DataStore: $deviceId")
            } else {
                preferences.remove(DEVICE_ID_KEY)
                Log.d(TAG, "✅ DeviceId removed from DataStore")
            }
        }
    }

    suspend fun getDeviceID(): String? {
        val deviceId = deviceIdFlow.first()
        Log.d(TAG, "getDeviceID called: deviceId=$deviceId")
        return deviceId
    }
}