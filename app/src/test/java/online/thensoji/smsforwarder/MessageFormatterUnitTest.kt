package online.thensoji.smsforwarder

import online.thensoji.smsforwarder.util.MessageFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageFormatterUnitTest {

    @Test
    fun testFormatCompactNumber() {
        assertEquals("0", MessageFormatter.formatCompactNumber(0))
        assertEquals("999", MessageFormatter.formatCompactNumber(999))
        assertEquals("1k", MessageFormatter.formatCompactNumber(1000))
        assertEquals("1.5k", MessageFormatter.formatCompactNumber(1500))
        assertEquals("50k", MessageFormatter.formatCompactNumber(50000))
        assertEquals("1Lc", MessageFormatter.formatCompactNumber(100000))
        assertEquals("1.5Lc", MessageFormatter.formatCompactNumber(150000))
        assertEquals("1cr", MessageFormatter.formatCompactNumber(10000000))
        assertEquals("2.5cr", MessageFormatter.formatCompactNumber(25000000))
    }

    @Test
    fun testFormatDelayDuration() {
        assertEquals("45s", MessageFormatter.formatDelayDuration(45_000L))
        assertEquals("5m", MessageFormatter.formatDelayDuration(300_000L))
        assertEquals("1h 15m", MessageFormatter.formatDelayDuration(4_500_000L))
        assertEquals("1d 2h", MessageFormatter.formatDelayDuration(93_600_000L))
    }

    @Test
    fun testInjectDelayTag() {
        val original = "📱 [Pixel 7]\nFrom: 123456\nSIM Slot: 1\nTime: 2026-08-27 12:00:00\n\nTest message"
        
        // < 1 minute should not inject delay
        val noDelay = MessageFormatter.injectDelayTag(original, 30_000L)
        assertEquals(original, noDelay)

        // >= 1 minute should inject delay tag
        val delayed = MessageFormatter.injectDelayTag(original, 120_000L)
        assertTrue(delayed.contains("⏳ [Delayed by 2m]"))
        assertTrue(delayed.startsWith("📱 [Pixel 7]\n⏳ [Delayed by 2m]"))
    }

    @Test
    fun testExtractRawBody() {
        val formatted = "📱 [Pixel 7]\nID: #32\nFrom: 123456\nSIM Slot: 1\nTime: 2026-08-27 12:00:00\n\nTest message body"
        val raw = MessageFormatter.extractRawBody(formatted)
        assertEquals("Test message body", raw)

        val unformatted = "Hello World"
        assertEquals("Hello World", MessageFormatter.extractRawBody(unformatted))
    }
}
