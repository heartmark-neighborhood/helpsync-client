package com.example.helpsync.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.functions.ktx.functions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Location(
    val latitude: Double,
    val longtitude: Double
)

data class Evaluation(
    val rating: Int,
    val comment: Double
)

class SupporterViewModel(

): ViewModel() {
    var helpRequestId = mutableStateOf("")

    private val _requesterProfile: MutableStateFlow<Map<String, String>?> = MutableStateFlow(null)
    val requesterProfile: StateFlow<Map<String, String>?> = _requesterProfile

    fun callNotifyProximityVerificationResult(scanResult: Boolean) {
        viewModelScope.launch {
            try {
                val functions = Firebase.functions("asia-northeast2")
                val result = if (scanResult) "verified"
                else "failed"
                val data = hashMapOf(
                    "result" to result
                )
                val callResult =
                    functions.getHttpsCallable("notifyProximityVerificationResult").call(data)
                        .await()
            } catch(e: Exception){
                Log.d("Error", "failed to call notifyProximityVerificationResult")
            }

        }
    }

    fun callRespondToHelpRequest(request_response: Boolean) {
        viewModelScope.launch {
            try {
                val functions = Firebase.functions("asia-northeast2")
                val response = if(request_response) "accept"
                else "decline"
                val data = hashMapOf(
                    "helpRequestId" to helpRequestId.value,
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
}