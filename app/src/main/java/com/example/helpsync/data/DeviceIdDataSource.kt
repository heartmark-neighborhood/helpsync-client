package com.example.helpsync.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
class DeviceIdDataSource(private val dataStore: DataStore<Preferences>) {

    private val DEVICE_ID_KEY = stringPreferencesKey("device_id")

    val deviceIdFlow = dataStore.data
        .map { preferences ->
            preferences[DEVICE_ID_KEY]
        }

    suspend fun saveDeviceId(deviceId: String) {
        dataStore.edit { preferences ->
            preferences[DEVICE_ID_KEY] = deviceId
        }
    }

    suspend fun getDeviceID(): String? {
        return deviceIdFlow.first()
    }
}