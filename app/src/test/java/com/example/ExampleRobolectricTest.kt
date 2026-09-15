package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.CoupleCryptoUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TwoGether", appName)
  }

  @Test
  fun `crypto utility encrypts and decrypts correctly with AES-256-GCM`() {
    val secretCoupleCode = "LOVE-4892-PARIS"
    val sampleJsonData = """{"appointments":[{"id":"1","title":"Romantic Dinner","notes":"Secret restaurant reservation"}]}"""

    val encryptedPayload = CoupleCryptoUtil.encrypt(sampleJsonData, secretCoupleCode)
    
    // Ciphertext should not contain the original sensitive strings
    assertNotEquals(sampleJsonData, encryptedPayload.ciphertext)
    assertTrue(!encryptedPayload.ciphertext.contains("Romantic Dinner"))
    assertTrue(!encryptedPayload.ciphertext.contains("Secret restaurant"))
    assertEquals("AES-256-GCM", encryptedPayload.algorithm)

    // Decrypting with correct couple code should yield exact original plaintext
    val decrypted = CoupleCryptoUtil.decrypt(encryptedPayload, secretCoupleCode)
    assertEquals(sampleJsonData, decrypted)
  }

  @Test
  fun `crypto utility rejects wrong key`() {
    val correctCode = "LOVE-4892-PARIS"
    val wrongCode = "HACKER-TRYING-CODE"
    val secretData = "Top secret wedding plan"

    val encryptedPayload = CoupleCryptoUtil.encrypt(secretData, correctCode)

    try {
      CoupleCryptoUtil.decrypt(encryptedPayload, wrongCode)
      fail("Decryption with wrong passphrase must fail with an exception")
    } catch (expected: Exception) {
      // Expected GeneralSecurityException / AEADBadTagException
      assertTrue(true)
    }
  }

  @Test
  fun `crypto utility json envelope roundtrip`() {
    val code = "PAIR-7788"
    val original = "{\"date\":\"2026-09-14\",\"title\":\"Doctor Appointment\"}"
    val jsonEnvelope = CoupleCryptoUtil.encryptToJson(original, code)

    assertTrue(jsonEnvelope.contains("AES-256-GCM"))
    assertTrue(!jsonEnvelope.contains("Doctor Appointment"))

    val restored = CoupleCryptoUtil.decryptFromJson(jsonEnvelope, code)
    assertEquals(original, restored)
  }

  @Test
  fun `partner sync service encrypts cloud backup and restores with E2EE`() = kotlinx.coroutines.runBlocking {
    val syncService = com.example.data.remote.PartnerSyncService()
    val testEmail = "couple@test.com"
    val testBackup = com.example.data.remote.GoogleCloudBackupEnvelope(
      userEmail = testEmail,
      coupleCode = "CODE-1234",
      myName = "Romeo",
      partnerName = "Juliet",
      isPaired = true,
      lastUpdated = 123456789L,
      appointments = listOf(
        com.example.data.remote.RemoteAppointment(
          id = "appt-1",
          title = "Surprise Anniversary Dinner",
          startEpochMillis = 1789800000000L,
          ownerType = "YOU"
        )
      )
    )

    // Export to encrypted JSON
    val encryptedJson = syncService.exportEncryptedBackupJson(testBackup)
    
    // JSON must be encrypted: contains ciphertext and AES-256-GCM, does NOT contain plaintext title or names
    assertTrue(encryptedJson.contains("AES-256-GCM"))
    assertTrue(!encryptedJson.contains("Surprise Anniversary Dinner"))
    assertTrue(!encryptedJson.contains("Romeo"))
    assertTrue(!encryptedJson.contains("Juliet"))

    // Save backup to cloud service
    syncService.saveGoogleCloudBackup(testBackup)

    // Restore backup
    val restored = syncService.restoreGoogleCloudBackup(testEmail).getOrNull()
    org.junit.Assert.assertNotNull(restored)
    assertEquals("Romeo", restored?.myName)
    assertEquals("Juliet", restored?.partnerName)
    assertEquals(1, restored?.appointments?.size)
    assertEquals("Surprise Anniversary Dinner", restored?.appointments?.first()?.title)
  }
}
