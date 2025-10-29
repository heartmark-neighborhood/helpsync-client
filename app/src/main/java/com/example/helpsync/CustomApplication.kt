package com.example.helpsync

import android.app.Application
import android.util.Log
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import com.example.helpsync.DI.appModule
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory

class CustomApplication : Application() {
        override fun onCreate() {
            super.onCreate()
    
            Log.d("AppCheckDebug", "CustomApplication.onCreate() called")
            FirebaseApp.initializeApp(this)
            val firebaseAppCheck = FirebaseAppCheck.getInstance()

            Log.d("a", "${BuildConfig.DEBUG}")
            if (BuildConfig.DEBUG) {
                Log.d("AppCheckDebug", "DEBUG build detected. Installing DebugAppCheckProviderFactory.")
                // デバッグビルドの場合
                firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            } else {
                Log.d("AppCheckDebug", "RELEASE build detected. Installing PlayIntegrityAppCheckProviderFactory.")
                // リリースビルドの場合
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }
    
            startKoin {
                androidLogger()
                androidContext(this@CustomApplication)
                modules(appModule)
                workManagerFactory()
            }
        }
}