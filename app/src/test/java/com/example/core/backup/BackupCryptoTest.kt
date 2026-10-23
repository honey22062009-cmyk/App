package com.example.core.backup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
class BackupCryptoTest {
 @Test fun encryptedBackupRoundTrips() {
  val original="""{"format":"strict-blocker-backup","schemaVersion":1}"""
  val encrypted=BackupCrypto.encrypt(original,"safe-backup-passphrase")
  assertEquals(original,BackupCrypto.decrypt(encrypted,"safe-backup-passphrase"))
 }
 @Test fun encryptedBackupRejectsWrongPassword() {
  val encrypted=BackupCrypto.encrypt("schedule data","right-passphrase")
  assertThrows(IllegalArgumentException::class.java) { BackupCrypto.decrypt(encrypted,"wrong-passphrase") }
 }
}
