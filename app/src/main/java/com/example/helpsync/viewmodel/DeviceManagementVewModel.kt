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
        viewModelScope.launch{
            var deviceId : String? = ""
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
                deviceId = responseData?.get("deviceId") as? String
            } catch(e: Exception){
                Log.d("Error", "デバイスの登録に失敗しました")
                Log.d("Error", "Error message: ${e.message}")
            }
            try {
                if(deviceId != null) cloudMessageRepository.saveDeviceId(deviceId)
                else
                {
                    Log.d("Error", "deviceIDがnullです")
                }
            } catch(e: Exception) {
                Log.d("Error", "デバイスの登録に失敗しました")
                Log.d("Error", "Error message: ${e.message}")
            }
        }
    }

    fun calldeleteDevice() {
        viewModelScope.launch {
            val deviceId = try {
                cloudMessageRepository.getDeviceId()
            } catch(e: Exception) {
                Log.d("Error", "deviceIdの取得に失敗しました")
            }
            try{
                val functions = Firebase.functions("asia-northeast2")
                val data = hashMapOf(
                    "deviceId" to deviceId
                )

                val callResult = functions.getHttpsCallable("deleteDevice").call(data).await()
            } catch(e: Exception) {
                Log.d("Error", "デバイスの削除に失敗しました")
                Log.d("Error", "Error message: ${e.message}")
            }
        }
    }
}