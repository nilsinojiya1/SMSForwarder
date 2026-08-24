package online.thensoji.smsforwarder.network.model

import com.google.gson.annotations.SerializedName

data class TelegramMessageDto(
    @SerializedName("message_id")
    val messageId: Long,
    @SerializedName("date")
    val date: Long? = null
)

