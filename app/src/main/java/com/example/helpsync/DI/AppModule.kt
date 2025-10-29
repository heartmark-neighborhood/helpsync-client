package com.example.helpsync.DI

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.helpsync.data.DeviceIdDataSource
import com.example.helpsync.location_worker.LocationWorker
import com.example.helpsync.repository.CloudMessageRepository
import com.example.helpsync.repository.CloudMessageRepositoryImpl
import com.example.helpsync.viewmodel.DeviceManagementVewModel
import com.example.helpsync.viewmodel.HelpMarkHolderViewModel
import com.example.helpsync.viewmodel.SupporterViewModel
import com.example.helpsync.viewmodel.UserViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val Context.dataStoreInstance: DataStore<Preferences> by preferencesDataStore("deviceIdStore")

val appModule = module {

    single<DataStore<Preferences>> {
        androidContext().dataStoreInstance
    }
    single { DeviceIdDataSource(get()) }

    single<CloudMessageRepository> { CloudMessageRepositoryImpl(get()) }


    factory {
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
    factory { UserViewModel() }

    worker {
        LocationWorker(
            context = get(),
            workerParams = get(),
            repository = get()
        )
    }


}
