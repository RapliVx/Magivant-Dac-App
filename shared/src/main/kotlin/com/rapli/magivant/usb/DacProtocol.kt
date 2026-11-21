package com.rapli.magivant.usb

object DacProtocol {
    const val VENDOR_ID = 0x2FC6
    val productIds = setOf(0xF13A, 0xF13B)

    fun isSupported(vendorId: Int, productId: Int): Boolean =
        vendorId == VENDOR_ID && productId in productIds

    fun createPayload(
        commandId: Byte,
        value1: Byte = 0,
        value2: Byte = 0,
        value3: Byte = 0
    ): ByteArray {
        val payload = byteArrayOf(0xC1.toByte(), 0xA7.toByte(), commandId, value1, value2, value3, 0)
        payload[6] = (payload.take(6).sumOf { it.toInt() } and 0xFF).toByte()
        return payload
    }
}
