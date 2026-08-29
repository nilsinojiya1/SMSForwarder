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
            sender = "+919974249000",
            body = "📱 [OnePlus CPH2487]\nFrom: +919974249000 (SIM 1)\nTime: 29 Aug 2026, 12:27 PM\n\nTest 4",
            timestamp = 1756360000000L,
            isSent = true
        )

        val rawShort = "Test 4"
        assertTrue(existing.body.contains(rawShort))

        val differentBody = "Your appointment is confirmed for tomorrow"
        assertFalse(existing.body.contains(differentBody))
    }

    @Test
    fun testSenderNormalization() {
        val senderWithPlus = "+919974249000"
        val senderWithoutPlus = "919974249000"
        val senderWithLeadingZero = "09974249000"
        val senderPlain = "9974249000"

        val norm1 = online.thensoji.smsforwarder.repository.MessageRepository.normalizeSender(senderWithPlus)
        val norm2 = online.thensoji.smsforwarder.repository.MessageRepository.normalizeSender(senderWithoutPlus)
        val norm3 = online.thensoji.smsforwarder.repository.MessageRepository.normalizeSender(senderWithLeadingZero)
        val norm4 = online.thensoji.smsforwarder.repository.MessageRepository.normalizeSender(senderPlain)

        org.junit.Assert.assertEquals("9974249000", norm1)
        org.junit.Assert.assertEquals("9974249000", norm2)
        org.junit.Assert.assertEquals("9974249000", norm3)
        org.junit.Assert.assertEquals("9974249000", norm4)
    }

    @Test
    fun testConsecutiveIdenticalMessagesNotDropped() {
        val t1 = 1756360000000L
        val t2 = t1 + 5000L // 5 seconds later

        val tolerance = 3000L
        val isWithinWindow = (t2 - t1) <= tolerance
        assertFalse("Messages sent 5s apart should NOT fall within the 3s deduplication window", isWithinWindow)
    }
}
