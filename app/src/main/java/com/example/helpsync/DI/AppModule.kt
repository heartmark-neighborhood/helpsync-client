package com.example.helpsync.DI

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.helpsync.blescanner.BLEScanWorker
import com.example.helpsync.data.DeviceIdDataSource
import com.example.helpsync.data.HelpRequestIdDataSource
import com.example.helpsync.location_worker.LocationWorker
import com.example.helpsync.repository.CloudMessageRepository
import com.example.helpsync.repository.CloudMessageRepositoryImpl
import com.example.helpsync.viewmodel.DeviceManagementVewModel
import com.example.helpsync.viewmodel.HelpMarkHolderViewModel
import com.example.helpsync.viewmodel.SupporterViewModel
import com.example.helpsync.viewmodel.UserViewModel
import com.example.helpsync.worker.CallCloudFunctionWorker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.qualifier.named
import org.koin.dsl.module

val DEVICE_ID = named("deviceIdStore")
val HELP_REQUEST_ID = named("helpRequestIdStore")

val Context.dataStoreInstance: DataStore<Preferences> by preferencesDataStore("deviceIdStore")
val Context.helpRequestDataStoreInstance: DataStore<Preferences> by preferencesDataStore("helpRequestIdStore")
val appModule = module {

    single<DataStore<Preferences>>(DEVICE_ID) {
        androidContext().dataStoreInstance
    }

    single<DataStore<Preferences>>(HELP_REQUEST_ID) {
        androidContext().helpRequestDataStoreInstance
    }
    single { DeviceIdDataSource(get(DEVICE_ID)) }
    single { HelpRequestIdDataSource(get(HELP_REQUEST_ID))}

    single<CloudMessageRepository> { CloudMessageRepositoryImpl(get(), get()) }


    single {
        HelpMarkHolderViewModel(
            cloudMessageRepository = get()
        )
    }
    factory {
        SupporterViewModel(
            cloudMessageRepository = get()
        )
    }
    factory {
        DeviceManagementVewModel(
            cloudMessageRepository = get()
        )
    }
    single { UserViewModel(cloudMessageRepository = get()) }

    worker {
        LocationWorker(
            context = get(),
            workerParams = get(),
            repository = get()
        )
    }

    worker {
        CallCloudFunctionWorker(get(), get(), get())
    }

    worker {
        BLEScanWorker(get(), get(), get())
    }
}
