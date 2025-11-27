package com.officesuite.app.scanner

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Types of scannable codes
 */
enum class ScanType {
    QR_CODE,
    BARCODE,
    ALL
}

/**
 * Result types from barcode/QR scanning
 */
sealed class ScanResult {
    data class Text(val value: String) : ScanResult()
    data class Url(val url: String, val title: String?) : ScanResult()
    data class Email(val address: String, val subject: String?, val body: String?) : ScanResult()
    data class Phone(val number: String) : ScanResult()
    data class Wifi(val ssid: String, val password: String?, val encryptionType: Int) : ScanResult()
    data class ContactInfo(
        val name: String?,
        val organization: String?,
        val title: String?,
        val phones: List<String>,
        val emails: List<String>,
        val addresses: List<String>,
        val urls: List<String>
    ) : ScanResult()
    data class CalendarEvent(
        val summary: String?,
        val description: String?,
        val location: String?,
        val start: Long?,
        val end: Long?
    ) : ScanResult()
    data class GeoPoint(val latitude: Double, val longitude: Double) : ScanResult()
    data class Product(val productCode: String, val format: String) : ScanResult()
    data class Unknown(val rawValue: String?, val format: Int) : ScanResult()
}

/**
 * Detailed scan result including format and raw data
 */
data class BarcodeScanResult(
    val results: List<ScannedCode>,
    val success: Boolean,
    val error: String? = null
)

data class ScannedCode(
    val rawValue: String?,
    val displayValue: String?,
    val format: Int,
    val formatName: String,
    val valueType: Int,
    val scanResult: ScanResult,
    val boundingBox: android.graphics.Rect?
)

/**
 * Barcode and QR Code Scanner using ML Kit
 */
class BarcodeScanner {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_AZTEC,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_PDF417,
            Barcode.FORMAT_DATA_MATRIX,
            Barcode.FORMAT_ITF
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    /**
     * Scans an image for barcodes and QR codes
     * 
     * @param bitmap The image to scan
     * @return BarcodeScanResult containing all found codes
     */
    suspend fun scan(bitmap: Bitmap): BarcodeScanResult {
        return suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val results = barcodes.map { barcode ->
                        ScannedCode(
                            rawValue = barcode.rawValue,
                            displayValue = barcode.displayValue,
                            format = barcode.format,
                            formatName = getFormatName(barcode.format),
                            valueType = barcode.valueType,
                            scanResult = parseBarcode(barcode),
                            boundingBox = barcode.boundingBox
                        )
                    }

                    continuation.resume(BarcodeScanResult(
                        results = results,
                        success = true
                    ))
                }
                .addOnFailureListener { exception ->
                    continuation.resume(BarcodeScanResult(
                        results = emptyList(),
                        success = false,
                        error = exception.message
                    ))
                }
        }
    }

    private fun parseBarcode(barcode: Barcode): ScanResult {
        return when (barcode.valueType) {
            Barcode.TYPE_URL -> {
                val url = barcode.url
                ScanResult.Url(url?.url ?: barcode.rawValue ?: "", url?.title)
            }
            Barcode.TYPE_EMAIL -> {
                val email = barcode.email
                ScanResult.Email(
                    address = email?.address ?: "",
                    subject = email?.subject,
                    body = email?.body
                )
            }
            Barcode.TYPE_PHONE -> {
                val phone = barcode.phone
                ScanResult.Phone(phone?.number ?: barcode.rawValue ?: "")
            }
            Barcode.TYPE_WIFI -> {
                val wifi = barcode.wifi
                ScanResult.Wifi(
                    ssid = wifi?.ssid ?: "",
                    password = wifi?.password,
                    encryptionType = wifi?.encryptionType ?: 0
                )
            }
            Barcode.TYPE_CONTACT_INFO -> {
                val contact = barcode.contactInfo
                ScanResult.ContactInfo(
                    name = contact?.name?.formattedName,
                    organization = contact?.organization,
                    title = contact?.title,
                    phones = contact?.phones?.mapNotNull { it.number } ?: emptyList(),
                    emails = contact?.emails?.mapNotNull { it.address } ?: emptyList(),
                    addresses = contact?.addresses?.mapNotNull { it.addressLines?.joinToString(", ") } ?: emptyList(),
                    urls = contact?.urls ?: emptyList()
                )
            }
            Barcode.TYPE_CALENDAR_EVENT -> {
                val event = barcode.calendarEvent
                ScanResult.CalendarEvent(
                    summary = event?.summary,
                    description = event?.description,
                    location = event?.location,
                    start = event?.start?.rawValue?.let { parseCalendarDate(it) },
                    end = event?.end?.rawValue?.let { parseCalendarDate(it) }
                )
            }
            Barcode.TYPE_GEO -> {
                val geo = barcode.geoPoint
                ScanResult.GeoPoint(
                    latitude = geo?.lat ?: 0.0,
                    longitude = geo?.lng ?: 0.0
                )
            }
            Barcode.TYPE_PRODUCT -> {
                ScanResult.Product(
                    productCode = barcode.rawValue ?: "",
                    format = getFormatName(barcode.format)
                )
            }
            Barcode.TYPE_TEXT -> {
                ScanResult.Text(barcode.rawValue ?: "")
            }
            else -> {
                ScanResult.Unknown(barcode.rawValue, barcode.format)
            }
        }
    }

    private fun getFormatName(format: Int): String {
        return when (format) {
            Barcode.FORMAT_QR_CODE -> "QR Code"
            Barcode.FORMAT_AZTEC -> "Aztec"
            Barcode.FORMAT_CODE_128 -> "Code 128"
            Barcode.FORMAT_CODE_39 -> "Code 39"
            Barcode.FORMAT_CODE_93 -> "Code 93"
            Barcode.FORMAT_EAN_8 -> "EAN-8"
            Barcode.FORMAT_EAN_13 -> "EAN-13"
            Barcode.FORMAT_UPC_A -> "UPC-A"
            Barcode.FORMAT_UPC_E -> "UPC-E"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
            Barcode.FORMAT_ITF -> "ITF"
            else -> "Unknown"
        }
    }

    private fun parseCalendarDate(dateString: String): Long? {
        return try {
            // Simple parsing for common formats
            val formatter = java.text.SimpleDateFormat("yyyyMMdd'T'HHmmss", java.util.Locale.US)
            formatter.parse(dateString)?.time
        } catch (e: Exception) {
            null
        }
    }

    fun close() {
        scanner.close()
    }
}
