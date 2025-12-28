package com.harmonixia.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
sealed class WebSocketMessage {

    @Serializable
    @SerialName("request")
    data class RequestMessage(
        @SerialName("message_id")
        val messageId: Int,
        val command: String,
        val params: JsonElement? = null
    ) : WebSocketMessage()

    @Serializable
    @SerialName("response")
    data class ResponseMessage(
        @SerialName("message_id")
        val messageId: Int,
        val result: JsonElement? = null,
        val error: JsonElement? = null
    ) : WebSocketMessage()

    @Serializable
    @SerialName("event")
    data class EventMessage(
        val event: String,
        val data: JsonElement? = null
    ) : WebSocketMessage()
}
