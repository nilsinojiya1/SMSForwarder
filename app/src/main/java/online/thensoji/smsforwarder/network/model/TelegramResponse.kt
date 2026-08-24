package online.thensoji.smsforwarder.network.model

import com.google.gson.annotations.SerializedName

data class TelegramResponse<T>(
    @SerializedName("ok")
    val ok: Boolean,
    @SerializedName("result")
    val result: T? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("error_code")
    val errorCode: Int? = null
)

