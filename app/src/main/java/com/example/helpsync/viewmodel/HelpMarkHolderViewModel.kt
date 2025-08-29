package com.example.helpsync.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.functions.ktx.functions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Location(
    val latitude: Double,
    val longitude: Double
)
data class Evaluation(
    val rating: Int,
    val comment: String?
)
class HelpMarkHolderViewModel : ViewModel() {

    var requestId = mutableStateOf("")
    fun callCreateHelpRequest(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                val functions = Firebase.functions("asia-northeast2")
                val location = Location(latitude, longitude)
                val data = hashMapOf(
                    "location" to location
                )

                val callResult = functions.getHttpsCallable("createHelpRequest").call(data).await()

                val responseData = callResult.data as? Map<String, Any>
                val status = responseData?.get("status") as? String
                val helpRequestIdResult = responseData?.get("helpRequestId") as? String
                if(status != null) requestId.value = "$helpRequestIdResult"

            } catch(e: Exception){
                requestId.value =  "Error ${e.message}"
            }
        }
    }
    fun callNotifyProximityVerificationResult(scanResult: Boolean) {
        viewModelScope.launch {
            try {
                val functions = Firebase.functions("asis-northeast2")
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
            try {
                val functions = Firebase.functions("asis-northeast2")
                val evaluation = Evaluation(rating, comment)
                val data = hashMapOf(
                    "helpRequestId" to requestId.value,
                    "evaluation" to evaluation
                )
                val callResult = functions.getHttpsCallable("completeHelp").call(data).await()
            } catch(e: Exception) {
                Log.d("Error", "failed to call functions")
            }

        }
    }
}