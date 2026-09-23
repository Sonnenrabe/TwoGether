package com.example

import com.example.data.model.PartnerLinkRequest
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun partnerLinkRequest_validation() {
    val req = PartnerLinkRequest(
        coupleCode = "TEST-999",
        partnerName = "Sarah",
        partnerDeviceId = "device-b",
        timestamp = 123456789L
    )
    assertEquals("TEST-999", req.coupleCode)
    assertEquals("Sarah", req.partnerName)
    assertEquals("device-b", req.partnerDeviceId)
    assertNotEquals("device-a", req.partnerDeviceId)
  }
}
