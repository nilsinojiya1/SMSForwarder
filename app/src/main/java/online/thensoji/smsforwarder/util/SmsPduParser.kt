package online.thensoji.smsforwarder.util

import android.telephony.SmsMessage
import android.util.Log

data class SmsConcatInfo(
    val refNumber: Int,
    val totalParts: Int,
    val partIndex: Int
)

object SmsPduParser {
    private const val TAG = "SmsPduParser"

    fun getConcatInfo(sms: SmsMessage, rawPdu: ByteArray?): SmsConcatInfo? {
        // 1. Try from sms.userData if available
        try {
            val userData = sms.userData
            if (userData != null && userData.isNotEmpty()) {
                val info = parseUdh(userData)
                if (info != null) return info
            }
        } catch (e: Exception) {
            Log.d(TAG, "Could not extract UDH from userData: ${e.message}")
        }

        // 2. Try scanning the raw PDU
        val pdu = rawPdu ?: try {
            sms.pdu
        } catch (e: Exception) {
            null
        }

        if (pdu != null && pdu.isNotEmpty()) {
            return scanPduForConcatHeader(pdu)
        }

        return null
    }

    private fun parseUdh(userData: ByteArray): SmsConcatInfo? {
        val udhl = userData[0].toInt() and 0xFF
        if (udhl <= 0 || udhl >= userData.size) return null

        var i = 1
        while (i < udhl + 1 && i < userData.size) {
            val iei = userData[i].toInt() and 0xFF
            val iedl = if (i + 1 < userData.size) userData[i + 1].toInt() and 0xFF else 0
            if (iei == 0x00 && iedl == 3 && i + 4 < userData.size) {
                val refNumber = userData[i + 2].toInt() and 0xFF
                val totalParts = userData[i + 3].toInt() and 0xFF
                val partIndex = userData[i + 4].toInt() and 0xFF
                if (totalParts in 2..255 && partIndex in 1..totalParts) {
                    return SmsConcatInfo(refNumber, totalParts, partIndex)
                }
            } else if (iei == 0x08 && iedl == 4 && i + 5 < userData.size) {
                val refNumber = ((userData[i + 2].toInt() and 0xFF) shl 8) or (userData[i + 3].toInt() and 0xFF)
                val totalParts = userData[i + 4].toInt() and 0xFF
                val partIndex = userData[i + 5].toInt() and 0xFF
                if (totalParts in 2..255 && partIndex in 1..totalParts) {
                    return SmsConcatInfo(refNumber, totalParts, partIndex)
                }
            }
            i += 2 + iedl
        }
        return null
    }

    private fun scanPduForConcatHeader(pdu: ByteArray): SmsConcatInfo? {
        for (i in 0 until pdu.size - 4) {
            val iei = pdu[i].toInt() and 0xFF
            val iedl = pdu[i + 1].toInt() and 0xFF

            // 8-bit concat: IEI = 0x00, IEDL = 3
            if (iei == 0x00 && iedl == 3 && i + 4 < pdu.size) {
                val ref = pdu[i + 2].toInt() and 0xFF
                val total = pdu[i + 3].toInt() and 0xFF
                val seq = pdu[i + 4].toInt() and 0xFF
                if (total in 2..255 && seq in 1..total) {
                    return SmsConcatInfo(ref, total, seq)
                }
            }
            // 16-bit concat: IEI = 0x08, IEDL = 4
            if (iei == 0x08 && iedl == 4 && i + 5 < pdu.size) {
                val ref = ((pdu[i + 2].toInt() and 0xFF) shl 8) or (pdu[i + 3].toInt() and 0xFF)
                val total = pdu[i + 4].toInt() and 0xFF
                val seq = pdu[i + 5].toInt() and 0xFF
                if (total in 2..255 && seq in 1..total) {
                    return SmsConcatInfo(ref, total, seq)
                }
            }
        }
        return null
    }
}

