package com.example.helpsync.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class HelpRequestIdDataSource(private val dataStore: DataStore<Preferences>) {
    private val HELP_REQUEST_ID_KEY = stringPreferencesKey("help_request_id")

    val helpRequestIdFlow = dataStore.data
        .map { preferences ->
            preferences[HELP_REQUEST_ID_KEY]
        }

    suspend fun saveHelpRequestId(helpRequestId: String?) {
        dataStore.edit { preferences ->
            if(helpRequestId != null) {
                preferences[HELP_REQUEST_ID_KEY] = helpRequestId
            } else {
                preferences.remove(HELP_REQUEST_ID_KEY)
            }
        }
    }

    suspend fun getHelpRequestId(): String? {
        return helpRequestIdFlow.first()
    }
}