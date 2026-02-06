package com.example.cakecompiler.domain.model

import kotlin.test.Test
import kotlin.test.assertTrue

class ButterflyEffectTest {

    @Test
    fun `trigger returns non-empty notification`() {
        val result = ButterflyEffect.trigger()
        assertTrue(result.notification.isNotBlank())
    }

    @Test
    fun `trigger returns non-empty memory moment`() {
        val result = ButterflyEffect.trigger()
        assertTrue(result.memory.moment.isNotBlank())
    }

    @Test
    fun `trigger returns valid ButterflyMessage multiple times`() {
        repeat(20) {
            val result = ButterflyEffect.trigger()
            assertTrue(result.notification.isNotBlank())
            assertTrue(result.memory.message == result.notification)
        }
    }
}
