package com.officesuite.app.scanner

import org.junit.Assert.*
import org.junit.Test
import java.util.Date

/**
 * Unit tests for scanner data classes and enums.
 */
class ScannerDataClassesTest {

    // ReceiptScanner tests

    @Test
    fun `ExpenseCategory enum has all expected values`() {
        val categories = ExpenseCategory.values()
        
        assertEquals(10, categories.size)
        assertTrue(categories.contains(ExpenseCategory.FOOD_DINING))
        assertTrue(categories.contains(ExpenseCategory.GROCERIES))
        assertTrue(categories.contains(ExpenseCategory.TRANSPORTATION))
        assertTrue(categories.contains(ExpenseCategory.SHOPPING))
        assertTrue(categories.contains(ExpenseCategory.ENTERTAINMENT))
        assertTrue(categories.contains(ExpenseCategory.UTILITIES))
        assertTrue(categories.contains(ExpenseCategory.HEALTHCARE))
        assertTrue(categories.contains(ExpenseCategory.TRAVEL))
        assertTrue(categories.contains(ExpenseCategory.OFFICE_SUPPLIES))
        assertTrue(categories.contains(ExpenseCategory.OTHER))
    }

    @Test
    fun `ReceiptLineItem holds values correctly`() {
        val item = ReceiptLineItem(
            description = "Coffee",
            quantity = 2,
            unitPrice = 3.50,
            totalPrice = 7.00
        )
        
        assertEquals("Coffee", item.description)
        assertEquals(2, item.quantity)
        assertEquals(3.50, item.unitPrice!!, 0.01)
        assertEquals(7.00, item.totalPrice!!, 0.01)
    }

    @Test
    fun `ReceiptLineItem with null optional values`() {
        val item = ReceiptLineItem(
            description = "Misc Item",
            quantity = null,
            unitPrice = null,
            totalPrice = 15.99
        )
        
        assertEquals("Misc Item", item.description)
        assertNull(item.quantity)
        assertNull(item.unitPrice)
        assertEquals(15.99, item.totalPrice!!, 0.01)
    }

    @Test
    fun `ReceiptInfo holds values correctly`() {
        val date = Date()
        val lineItems = listOf(
            ReceiptLineItem("Item 1", 1, 10.0, 10.0),
            ReceiptLineItem("Item 2", 2, 5.0, 10.0)
        )
        
        val info = ReceiptInfo(
            merchantName = "Test Store",
            date = date,
            dateString = "01/15/2024",
            subtotal = 20.0,
            tax = 1.80,
            total = 21.80,
            paymentMethod = "VISA",
            lineItems = lineItems,
            category = ExpenseCategory.SHOPPING,
            currency = "USD",
            rawText = "Full receipt text...",
            confidence = 0.85f
        )
        
        assertEquals("Test Store", info.merchantName)
        assertEquals(date, info.date)
        assertEquals("01/15/2024", info.dateString)
        assertEquals(20.0, info.subtotal!!, 0.01)
        assertEquals(1.80, info.tax!!, 0.01)
        assertEquals(21.80, info.total!!, 0.01)
        assertEquals("VISA", info.paymentMethod)
        assertEquals(2, info.lineItems.size)
        assertEquals(ExpenseCategory.SHOPPING, info.category)
        assertEquals("USD", info.currency)
        assertEquals(0.85f, info.confidence, 0.01f)
    }

    @Test
    fun `ReceiptInfo with minimal data`() {
        val info = ReceiptInfo(
            merchantName = null,
            date = null,
            dateString = null,
            subtotal = null,
            tax = null,
            total = 25.99,
            paymentMethod = null,
            lineItems = emptyList(),
            category = ExpenseCategory.OTHER,
            currency = null,
            rawText = "",
            confidence = 0.0f
        )
        
        assertNull(info.merchantName)
        assertNull(info.date)
        assertEquals(25.99, info.total!!, 0.01)
        assertEquals(ExpenseCategory.OTHER, info.category)
    }

