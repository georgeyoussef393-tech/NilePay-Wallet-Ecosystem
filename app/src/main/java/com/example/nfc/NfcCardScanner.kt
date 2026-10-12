package com.example.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.example.model.CardType
import java.nio.charset.StandardCharsets
import java.util.Locale

data class EmvScannedCard(
    val pan: String,
    val last4: String,
    val holderName: String,
    val expiryMonth: String,
    val expiryYear: String,
    val cardType: CardType,
    val issuerBank: String,
    val aid: String,
    val apduExchangeLogs: List<String>
)

object NfcCardScanner {

    // Standard Payment AIDs
    val AID_VISA = byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x03, 0x10, 0x10)
    val AID_MASTERCARD = byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x04, 0x10, 0x10)
    val AID_MEEZA = byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x07, 0x27, 0x10, 0x10) // Central Bank of Egypt Meeza AID
    val AID_VISA_ELECTRON = byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x03, 0x20, 0x10)

    // PPSE Name: 2PAY.SYS.DDF01
    private val PPSE_NAME = "2PAY.SYS.DDF01".toByteArray(StandardCharsets.US_ASCII)

    /**
     * Reads a physical Visa, Mastercard, or Meeza contactless card via IsoDep NFC interface.
     */
    fun readEmvCard(tag: Tag): Result<EmvScannedCard> {
        val isoDep = IsoDep.get(tag) ?: return Result.failure(Exception("NFC Tag does not support ISO 14443-4 (IsoDep)"))
        val logs = mutableListOf<String>()

        try {
            isoDep.connect()
            isoDep.timeout = 5000
            logs.add("NFC Connected (Historical Bytes: ${bytesToHex(isoDep.historicalBytes ?: ByteArray(0))})")

            // Step 1: SELECT PPSE (2PAY.SYS.DDF01)
            val selectPpseApdu = createSelectApdu(PPSE_NAME)
            logs.add("CMD -> SELECT PPSE: ${bytesToHex(selectPpseApdu)}")
            val ppseResponse = isoDep.transceive(selectPpseApdu)
            logs.add("RSP <- ${bytesToHex(ppseResponse)}")

            var targetAid: ByteArray? = null

            if (isSuccessStatusWord(ppseResponse)) {
                // Parse AID from PPSE FCI Template (Tag 4F)
                val extractedAid = extractTlvValue(ppseResponse, 0x4F)
                if (extractedAid != null && extractedAid.isNotEmpty()) {
                    targetAid = extractedAid
                    logs.add("Extracted AID from PPSE: ${bytesToHex(extractedAid)}")
                }
            }

            // Fallback list of known Egyptian / Global Payment AIDs if PPSE didn't specify
            val candidateAids = if (targetAid != null) {
                listOf(targetAid)
            } else {
                listOf(AID_VISA, AID_MASTERCARD, AID_MEEZA, AID_VISA_ELECTRON)
            }

            var cardPan: String? = null
            var cardExpiry: String? = null
            var cardholderName = "VALUED CARDHOLDER"
            var matchedAidHex = ""

            for (aid in candidateAids) {
                val selectAidApdu = createSelectApdu(aid)
                logs.add("CMD -> SELECT AID: ${bytesToHex(selectAidApdu)}")
                val aidResponse = isoDep.transceive(selectAidApdu)
                logs.add("RSP <- ${bytesToHex(aidResponse)}")

                if (!isSuccessStatusWord(aidResponse)) {
                    continue
                }

                matchedAidHex = bytesToHex(aid)

                // Step 2: GET PROCESSING OPTIONS (GPO)
                // PDOL empty or 0-length dummy GPO
                val gpoApdu = byteArrayOf(0x80.toByte(), 0xA8.toByte(), 0x00, 0x00, 0x02, 0x83.toByte(), 0x00, 0x00)
                logs.add("CMD -> GET PROCESSING OPTIONS: ${bytesToHex(gpoApdu)}")
                val gpoResponse = isoDep.transceive(gpoApdu)
                logs.add("RSP <- ${bytesToHex(gpoResponse)}")

                // Step 3: READ RECORDS (Iterate SFI 1 to 3, Records 1 to 4)
                for (sfi in 1..3) {
                    for (record in 1..4) {
                        val p2 = ((sfi shl 3) or 0x04).toByte()
                        val readRecordApdu = byteArrayOf(0x00, 0xB2.toByte(), record.toByte(), p2, 0x00)
                        val recordResponse = isoDep.transceive(readRecordApdu)

                        if (isSuccessStatusWord(recordResponse)) {
                            logs.add("RSP <- READ RECORD (SFI $sfi, Rec $record): ${bytesToHex(recordResponse)}")

                            // Check Tag 57 (Track 2 Equivalent Data)
                            val track2 = extractTlvValue(recordResponse, 0x57)
                            if (track2 != null && track2.isNotEmpty()) {
                                val track2Hex = bytesToHex(track2)
                                val parsed = parseTrack2(track2Hex)
                                if (parsed != null) {
                                    cardPan = parsed.first
                                    cardExpiry = parsed.second
                                    logs.add("Discovered Track 2 Data: PAN masked=${maskPan(cardPan)}, Expiry=$cardExpiry")
                                }
                            }

                            // Check Tag 5A (Application PAN)
                            if (cardPan == null) {
                                val panBytes = extractTlvValue(recordResponse, 0x5A)
                                if (panBytes != null && panBytes.isNotEmpty()) {
                                    val panHex = bytesToHex(panBytes).replace("F", "")
                                    cardPan = panHex
                                }
                            }

                            // Check Tag 5F24 (Application Expiration Date - YYMMDD)
                            if (cardExpiry == null) {
                                val expBytes = extractTlvValue(recordResponse, 0x5F24)
                                if (expBytes != null && expBytes.size >= 2) {
                                    val expHex = bytesToHex(expBytes)
                                    cardExpiry = expHex.take(4) // YYMM
                                }
                            }

                            // Check Tag 5F20 (Cardholder Name)
                            val nameBytes = extractTlvValue(recordResponse, 0x5F20)
                            if (nameBytes != null && nameBytes.isNotEmpty()) {
                                val nameStr = String(nameBytes, StandardCharsets.US_ASCII).trim()
                                if (nameStr.isNotBlank()) {
                                    cardholderName = nameStr
                                }
                            }

                            if (cardPan != null) break
                        }
                    }
                    if (cardPan != null) break
                }

                if (cardPan != null) break
            }

            if (cardPan == null) {
                return Result.failure(Exception("Unable to read EMV Track 2 / PAN. Card may require contact chip or alternate authentication."))
            }

            val cleanPan = cardPan.replace("F", "").trim()
            val (expMonth, expYear) = formatExpiry(cardExpiry)
            val cardType = resolveCardType(cleanPan, matchedAidHex)
            val bank = resolveIssuingBank(cleanPan)

            return Result.success(
                EmvScannedCard(
                    pan = cleanPan,
                    last4 = cleanPan.takeLast(4),
                    holderName = cardholderName,
                    expiryMonth = expMonth,
                    expiryYear = expYear,
                    cardType = cardType,
                    issuerBank = bank,
                    aid = matchedAidHex,
                    apduExchangeLogs = logs
                )
            )

        } catch (e: Exception) {
            logs.add("EMV Error: ${e.message}")
            return Result.failure(e)
        } finally {
            try {
                isoDep.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Parses Track 2 Equivalent Data (Tag 57).
     * Format: [PAN] D [YYMM] [Service Code] [Discretionary Data]
     */
    private fun parseTrack2(track2Hex: String): Pair<String, String>? {
        val separatorIndex = track2Hex.indexOfAny(charArrayOf('D', 'd'))
        if (separatorIndex == -1) return null

        val pan = track2Hex.substring(0, separatorIndex)
        val afterSep = track2Hex.substring(separatorIndex + 1)
        val expiry = if (afterSep.length >= 4) afterSep.substring(0, 4) else "2812"

        return Pair(pan, expiry)
    }

    private fun formatExpiry(expiryYyMm: String?): Pair<String, String> {
        if (expiryYyMm == null || expiryYyMm.length < 4) {
            return Pair("12", "28")
        }
        val yy = expiryYyMm.take(2)
        val mm = expiryYyMm.substring(2, 4)
        return Pair(mm, yy)
    }

    fun resolveCardType(pan: String, aidHex: String): CardType {
        return when {
            aidHex.contains("07271010") || pan.startsWith("5078") || pan.startsWith("6080") -> CardType.MEEZA
            pan.startsWith("4") || aidHex.contains("031010") -> CardType.VISA
            pan.startsWith("5") || pan.startsWith("2") || aidHex.contains("041010") -> CardType.MASTERCARD
            else -> CardType.VISA
        }
    }

    fun resolveIssuingBank(pan: String): String {
        return when {
            pan.startsWith("4916") || pan.startsWith("4211") -> "Commercial International Bank (CIB)"
            pan.startsWith("5412") || pan.startsWith("5234") -> "Banque Misr"
            pan.startsWith("5078") || pan.startsWith("4732") -> "National Bank of Egypt (NBE)"
            pan.startsWith("4023") || pan.startsWith("5188") -> "QNB Alahli"
            pan.startsWith("4588") -> "Alexbank (Intesa Sanpaolo)"
            else -> "Egyptian Commercial Bank (CBE Licensed)"
        }
    }

    private fun maskPan(pan: String): String {
        if (pan.length < 8) return pan
        return "${pan.take(4)} •••• •••• ${pan.takeLast(4)}"
    }

    private fun createSelectApdu(nameBytes: ByteArray): ByteArray {
        val apdu = ByteArray(6 + nameBytes.size)
        apdu[0] = 0x00 // CLA
        apdu[1] = 0xA4.toByte() // INS: SELECT
        apdu[2] = 0x04 // P1: Select by name / DF
        apdu[3] = 0x00 // P2: First or only occurrence
        apdu[4] = nameBytes.size.toByte() // Lc
        System.arraycopy(nameBytes, 0, apdu, 5, nameBytes.size)
        apdu[apdu.size - 1] = 0x00 // Le
        return apdu
    }

    private fun isSuccessStatusWord(response: ByteArray): Boolean {
        if (response.size < 2) return false
        val sw1 = response[response.size - 2]
        val sw2 = response[response.size - 1]
        return (sw1 == 0x90.toByte() && sw2 == 0x00.toByte()) || sw1 == 0x61.toByte()
    }

    /**
     * Helper to search for a 1-byte or 2-byte ASN.1 BER-TLV tag in a byte array.
     */
    private fun extractTlvValue(data: ByteArray, targetTag: Int): ByteArray? {
        var i = 0
        while (i < data.size - 2) {
            val tag: Int
            val tagLen: Int
            val firstByte = data[i].toInt() and 0xFF

            if ((firstByte and 0x1F) == 0x1F) {
                // 2-byte tag
                if (i + 1 >= data.size) break
                val secondByte = data[i + 1].toInt() and 0xFF
                tag = (firstByte shl 8) or secondByte
                tagLen = 2
            } else {
                tag = firstByte
                tagLen = 1
            }

            i += tagLen
            if (i >= data.size) break

            var length = data[i].toInt() and 0xFF
            i++

            if (length == 0x81) {
                if (i >= data.size) break
                length = data[i].toInt() and 0xFF
                i++
            } else if (length == 0x82) {
                if (i + 1 >= data.size) break
                length = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
                i += 2
            }

            if (tag == targetTag) {
                if (i + length <= data.size) {
                    val result = ByteArray(length)
                    System.arraycopy(data, i, result, 0, length)
                    return result
                }
            }

            // If it's a constructed template (bit 6 = 1), we don't skip children
            if ((firstByte and 0x20) == 0x20) {
                continue
            } else {
                i += length
            }
        }
        return null
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val result = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val octet = b.toInt() and 0xFF
            result.append(hexChars[octet shr 4])
            result.append(hexChars[octet and 0x0F])
        }
        return result.toString()
    }
}
