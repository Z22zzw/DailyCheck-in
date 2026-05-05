package com.z22zzw.dailycheckin.data.api.dto

import com.google.gson.annotations.SerializedName

data class DeepSeekRequest(
    @SerializedName("model") val model: String = "deepseek-chat",
    @SerializedName("messages") val messages: List<Message>,
    @SerializedName("stream") val stream: Boolean = false
)

data class Message(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)
