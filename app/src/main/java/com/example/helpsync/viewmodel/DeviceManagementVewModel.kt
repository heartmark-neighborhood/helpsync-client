package com.example.helpsync.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helpsync.repository.CloudMessageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DeviceManagementVewModel(
    private val cloudMessageRepository: CloudMessageRepository
) : ViewModel(){

    fun callRegisterNewDevice(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                val functions = Firebase.functions("asia-northeast2")
                val owner = FirebaseAuth.getInstance().currentUser
                val ownerId = owner?.uid
                val deviceToken = FirebaseMessaging.getInstance().token.await()
                val locationMap = hashMapOf(
                    "latitude" to latitude,
                    "longitude" to longitude
                )
                val data = hashMapOf(
                    "ownerId" to ownerId,
                    "deviceToken" to deviceToken,
                    "location" to locationMap
                )

                val callResult = functions.getHttpsCallable("registerNewDevice").call(data).await()

                val responseData = callResult.data as? Map<String, Any>
                val deviceId = responseData?.get("deviceId") as? String

                if (deviceId != null) {
                    cloudMessageRepository.saveDeviceId(deviceId)
                    Log.d("DeviceManagement", "デバイスIDを保存しました: $deviceId")
                } else {
                    Log.e("DeviceManagement", "deviceIdがnullです")
                }
            } catch (e: Exception) {
                Log.e("DeviceManagement", "デバイスの登録に失敗しました", e)
            }
        }
    }
    
    suspend fun isDeviceRegistered(): Boolean {
        return try {
            val deviceId = cloudMessageRepository.getDeviceId()
            deviceId != null
        } catch(e: Exception) {
            Log.d("DeviceManagement", "デバイスID確認エラー: ${e.message}")
            false
        }
    }

    fun calldeleteDevice(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val deviceId = try {
                cloudMessageRepository.getDeviceId()
            } catch(e: Exception) {
                Log.d("Error", "deviceIdの取得に失敗しました")
                null
            }
            try{
                val functions = Firebase.functions("asia-northeast2")
                val data = hashMapOf(
                    "deviceId" to deviceId
                )

                val callResult = functions.getHttpsCallable("deleteDevice").call(data).await()
                Log.d("DeviceManagement", "デバイスの削除に成功しました")
            } catch(e: Exception) {
                Log.d("Error", "デバイスの削除に失敗しました")
                Log.d("Error", "Error message: ${e.message}")
            } finally {
                // 成功・失敗に関わらずコールバックを実行
                onComplete()
            }
        }
    }
}