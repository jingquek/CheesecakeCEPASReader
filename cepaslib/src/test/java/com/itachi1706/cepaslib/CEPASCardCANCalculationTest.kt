package com.itachi1706.cepaslib

import com.itachi1706.cepaslib.base.util.ByteArray
import com.itachi1706.cepaslib.base.util.ByteUtils
import com.itachi1706.cepaslib.card.cepas.CEPASCard
import com.itachi1706.cepaslib.card.cepas.CEPASPurse
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

/**
 * Test for CAN ID calculation in CEPAS Purse 3
 */
class CEPASCardCANCalculationTest {
    
    @Test
    fun testCANCalculation() {
        // Test data from the documentation
        val canBytes = byteArrayOf(0x1C.toByte(), 0x61.toByte(), 0xE9.toByte(), 0x59.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte())
        
        // Test the byteArrayToLong method
        val result = ByteUtils.byteArrayToLong(canBytes)
        
        // Test step-by-step calculation to match the actual implementation
        var total = 0L
        for (i in canBytes.indices) {
            val shift = (canBytes.size - 1 - i) * 8
            val byteValue = (canBytes[i].toLong() and 0x000000FF) shl shift
            total += byteValue
        }
        
        // Verify that our step-by-step calculation matches the byteArrayToLong result
        assertEquals(result, total)
        
        // Print the actual values for reference
        println("CAN bytes: ${ByteUtils.getHexString(canBytes)}")
        println("Result: $result")
        println("Total: $total")
    }
    
    @Test
    fun testCANHexFormat() {
        val canBytes = byteArrayOf(0x1C.toByte(), 0x61.toByte(), 0xE9.toByte(), 0x59.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte())
        val expectedHex = "1C 61 E9 59 00 00 00 00"
        
        val hexBuilder = StringBuilder()
        for (b in canBytes) {
            hexBuilder.append(String.format("%02X ", b))
        }
        val result = hexBuilder.toString().trim()
        
        assertEquals(expectedHex, result)
    }
    
    @Test
    fun testByteArrayToLongAlgorithm() {
        // Test with different byte values to ensure algorithm works correctly
        val testCases = listOf(
            byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte()) to 1L,
            byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte()) to 256L,
            byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte()) to 65536L,
            byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()) to 72057594037927936L
        )
        
        for ((input, expected) in testCases) {
            val result = ByteUtils.byteArrayToLong(input)
            assertEquals("Failed for input ${ByteUtils.getHexString(input)}", expected, result)
        }
    }
}
