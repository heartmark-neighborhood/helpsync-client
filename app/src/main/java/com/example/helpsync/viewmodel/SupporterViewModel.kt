package com.example.helpsync.viewmodel

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helpsync.data.Location
import com.example.helpsync.data.Evaluation
import com.example.helpsync.repository.CloudMessageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.functions.ktx.functions
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

    fun handleFCMData(data: Map<String, String>) {
        when(data["type"]) {
            "proximity-verification" -> {
                _bleRequestUuid.value = data
                val rawData = data["data"]
                val json = JSONObject(rawData)
                viewModelScope.launch {
                    try {
                        cloudMessageRepository.saveHelpRequestId(json.getString("helpRequestId"))
                    } catch(e: Exception) {
                        Log.d("Error", "HelpRequestIdの保存に失敗しました")
                        Log.d("Error", "Error Message: ${e.message}")
                    }
                }

            }
            "help-request" -> {
                _helpRequestJson.value = data
            }
        }
    }
    fun callHandleProximityVerificationResult(scanResult: Boolean) {
        viewModelScope.launch {
            val helpRequestId = try {
                cloudMessageRepository.getHelpRequestId()
            } catch (e: Exception){
                Log.d("Error", "HelpRequestIdの取得に失敗しました")
                Log.d("Error", "Error Message:${e.message}")
            }
            try {
                val functions = Firebase.functions("asia-northeast2")
                val uid: String? = FirebaseAuth.getInstance().currentUser?.uid
                val data = hashMapOf(
                    "verificationResult" to scanResult,
                    "helpRequestId" to helpRequestId,
                    "userId" to uid
                )
                Log.d("SupporterViewModel", "callHandleProximityVerificationResult: scanResult=$scanResult, helpRequestId=${helpRequestId}, userId=$uid")
                Log.d("SupporterViewModel", "Calling Cloud Function: handleProximityVerificationResult with data=$data")
                
                val callResult = functions.getHttpsCallable("handleProximityVerificationResult").call(data).await()
                
                Log.d("SupporterViewModel", "Cloud Function call successful: result=${callResult.data}")
            } catch(e: Exception){
                Log.e("SupporterViewModel", "Failed to call handleProximityVerificationResult", e)
                Log.e("SupporterViewModel", "Error message: ${e.message}")
            }
        }
    }

    fun callRespondToHelpRequest(request_response: Boolean) {
        viewModelScope.launch {
            val helpRequestId = try {
                cloudMessageRepository.getHelpRequestId()
            } catch (e: Exception){
                Log.d("Error", "HelpRequestIdの取得に失敗しました")
                Log.d("Error", "Error Message:${e.message}")
            }
            try {
                val functions = Firebase.functions("asia-northeast2")
                val response = if(request_response) "accept"
                else "decline"
                val data = hashMapOf(
                    "helpRequestId" to helpRequestId,
                    "response" to response
                )
                val callResult = functions.getHttpsCallable("respondToHelpRequest").call(data).await()

                val responseData = callResult.data as? Map<String, String>?
                val status = responseData?.get("status") as? String
                if(status == "accepted")_requesterProfile.value = responseData?.get("requesterProfile") as? Map<String, String>
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

    fun clearViewedRequest() { // 既存のものを流用 or 新設
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
        Log.d("SupporterViewModel", "Cleared viewed request data.")
    }

    fun getHelpRequestId(): String? {
        var helpRequestId: String? = null
        viewModelScope.launch {
            helpRequestId = try {
                cloudMessageRepository.getHelpRequestId()
            } catch (e: Exception) {
                Log.d("Error", "helpRequestIdの取得に失敗しました")
                null
            }
        }
        return helpRequestId
    }
}