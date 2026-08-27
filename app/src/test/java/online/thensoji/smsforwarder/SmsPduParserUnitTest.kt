package online.thensoji.smsforwarder

import online.thensoji.smsforwarder.util.SmsConcatInfo
import online.thensoji.smsforwarder.util.SmsPduParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SmsPduParserUnitTest {

    @Test
    fun test8BitConcatHeaderScanning() {
        // Construct simulated 8-bit concat PDU containing IEI=0x00, IEDL=0x03, ref=42, total=3, seq=2
        val pdu = byteArrayOf(
            0x07, 0x91.toByte(), 0x12, 0x34, // header bytes
            0x00, 0x03, 42, 3, 2,           // 8-bit concat UDH
            0x41, 0x42, 0x43                 // payload
        )

        // Reflection or direct parsing verification
        val method = SmsPduParser::class.java.getDeclaredMethod("scanPduForConcatHeader", ByteArray::class.java)
        method.isAccessible = true
        val result = method.invoke(SmsPduParser, pdu) as? SmsConcatInfo

        assertNotNull(result)
        assertEquals(42, result?.refNumber)
        assertEquals(3, result?.totalParts)
        assertEquals(2, result?.partIndex)
    }

    @Test
    fun test16BitConcatHeaderScanning() {
        // Construct simulated 16-bit concat PDU containing IEI=0x08, IEDL=0x04, ref=0x0102 (258), total=4, seq=3
        val pdu = byteArrayOf(
            0x07, 0x91.toByte(), 0x12, 0x34, // header bytes
            0x08, 0x04, 0x01, 0x02, 4, 3,   // 16-bit concat UDH
            0x41, 0x42, 0x43                 // payload
        )

        val method = SmsPduParser::class.java.getDeclaredMethod("scanPduForConcatHeader", ByteArray::class.java)
        method.isAccessible = true
        val result = method.invoke(SmsPduParser, pdu) as? SmsConcatInfo

        assertNotNull(result)
        assertEquals(258, result?.refNumber)
        assertEquals(4, result?.totalParts)
        assertEquals(3, result?.partIndex)
    }
}
