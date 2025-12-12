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
                // 既にデバイスが登録されているかチェック
                val existingDeviceId = try {
                    cloudMessageRepository.getDeviceId()
                } catch (e: Exception) {
                    null
                }
                
                if (existingDeviceId != null) {
                    Log.d("DeviceManagement", "デバイスは既に登録されています: $existingDeviceId")
                    return@launch
                }
                
                val functions = Firebase.functions("asia-northeast2")
                val owner = FirebaseAuth.getInstance().currentUser
                val ownerId = owner?.uid
                
                if (ownerId == null) {
                    Log.e("DeviceManagement", "ユーザーが認証されていません")
                    return@launch
                }
                
                val deviceToken = FirebaseMessaging.getInstance().token.await()
                
                if (deviceToken.isNullOrEmpty()) {
                    Log.e("DeviceManagement", "デバイストークンの取得に失敗しました")
                    return@launch
                }
                
                val locationMap = hashMapOf(
                    "latitude" to latitude,
                    "longitude" to longitude
                )
                val data = hashMapOf(
                    "ownerId" to ownerId,
                    "deviceToken" to deviceToken,
                    "location" to locationMap
                )

                Log.d("DeviceManagement", "デバイス登録リクエスト: ownerId=$ownerId, token=${deviceToken.take(10)}...")
                val callResult = functions.getHttpsCallable("registerNewDevice").call(data).await()

                val responseData = callResult.data as? Map<String, Any>
                val deviceId = responseData?.get("deviceId") as? String

                if (deviceId != null) {
                    cloudMessageRepository.saveDeviceId(deviceId)
                    Log.d("DeviceManagement", "✅ デバイスIDを保存しました: $deviceId")
                } else {
                    Log.e("DeviceManagement", "❌ レスポンスにdeviceIdが含まれていません")
                }
            } catch (e: Exception) {
                Log.e("DeviceManagement", "❌ デバイスの登録に失敗しました", e)
                Log.e("DeviceManagement", "Error details: ${e.message}")
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

    fun callDeleteDevice(onComplete: () -> Unit = {}) {
        Log.d("DeviceManagement", "=== callDeleteDevice called ===")
        viewModelScope.launch {
            Log.d("DeviceManagement", "viewModelScope.launch started")
            // 先にdeviceIdを取得してから削除する
            val deviceId = try {
                cloudMessageRepository.getDeviceId()
            } catch(e: Exception) {
                Log.e("DeviceManagement", "deviceIdの取得に失敗しました", e)
                null
            }
            
            Log.d("DeviceManagement", "Retrieved deviceId: $deviceId")
            
            if (deviceId == null) {
                Log.w("DeviceManagement", "削除するdeviceIdが見つかりません")
                onComplete()
                return@launch
            }
            
            try{
                Log.d("DeviceManagement", "Calling Firebase Functions deleteDevice with deviceId: $deviceId")
                val functions = Firebase.functions("asia-northeast2")
                val data = hashMapOf(
                    "deviceId" to deviceId
                )

                Log.d("DeviceManagement", "Awaiting Cloud Functions response...")
                val callResult = functions.getHttpsCallable("deleteDevice").call(data).await()
                Log.d("DeviceManagement", "✅ Cloud Functions側でデバイスを削除しました")
                Log.d("DeviceManagement", "Response: ${callResult.data}")
            } catch(e: Exception) {
                Log.e("DeviceManagement", "Exception caught in callDeleteDevice", e)
                // デバイスが見つからない場合は既に削除されているので、ワーニングログのみ
                if (e.message?.contains("not found", ignoreCase = true) == true) {
                    Log.w("DeviceManagement", "⚠️ デバイスは既にサーバー側で削除されています: $deviceId")
                } else {
                    Log.e("DeviceManagement", "❌ デバイスの削除に失敗しました", e)
                    Log.e("DeviceManagement", "Error message: ${e.message}")
                }
            } finally {
                Log.d("DeviceManagement", "Entering finally block")
                // エラーの有無に関わらず、ローカルのdeviceIdは削除する
                try {
                    cloudMessageRepository.saveDeviceId(null)
                    Log.d("DeviceManagement", "✅ ローカルのdeviceIdを削除しました")
                } catch (e: Exception) {
                    Log.e("DeviceManagement", "❌ ローカルのdeviceId削除に失敗", e)
                }
                
                Log.d("DeviceManagement", "Calling onComplete callback")
                // 成功・失敗に関わらずコールバックを実行
                onComplete()
                Log.d("DeviceManagement", "=== callDeleteDevice completed ===")
            }
        }
    }
}