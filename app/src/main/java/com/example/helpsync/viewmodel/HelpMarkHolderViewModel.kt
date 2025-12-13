package com.example.helpsync.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helpsync.repository.CloudMessageRepository
import com.google.firebase.ktx.Firebase
import com.google.firebase.functions.ktx.functions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HelpMarkHolderViewModel(
    private val cloudMessageRepository: CloudMessageRepository
) : ViewModel() {
    private val _bleRequestUuid: MutableStateFlow<Map<String, String>?> = MutableStateFlow(null)
    val bleRequestUuid: StateFlow<Map<String, String>?> = _bleRequestUuid

    private val _helpRequestJson: MutableStateFlow<Map<String, String>?> = MutableStateFlow(null)
    val helpRequestJson: StateFlow<Map<String, String>?> = _helpRequestJson

    init {
        viewModelScope.launch {
            cloudMessageRepository.bleRequestMessageFlow
                .collect { data ->
                    handleFCMData(data)
                }
        }

        viewModelScope.launch {
            cloudMessageRepository.helpRequestMessageFlow
                .collect {data ->
                    handleFCMData(data)
                }
        }
    }

    fun handleFCMData(data: Map<String, String>)
    {
        when(data["type"]) {
            "proximity-verification" -> {
                _bleRequestUuid.value = data

            }
            "help-request" -> {
                _helpRequestJson.value = data
            }
        }
    }

    fun callCreateHelpRequest(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            // 1. DeviceIdを安全に取得する
            val deviceId = try {
                cloudMessageRepository.getDeviceId()
            } catch(e: Exception) {
                Log.e("HelpMarkHolderViewModel", "DeviceId取得エラー: ${e.message}")
                null
            }

            // 2. DeviceIdが取れなかったらここで中断する（サーバーに送らない）
            if (deviceId.isNullOrBlank()) {
                Log.e("HelpMarkHolderViewModel", "DeviceIdが無いためリクエストを中止します。")
                // 必要であればここで画面にエラーメッセージを表示する処理を追加
                return@launch
            }

            // 3. DeviceIdがある場合のみ実行
            try {
                val functions = Firebase.functions("asia-northeast2")
                val locationMap = hashMapOf(
                    "latitude" to latitude,
                    "longitude" to longitude
                )
                val data = hashMapOf(
                    "deviceId" to deviceId,
                    "location" to locationMap
                )

                Log.d("HelpMarkHolderViewModel", "Creating help request with deviceId: $deviceId")

                val callResult = functions.getHttpsCallable("createHelpRequest").call(data).await()

                val responseData = callResult.data as? Map<String, Any>
                val status = responseData?.get("status") as? String // statusフィールドがあるかサーバーの戻り値次第ですが
                val helpRequestIdResult = responseData?.get("helpRequestId") as? String

                // 成功したらIDを保存
                if (helpRequestIdResult != null) {
                    cloudMessageRepository.saveHelpRequestId(helpRequestIdResult)
                    Log.d("HelpMarkHolderViewModel", "Success: Request ID $helpRequestIdResult")
                }

            } catch(e: Exception){
                Log.e("HelpMarkHolderViewModel", "createHelpRequestの呼びだしに失敗しました")
                Log.e("HelpMarkHolderViewModel", "エラーメッセージ ${e.message}")
            }
        }
    }

    fun callUpdateDeviceLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val deviceId = try {
                cloudMessageRepository.getDeviceId()
            } catch(e: Exception) {
                Log.d("Error", "deviceIdの取得に失敗しました")
            }
            try {
                val functions = Firebase.functions("asia-northeast2")
                val locationMap = hashMapOf(
                    "latitude" to latitude,
                    "longitude" to longitude
                )
                val data = hashMapOf(
                    "location" to locationMap,
                    "deviceId" to deviceId
                )

                val callResult = functions.getHttpsCallable("updateDeviceLocation").call(data).await()
            } catch(e: Exception) {
                Log.d("Error", "updateDeviceLocationの呼び出しに失敗しました")
                Log.d("Error", "エラーメッセージ: ${e.message}")
            }
        }
    }
    fun callNotifyProximityVerificationResult(scanResult: Boolean) {
        viewModelScope.launch {
            try {
                val functions = Firebase.functions("asia-northeast2")
                val result = if(scanResult) "verified"
                else "failed"
                val data = hashMapOf(
                    "result" to result
                )
                val callResult = functions.getHttpsCallable("notifyProximityVerificationResult").call(data).await()
            } catch(e: Exception) {
                Log.d("Error", "failed to call functions")
            }
        }
    }
    fun callCompleteHelp(rating: Int, comment: String?) {
        viewModelScope.launch {
            val helpRequestId = try {
                cloudMessageRepository.getHelpRequestId()
            } catch (e: Exception){
                Log.d("Error", "HelpRequestIdの取得に失敗しました")
                Log.d("Error", "Error Message:${e.message}")
            }
            try {
                val functions = Firebase.functions("asis-northeast2")
                val evaluationMap = hashMapOf(
                    "rating" to rating,
                    "comment" to comment
                )
                val data = hashMapOf(
                    "helpRequestId" to helpRequestId,
                    "evaluation" to evaluationMap
                )
                val callResult = functions.getHttpsCallable("completeHelp").call(data).await()
            } catch(e: Exception) {
                Log.d("Error", "failed to call functions")
            }

        }
    }
}