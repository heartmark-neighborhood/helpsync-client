package com.example.helpsync.viewmodel

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

class HelpRequestManagementViewModel : ViewModel() {

    var requestId = mutableStateOf("")
    fun callCreateHelpRequest(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                val functions = Firebase.functions("asia-northeast2")
                val location = Location(latitude, longitude)
                val data = hashMapOf(
                    "location" to location
                )

                val result = functions.getHttpsCallable("createHelpRequest").call().await()

                val responseData = result.data as? Map<String, Any>
                val status = responseData?.get("status") as? String
                val helpRequestIdResult = responseData?.get("helpRequestId") as? String
                if(status != null) requestId.value = "$helpRequestIdResult"

            } catch(e: Exception){
                requestId.value =  "Error ${e.message}"
            }
        }
    }
}