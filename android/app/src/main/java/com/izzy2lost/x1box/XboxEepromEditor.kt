package com.izzy2lost.x1box

import java.io.File
import java.io.IOException

internal object XboxEepromEditor {
  private const val EEPROM_SIZE = 256

  private const val FACTORY_CHECKSUM_OFFSET = 0x30
  private const val FACTORY_CHECKSUM_START  = 0x34
  private const val FACTORY_CHECKSUM_LENGTH = 0x2C

  private const val USER_CHECKSUM_OFFSET = 0x60
  private const val USER_CHECKSUM_START  = 0x64
  private const val USER_CHECKSUM_LENGTH = 0x5C

  private const val VIDEO_STANDARD_OFFSET = 0x58
  private const val LANGUAGE_OFFSET       = 0x90

  enum class Language(val id: UInt) {
    ENGLISH(0x00000001u),
    JAPANESE(0x00000002u),
    GERMAN(0x00000003u),
    FRENCH(0x00000004u),
    SPANISH(0x00000005u),
    ITALIAN(0x00000006u),
    KOREAN(0x00000007u),
    CHINESE(0x00000008u),
    PORTUGUESE(0x00000009u);

    companion object {
      fun fromId(id: UInt): Language? = entries.firstOrNull { it.id == id }
    }
  }

  enum class VideoStandard(val id: UInt) {
    NTSC_M(0x00400100u),
    NTSC_J(0x00400200u),
    PAL_I(0x00800300u),
    PAL_M(0x00400400u);

    companion object {
      fun fromId(id: UInt): VideoStandard? = entries.firstOrNull { it.id == id }
    }
  }

  data class Snapshot(
    val language: Language,
    val videoStandard: VideoStandard,
    val rawLanguage: UInt,
    val rawVideoStandard: UInt,
  )

  @Throws(IOException::class, IllegalArgumentException::class)
  fun load(file: File): Snapshot {
    val data = file.readBytes()
    ensureValidSize(file, data)

    val rawLanguage = readLeUInt32(data, LANGUAGE_OFFSET)
    val rawVideoStandard = readLeUInt32(data, VIDEO_STANDARD_OFFSET)

    return Snapshot(
      language = Language.fromId(rawLanguage) ?: Language.ENGLISH,
      videoStandard = VideoStandard.fromId(rawVideoStandard) ?: VideoStandard.NTSC_M,
      rawLanguage = rawLanguage,
      rawVideoStandard = rawVideoStandard,
    )
  }

  @Throws(IOException::class, IllegalArgumentException::class)
  fun apply(file: File, language: Language, videoStandard: VideoStandard): Boolean {
    val data = file.readBytes()
    ensureValidSize(file, data)

    val currentLanguage = readLeUInt32(data, LANGUAGE_OFFSET)
    val currentVideoStandard = readLeUInt32(data, VIDEO_STANDARD_OFFSET)
    if (currentLanguage == language.id && currentVideoStandard == videoStandard.id) {
      return false
    }

    writeLeUInt32(data, LANGUAGE_OFFSET, language.id)
    writeLeUInt32(data, VIDEO_STANDARD_OFFSET, videoStandard.id)

    val factoryChecksum = xboxChecksum(data, FACTORY_CHECKSUM_START, FACTORY_CHECKSUM_LENGTH)
    writeLeUInt32(data, FACTORY_CHECKSUM_OFFSET, factoryChecksum)

    val userChecksum = xboxChecksum(data, USER_CHECKSUM_START, USER_CHECKSUM_LENGTH)
    writeLeUInt32(data, USER_CHECKSUM_OFFSET, userChecksum)

    file.writeBytes(data)
    return true
  }

  private fun ensureValidSize(file: File, data: ByteArray) {
    if (data.size != EEPROM_SIZE) {
      throw IllegalArgumentException(
        "Invalid EEPROM size ${data.size}, expected $EEPROM_SIZE (${file.absolutePath})"
      )
    }
  }

  private fun readLeUInt32(data: ByteArray, offset: Int): UInt {
    return ((data[offset].toUInt() and 0xFFu) or
      ((data[offset + 1].toUInt() and 0xFFu) shl 8) or
      ((data[offset + 2].toUInt() and 0xFFu) shl 16) or
      ((data[offset + 3].toUInt() and 0xFFu) shl 24))
  }

  private fun writeLeUInt32(data: ByteArray, offset: Int, value: UInt) {
    data[offset]     = (value and 0xFFu).toByte()
    data[offset + 1] = ((value shr 8) and 0xFFu).toByte()
    data[offset + 2] = ((value shr 16) and 0xFFu).toByte()
    data[offset + 3] = ((value shr 24) and 0xFFu).toByte()
  }

  private fun xboxChecksum(data: ByteArray, offset: Int, length: Int): UInt {
    require(length % 4 == 0) { "Checksum length must be 32-bit aligned" }

    var high = 0u
    var low  = 0u
    var pos  = offset
    val end  = offset + length

    while (pos < end) {
      val value = readLeUInt32(data, pos)
      val sum = (high.toULong() shl 32) or low.toULong()
      val next = sum + value.toULong()

      high = ((next shr 32) and 0xFFFF_FFFFuL).toUInt()
      low += value
      pos += 4
    }

    return (high + low).inv()
  }
}
