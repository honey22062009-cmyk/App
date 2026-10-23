package com.example.core.backup

import java.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Password-protected backup format. The passphrase itself is never written to disk. */
object BackupCrypto {
 private const val HEADER="STRICT-BLOCKER-ENCRYPTED-BACKUP-V1"
 private const val ITERATIONS=210_000
 private const val KEY_BITS=256
 fun encrypt(plainText:String,passphrase:String):String {
  require(passphrase.length>=8) { "Use a passphrase with at least 8 characters." }
  val salt=ByteArray(16).also { SecureRandom().nextBytes(it) }
  val iv=ByteArray(12).also { SecureRandom().nextBytes(it) }
  val cipher=Cipher.getInstance("AES/GCM/NoPadding")
  cipher.init(Cipher.ENCRYPT_MODE,deriveKey(passphrase,salt),GCMParameterSpec(128,iv))
  cipher.updateAAD(HEADER.toByteArray(Charsets.UTF_8))
  val ciphertext=cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
  return listOf(HEADER,Base64.getEncoder().encodeToString(salt),Base64.getEncoder().encodeToString(iv),Base64.getEncoder().encodeToString(ciphertext)).joinToString("\n")
 }
 fun decrypt(backup:String,passphrase:String):String {
  require(passphrase.length>=8) { "Enter the passphrase used to create this backup." }
  val lines=backup.trim().split("\n")
  require(lines.size==4 && lines[0]==HEADER) { "Unsupported encrypted backup format." }
  val salt=Base64.getDecoder().decode(lines[1])
  val iv=Base64.getDecoder().decode(lines[2])
  val ciphertext=Base64.getDecoder().decode(lines[3])
  require(salt.size==16 && iv.size==12 && ciphertext.size>=16) { "Backup file is incomplete." }
  val cipher=Cipher.getInstance("AES/GCM/NoPadding")
  cipher.init(Cipher.DECRYPT_MODE,deriveKey(passphrase,salt),GCMParameterSpec(128,iv))
  cipher.updateAAD(HEADER.toByteArray(Charsets.UTF_8))
  return try { String(cipher.doFinal(ciphertext),Charsets.UTF_8) } catch (_:Exception) { throw IllegalArgumentException("Incorrect passphrase or damaged backup file.") }
 }
 private fun deriveKey(passphrase:String,salt:ByteArray):SecretKeySpec {
  val spec=PBEKeySpec(passphrase.toCharArray(),salt,ITERATIONS,KEY_BITS)
  return try { SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,"AES") } finally { spec.clearPassword() }
 }
}
