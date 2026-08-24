package online.thensoji.smsforwarder.network.model

import com.google.gson.annotations.SerializedName

data class SendMessageRequest(
    @SerializedName("chat_id")
    val chatId: String,
    @SerializedName("text")
    val text: String
)

