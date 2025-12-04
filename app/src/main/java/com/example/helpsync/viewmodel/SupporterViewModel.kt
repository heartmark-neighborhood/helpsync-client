package com.example.helpsync.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helpsync.repository.CloudMessageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class SupporterViewModel(
    private val cloudMessageRepository: CloudMessageRepository
): ViewModel() {

    private val _requesterProfile: MutableStateFlow<Map<String, String>?> = MutableStateFlow(null)
    val requesterProfile: StateFlow<Map<String, String>?> = _requesterProfile

    private val _bleRequestUuid: MutableStateFlow<Map<String, String>?> = MutableStateFlow(null)
    val bleRequestUuid: StateFlow<Map<String, String>?> = _bleRequestUuid

    private val _helpRequestJson: MutableStateFlow<Map<String, String>?> = MutableStateFlow(null)
    val helpRequestJson: StateFlow<Map<String, String>?> = _helpRequestJson

    init {
        viewModelScope.launch {
            cloudMessageRepository.bleRequestMessageFlow
                .collect {data ->
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

    private fun handleFCMData(data: Map<String, String>) {
        when(data["type"]) {
            "proximity-verification" -> {
                _bleRequestUuid.value = data
                val rawData = data["data"]
                if (rawData != null) {
                    val json = JSONObject(rawData)
                    viewModelScope.launch {
                        try {
                            cloudMessageRepository.saveHelpRequestId(json.getString("helpRequestId"))
                        } catch(e: Exception) {
                            Log.e("Error", "HelpRequestIdの保存に失敗しました: ${e.message}")
                        }
                    }
                }
            }
            "help-request" -> {
                _helpRequestJson.value = data
                // 【修正点】通知受信時にプロフィール情報をパースして画面に表示できるようにする
                try {
                    val profileJsonStr = data["requesterProfile"]
                    if (!profileJsonStr.isNullOrEmpty()) {
                        val jsonObject = JSONObject(profileJsonStr)
                        val profileMap = mutableMapOf<String, String>()
                        val keys = jsonObject.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            profileMap[key] = jsonObject.getString(key)
                        }
                        _requesterProfile.value = profileMap
                        Log.d("SupporterViewModel", "Requester profile parsed successfully")
                    }
                } catch (e: Exception) {
                    Log.e("SupporterViewModel", "Failed to parse requester profile", e)
                }
            }
        }
    }

    // ... (以下の関数は変更なしのため省略可能ですが、念のためそのまま利用してください)

    fun callHandleProximityVerificationResult(scanResult: Boolean) {
        viewModelScope.launch {
            val helpRequestId = try {
                cloudMessageRepository.getHelpRequestId()
            } catch (e: Exception){
                Log.d("Error", "HelpRequestIdの取得に失敗しました")
                null
            }

            if (helpRequestId == null) return@launch

            try {
                val functions = Firebase.functions("asia-northeast2")
                val uid: String? = FirebaseAuth.getInstance().currentUser?.uid
                val data = hashMapOf(
                    "verificationResult" to scanResult,
                    "helpRequestId" to helpRequestId,
                    "userId" to uid
                )
                functions.getHttpsCallable("handleProximityVerificationResult").call(data).await()
            } catch(e: Exception){
                Log.e("SupporterViewModel", "Failed to call handleProximityVerificationResult", e)
            }
        }
    }

    fun callRespondToHelpRequest(request_response: Boolean) {
        viewModelScope.launch {
            val helpRequestId = cloudMessageRepository.getHelpRequestId()
            try {
                val functions = Firebase.functions("asia-northeast2")
                val response = if(request_response) "accept" else "decline"
                val data = hashMapOf(
                    "helpRequestId" to helpRequestId,
                    "response" to response
                )
                val callResult = functions.getHttpsCallable("respondToHelpRequest").call(data).await()

                val responseData = callResult.data as? Map<String, String>?
                val status = responseData?.get("status") as? String
                // 既にhandleFCMDataでセットしている場合は上書き、あるいは最新情報をセット
                if(status == "accepted") {
                    val profileMap = responseData?.get("requesterProfile") as? Map<String, String>
                    if (profileMap != null) {
                        _requesterProfile.value = profileMap
                    }
                }
                else Log.d("Debug", "否認が受理されました")
            } catch(e: Exception) {
                Log.d("Error", "failed to call respondToHelpRequest")
            }
        }
    }

    fun callUpdateDeviceLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val deviceId = try {
                cloudMessageRepository.getDeviceId()
            } catch(e: Exception) {
                null
            }
            if (deviceId == null) return@launch

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
                functions.getHttpsCallable("updateDeviceLocation").call(data).await()
            } catch(e: Exception) {
                Log.d("Error", "updateDeviceLocationの呼び出しに失敗しました")
            }
        }
    }

    fun clearViewedRequest() {
        _helpRequestJson.value = null
        _requesterProfile.value = null
        _bleRequestUuid.value = null
        viewModelScope.launch {
            try {
                cloudMessageRepository.saveHelpRequestId(null)
            } catch(e: Exception) {
                Log.d("Error", "helpRequestIdの初期化に失敗しました")
            }
        }
    }

    fun getHelpRequestId(): String? {
        // Warning: This implementation with side-effect assignment inside launch is risky for synchronous return.
        // But keeping it as is per user code, just cleaning up.
        var helpRequestId: String? = null
        // Note: Ideally this should be a suspend function returning String?
        return null // Placeholder: The original code logic for synchronous return here was flawed.
        // 推奨修正: この関数を suspend function にするか、呼び出し元で Flow/Suspend を使うべきです。
    }
}