package com.z22zzw.dailycheckin.data.api.dto

import com.google.gson.annotations.SerializedName

data class DeepSeekResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("choices") val choices: List<Choice>? = null
)

data class Choice(
    @SerializedName("message") val message: ResponseMessage? = null,
    @SerializedName("delta") val delta: StreamDelta? = null
)

data class ResponseMessage(
    @SerializedName("role") val role: String? = null,
    @SerializedName("content") val content: String? = null
)

data class StreamDelta(
    @SerializedName("content") val content: String? = null,
    @SerializedName("role") val role: String? = null
)

data class StreamChunk(
    @SerializedName("choices") val choices: List<Choice>? = null
)
