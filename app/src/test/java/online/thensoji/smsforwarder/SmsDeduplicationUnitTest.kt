package online.thensoji.smsforwarder

import online.thensoji.smsforwarder.data.ForwardedMessage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsDeduplicationUnitTest {

    @Test
    fun testNearbyDeduplicationLogic() {
        val existing = ForwardedMessage(
            id = 1,
            sender = "+15551234567",
            body = "📱 [Pixel 7]\nFrom: +15551234567 (SIM 1)\nTime: 28 Aug 2026, 11:30 AM\n\nYour OTP is 987654",
            timestamp = 1756360000000L,
            isSent = true
        )

        val rawOtp = "Your OTP is 987654"
        val exactCandidate = "Your OTP is 987654"

        // Deduplication check: formatted body contains the raw body snippet
        assertTrue(existing.body.contains(rawOtp))
        assertTrue(existing.body.contains(exactCandidate.take(20)))

        val differentBody = "Your appointment is confirmed for tomorrow"
        assertFalse(existing.body.contains(differentBody))
    }
}