    // IdDocumentScanner tests

    @Test
    fun `IdDocumentType enum has all expected values`() {
        val types = IdDocumentType.values()
        
        assertEquals(4, types.size)
        assertTrue(types.contains(IdDocumentType.PASSPORT))
        assertTrue(types.contains(IdDocumentType.DRIVERS_LICENSE))
        assertTrue(types.contains(IdDocumentType.ID_CARD))
        assertTrue(types.contains(IdDocumentType.UNKNOWN))
    }

    @Test
    fun `IdDocumentInfo holds values correctly`() {
        val dob = Date()
        val exp = Date()
        
        val info = IdDocumentInfo(
            documentType = IdDocumentType.PASSPORT,
            firstName = "John",
            lastName = "Doe",
            fullName = "John Doe",
            dateOfBirth = dob,
            dateOfBirthString = "01/01/1990",
            expirationDate = exp,
            expirationDateString = "12/31/2030",
            documentNumber = "AB1234567",
            nationality = "USA",
            sex = "M",
            address = "123 Main St",
            rawText = "Passport document text...",
            confidence = 0.9f
        )
        
        assertEquals(IdDocumentType.PASSPORT, info.documentType)
        assertEquals("John", info.firstName)
        assertEquals("Doe", info.lastName)
        assertEquals("John Doe", info.fullName)
        assertEquals(dob, info.dateOfBirth)
        assertEquals(exp, info.expirationDate)
        assertEquals("AB1234567", info.documentNumber)
        assertEquals("USA", info.nationality)
        assertEquals("M", info.sex)
        assertEquals("123 Main St", info.address)
        assertEquals(0.9f, info.confidence, 0.01f)
    }

    @Test
    fun `IdDocumentInfo with minimal data`() {
        val info = IdDocumentInfo(
            documentType = IdDocumentType.UNKNOWN,
            firstName = null,
            lastName = null,
            fullName = null,
            dateOfBirth = null,
            dateOfBirthString = null,
            expirationDate = null,
            expirationDateString = null,
            documentNumber = null,
            nationality = null,
            sex = null,
            address = null,
            rawText = "",
            confidence = 0.0f
        )
        
        assertEquals(IdDocumentType.UNKNOWN, info.documentType)
        assertNull(info.firstName)
        assertNull(info.fullName)
        assertEquals(0.0f, info.confidence, 0.01f)
    }

    // BarcodeScanner tests

    @Test
    fun `ScanType enum has all expected values`() {
        val types = ScanType.values()
        
        assertEquals(3, types.size)
        assertTrue(types.contains(ScanType.QR_CODE))
        assertTrue(types.contains(ScanType.BARCODE))
        assertTrue(types.contains(ScanType.ALL))
    }

    @Test
    fun `ScanResult Text holds value correctly`() {
        val result = ScanResult.Text("Hello World")
        
        assertEquals("Hello World", result.value)
    }

    @Test
    fun `ScanResult Url holds values correctly`() {
        val result = ScanResult.Url(
            url = "https://example.com",
            title = "Example Site"
        )
        
        assertEquals("https://example.com", result.url)
        assertEquals("Example Site", result.title)
    }

    @Test
    fun `ScanResult Email holds values correctly`() {
        val result = ScanResult.Email(
            address = "test@example.com",
            subject = "Hello",
            body = "Message body"
        )
        
        assertEquals("test@example.com", result.address)
        assertEquals("Hello", result.subject)
        assertEquals("Message body", result.body)
    }

    @Test
    fun `ScanResult Phone holds value correctly`() {
        val result = ScanResult.Phone(number = "+1234567890")
        
        assertEquals("+1234567890", result.number)
    }

