package com.example.helpsync.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helpsync.SupporterInfo
import com.example.helpsync.SupporterNavInfo
import com.example.helpsync.repository.CloudMessageRepository
import com.google.firebase.ktx.Firebase
import com.google.firebase.functions.ktx.functions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.net.URLEncoder

class HelpMarkHolderViewModel(
    private val cloudMessageRepository: CloudMessageRepository
) : ViewModel() {
    private val _bleRequestUuid: MutableStateFlow<Map<String, String>?> = MutableStateFlow(null)
    val bleRequestUuid: StateFlow<Map<String, String>?> = _bleRequestUuid

    private val _helpRequestJson: MutableStateFlow<Map<String, String>?> = MutableStateFlow(null)
    val helpRequestJson: StateFlow<Map<String, String>?> = _helpRequestJson

    private val _createdHelpRequestId = MutableStateFlow<String?>(null)
    val createdHelpRequestId: StateFlow<String?> = _createdHelpRequestId

    private val _matchedSupporterNavInfo = MutableStateFlow<com.example.helpsync.SupporterInfo?>(null)
    val matchedSupporterNavInfo: StateFlow<com.example.helpsync.SupporterInfo?> = _matchedSupporterNavInfo


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

    private fun handleFCMData(data: Map<String, String>) {
        Log.d("HelpMarkHolderViewModel", "FCM Data received: $data")
        when (data["type"]) {
            "proximity-verification" -> {
                _bleRequestUuid.value = data
            }
            "help-request" -> {
                _helpRequestJson.value = data

                val rawData = data["data"]
                if (!rawData.isNullOrEmpty()) {
                    try {
                        val json = JSONObject(rawData)
                        if (json.has("candidates")) {
                            val candidates = json.getJSONArray("candidates")
                            if (candidates.length() > 0) {
                                // 1人目の候補者を「マッチング相手」として採用
                                val supporterJson = candidates.getJSONObject(0)

                                // IDの解析 (ログ形式に対応: id: { value: "..." } または id: "..." )
                                val idObj = supporterJson.optJSONObject("id")
                                val supporterId = idObj?.optString("value") ?: supporterJson.optString("id")

                                val nickname = supporterJson.optString("nickname", "サポーター")
                                val iconUrl = supporterJson.optString("iconUrl", "")

                                // 画面遷移用のデータを作成
                                val supporterInfo = SupporterInfo(
                                    id = supporterId,
                                    nickname = nickname,
                                    iconUrl = iconUrl
                                )
                                // リクエストIDは現在保持しているものを使用
                                val currentRequestId = _createdHelpRequestId.value ?: ""
                                val navInfo = SupporterNavInfo(
                                    requestId = currentRequestId,
                                    supporterInfo = supporterInfo
                                )

                                // JSON文字列化してURLエンコード（画面遷移引数用）
                                val infoJson = Json.encodeToString(navInfo)
                                val encodedJson = URLEncoder.encode(infoJson, "UTF-8")

                                Log.d("HelpMarkHolderViewModel", "Candidates found! Object updated.")
                                _matchedSupporterNavInfo.value = supporterInfo
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("HelpMarkHolderViewModel", "JSON parse error", e)
                    }
                }
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

                    _createdHelpRequestId.value = helpRequestIdResult
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
    fun consumeMatchedSupporterInfo() {
        _matchedSupporterNavInfo.value = null
    }

    fun consumeHelpRequestId() {
        _createdHelpRequestId.value = null
    }

}