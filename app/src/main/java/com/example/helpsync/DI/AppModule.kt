package com.example.helpsync.DI

import androidx.datastore.core.DataStore
import android.content.Context
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.helpsync.repository.CloudMessageRepository
import com.example.helpsync.data.DeviceIdDataSource
import org.koin.dsl.module
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.android.ext.koin.androidContext
import com.example.helpsync.viewmodel.HelpMarkHolderViewModel
import com.example.helpsync.viewmodel.UserViewModel

val Context.dataStoreInstance: DataStore<Preferences> by preferencesDataStore("deviceIdStore")

val appModule = module{

    single<DataStore<Preferences>> {
        androidContext().dataStoreInstance
    }
    single { DeviceIdDataSource(get())}

    single<CloudMessageRepository> { CloudMessageRepositoryImpl(get()) }

    viewModel { HelpMarkHolderViewModel(get()) }
    viewModel { UserViewModel() }
}