    @Test
    fun `ScanResult Wifi holds values correctly`() {
        val result = ScanResult.Wifi(
            ssid = "MyNetwork",
            password = "secret123",
            encryptionType = 2
        )
        
        assertEquals("MyNetwork", result.ssid)
        assertEquals("secret123", result.password)
        assertEquals(2, result.encryptionType)
    }

    @Test
    fun `ScanResult ContactInfo holds values correctly`() {
        val result = ScanResult.ContactInfo(
            name = "John Doe",
            organization = "Acme Inc",
            title = "CEO",
            phones = listOf("+1234567890", "+0987654321"),
            emails = listOf("john@acme.com"),
            addresses = listOf("123 Main St, City"),
            urls = listOf("https://acme.com")
        )
        
        assertEquals("John Doe", result.name)
        assertEquals("Acme Inc", result.organization)
        assertEquals("CEO", result.title)
        assertEquals(2, result.phones.size)
        assertEquals(1, result.emails.size)
    }

    @Test
    fun `ScanResult CalendarEvent holds values correctly`() {
        val result = ScanResult.CalendarEvent(
            summary = "Team Meeting",
            description = "Weekly sync",
            location = "Conference Room A",
            start = 1700000000000L,
            end = 1700003600000L
        )
        
        assertEquals("Team Meeting", result.summary)
        assertEquals("Weekly sync", result.description)
        assertEquals("Conference Room A", result.location)
        assertEquals(1700000000000L, result.start)
    }

    @Test
    fun `ScanResult GeoPoint holds values correctly`() {
        val result = ScanResult.GeoPoint(
            latitude = 37.7749,
            longitude = -122.4194
        )
        
        assertEquals(37.7749, result.latitude, 0.0001)
        assertEquals(-122.4194, result.longitude, 0.0001)
    }

    @Test
    fun `ScanResult Product holds values correctly`() {
        val result = ScanResult.Product(
            productCode = "123456789012",
            format = "EAN-13"
        )
        
        assertEquals("123456789012", result.productCode)
        assertEquals("EAN-13", result.format)
    }

    @Test
    fun `ScanResult Unknown holds values correctly`() {
        val result = ScanResult.Unknown(
            rawValue = "unknown data",
            format = 999
        )
        
        assertEquals("unknown data", result.rawValue)
        assertEquals(999, result.format)
    }

    @Test
    fun `BarcodeScanResult success state`() {
        val result = BarcodeScanResult(
            results = listOf(),
            success = true,
            error = null
        )
        
        assertTrue(result.success)
        assertNull(result.error)
    }

    @Test
    fun `BarcodeScanResult failure state`() {
        val result = BarcodeScanResult(
            results = emptyList(),
            success = false,
            error = "Failed to scan"
        )
        
        assertFalse(result.success)
        assertEquals("Failed to scan", result.error)
    }

    @Test
    fun `ScannedCode holds values correctly`() {
        val scanResult = ScanResult.Text("Test")
        val code = ScannedCode(
            rawValue = "Test",
            displayValue = "Test Display",
            format = 256,
            formatName = "QR Code",
            valueType = 7,
            scanResult = scanResult,
            boundingBox = null
        )
        
        assertEquals("Test", code.rawValue)
        assertEquals("Test Display", code.displayValue)
        assertEquals(256, code.format)
        assertEquals("QR Code", code.formatName)
        assertEquals(7, code.valueType)
        assertTrue(code.scanResult is ScanResult.Text)
    }

    @Test
    fun `ScanResult with null optional values`() {
        val result = ScanResult.Url(
            url = "https://example.com",
            title = null
        )
        
        assertEquals("https://example.com", result.url)
        assertNull(result.title)
    }

    @Test
    fun `ScanResult Wifi with null password`() {
        val result = ScanResult.Wifi(
            ssid = "OpenNetwork",
            password = null,
            encryptionType = 0
        )
        
        assertEquals("OpenNetwork", result.ssid)
        assertNull(result.password)
        assertEquals(0, result.encryptionType)
    }
}
