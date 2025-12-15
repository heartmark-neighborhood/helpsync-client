package com.example.helpsync

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import com.example.helpsync.DI.appModule
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
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
            // デバッグビルドではDebugAppCheckProviderFactoryを使用
            // リリースビルドではPlayIntegrityAppCheckProviderFactoryを使用
            try {
                if (BuildConfig.DEBUG) {
                    Log.d("AppCheckDebug", "DEBUG build detected. Installing DebugAppCheckProviderFactory.")
                    // リフレクションでDebugAppCheckProviderFactoryを取得（リリースビルドでは利用不可）
                    val debugFactoryClass = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                    val getInstance = debugFactoryClass.getMethod("getInstance")
                    val debugFactory = getInstance.invoke(null)
                    firebaseAppCheck.installAppCheckProviderFactory(debugFactory as com.google.firebase.appcheck.AppCheckProviderFactory)
                } else {
                    Log.d("AppCheckDebug", "RELEASE build detected. Installing PlayIntegrityAppCheckProviderFactory.")
                    firebaseAppCheck.installAppCheckProviderFactory(
                        PlayIntegrityAppCheckProviderFactory.getInstance()
                    )
                }
            } catch (e: Exception) {
                Log.e("AppCheckDebug", "Failed to setup AppCheck: ${e.message}. Falling back to PlayIntegrity.")
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "WORK_CHANNEL_ID",
                    "Background Sync",
                    NotificationManager.IMPORTANCE_LOW
                )
                getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            }
    
            startKoin {
                androidLogger()
                androidContext(this@CustomApplication)
                modules(appModule)
                workManagerFactory()
            }
        }

}