package com.officesuite.app.scanner

import android.graphics.Bitmap
import com.officesuite.app.ocr.OcrManager
import com.officesuite.app.ocr.OcrLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * Expense categories for receipt items
 */
enum class ExpenseCategory {
    FOOD_DINING,
    GROCERIES,
    TRANSPORTATION,
    SHOPPING,
    ENTERTAINMENT,
    UTILITIES,
    HEALTHCARE,
    TRAVEL,
    OFFICE_SUPPLIES,
    OTHER
}

/**
 * Individual line item from a receipt
 */
data class ReceiptLineItem(
    val description: String,
    val quantity: Int?,
    val unitPrice: Double?,
    val totalPrice: Double?
)

/**
 * Scanned receipt information
 */
data class ReceiptInfo(
    val merchantName: String?,
    val date: Date?,
    val dateString: String?,
    val subtotal: Double?,
    val tax: Double?,
    val total: Double?,
    val paymentMethod: String?,
    val lineItems: List<ReceiptLineItem>,
    val category: ExpenseCategory,
    val currency: String?,
    val rawText: String,
    val confidence: Float
)

/**
 * Receipt Scanner that extracts expense information from receipt images
 */
class ReceiptScanner(private val ocrManager: OcrManager) {

    // Common date formats on receipts
    private val dateFormats = listOf(
        SimpleDateFormat("MM/dd/yyyy", Locale.US),
        SimpleDateFormat("dd/MM/yyyy", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("MMM dd, yyyy", Locale.US),
        SimpleDateFormat("MM-dd-yyyy", Locale.US),
        SimpleDateFormat("MM/dd/yy", Locale.US),
        SimpleDateFormat("dd-MMM-yyyy", Locale.US)
    )

    // Patterns for extracting information
    private val moneyPattern = Pattern.compile(
        "\\$?\\s?\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?|" +
        "€\\s?\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?|" +
        "£\\s?\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?"
    )
    
    private val datePattern = Pattern.compile(
        "\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4}|" +
        "[A-Za-z]{3}\\s+\\d{1,2},?\\s+\\d{4}"
    )

    // Keywords for identifying totals
    private val totalKeywords = listOf("TOTAL", "GRAND TOTAL", "AMOUNT DUE", "BALANCE DUE", "TOTAL DUE")
    private val subtotalKeywords = listOf("SUBTOTAL", "SUB-TOTAL", "SUB TOTAL", "BEFORE TAX")
    private val taxKeywords = listOf("TAX", "VAT", "GST", "HST", "SALES TAX")

    // Payment method keywords
    private val creditCardKeywords = listOf("VISA", "MASTERCARD", "AMEX", "AMERICAN EXPRESS", "CREDIT", "DEBIT")
    private val cashKeywords = listOf("CASH", "CHANGE")

    // Merchant category keywords for classification
    private val categoryKeywords = mapOf(
        ExpenseCategory.FOOD_DINING to listOf("RESTAURANT", "CAFE", "DINER", "PIZZA", "BURGER", "SUSHI", "COFFEE", "STARBUCKS", "MCDONALD", "SUBWAY"),
        ExpenseCategory.GROCERIES to listOf("GROCERY", "SUPERMARKET", "MARKET", "WALMART", "TARGET", "COSTCO", "KROGER", "SAFEWAY", "WHOLE FOODS"),
        ExpenseCategory.TRANSPORTATION to listOf("GAS", "FUEL", "SHELL", "EXXON", "CHEVRON", "BP", "UBER", "LYFT", "TAXI", "PARKING"),
        ExpenseCategory.SHOPPING to listOf("AMAZON", "EBAY", "STORE", "MALL", "BEST BUY", "NIKE", "ADIDAS"),
        ExpenseCategory.ENTERTAINMENT to listOf("MOVIE", "CINEMA", "THEATER", "NETFLIX", "SPOTIFY", "GAME"),
        ExpenseCategory.UTILITIES to listOf("ELECTRIC", "GAS", "WATER", "INTERNET", "PHONE", "AT&T", "VERIZON"),
        ExpenseCategory.HEALTHCARE to listOf("PHARMACY", "CVS", "WALGREENS", "DOCTOR", "HOSPITAL", "MEDICAL", "DENTAL"),
        ExpenseCategory.TRAVEL to listOf("HOTEL", "AIRLINE", "FLIGHT", "AIRBNB", "MARRIOTT", "HILTON"),
        ExpenseCategory.OFFICE_SUPPLIES to listOf("OFFICE", "STAPLES", "OFFICE DEPOT", "SUPPLIES")
    )

    /**
     * Scans a receipt image and extracts expense information
     * 
     * @param bitmap The receipt image
     * @return ReceiptInfo containing extracted expense details
     */
    suspend fun scan(bitmap: Bitmap): ReceiptInfo = withContext(Dispatchers.Default) {
        // Use Latin OCR for receipts
        ocrManager.setLanguage(OcrLanguage.LATIN)
        
        val ocrResult = ocrManager.extractText(bitmap)
        
        if (!ocrResult.success) {
            return@withContext ReceiptInfo(
                merchantName = null,
                date = null,
                dateString = null,
                subtotal = null,
                tax = null,
                total = null,
                paymentMethod = null,
                lineItems = emptyList(),
                category = ExpenseCategory.OTHER,
                currency = null,
                rawText = "",
                confidence = 0f
            )
        }

        val text = ocrResult.fullText
        val upperText = text.uppercase()
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }

        // Extract fields
        val merchantName = extractMerchantName(lines)
        val dateString = extractDate(text)
        val date = parseDate(dateString)
        val (subtotal, tax, total) = extractAmounts(lines, upperText)
        val paymentMethod = extractPaymentMethod(upperText)
        val lineItems = extractLineItems(lines)
        val category = categorizeExpense(upperText, merchantName)
        val currency = detectCurrency(text)

        // Calculate confidence
        val confidence = calculateConfidence(merchantName, total, date, lineItems)

        ReceiptInfo(
            merchantName = merchantName,
            date = date,
            dateString = dateString,
            subtotal = subtotal,
            tax = tax,
            total = total,
            paymentMethod = paymentMethod,
            lineItems = lineItems,
            category = category,
            currency = currency,
            rawText = text,
            confidence = confidence
        )
    }

    private fun extractMerchantName(lines: List<String>): String? {
        // Merchant name is usually at the top (first 3 lines)
        for (line in lines.take(3)) {
            // Skip lines that look like addresses, dates, or phone numbers
            if (!line.contains(Regex("\\d{3}[-.\\s]?\\d{3}[-.\\s]?\\d{4}")) && // phone
                !line.contains(Regex("\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4}")) && // date
                !line.contains(Regex("\\d+\\s+[A-Za-z]+\\s+(Street|St|Ave|Road|Rd)", RegexOption.IGNORE_CASE)) && // address
                line.length in 3..50 &&
                !line.matches(Regex("\\d+\\.\\d{2}")) // price
            ) {
                return line
            }
        }
        return lines.firstOrNull()
    }

    private fun extractDate(text: String): String? {
        val matcher = datePattern.matcher(text)
        if (matcher.find()) {
            return matcher.group()
        }
        return null
    }

    private fun parseDate(dateString: String?): Date? {
        if (dateString == null) return null
        
        for (format in dateFormats) {
            try {
                format.isLenient = false
                return format.parse(dateString)
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    private fun extractAmounts(lines: List<String>, upperText: String): Triple<Double?, Double?, Double?> {
        var subtotal: Double? = null
        var tax: Double? = null
        var total: Double? = null

        for (line in lines) {
            val upperLine = line.uppercase()
            val amount = extractAmount(line)
            
            when {
                amount != null && totalKeywords.any { upperLine.contains(it) } -> {
                    total = amount
                }
                amount != null && subtotalKeywords.any { upperLine.contains(it) } -> {
                    subtotal = amount
                }
                amount != null && taxKeywords.any { upperLine.contains(it) } -> {
                    tax = amount
                }
            }
        }

        // If we only found total, try to infer from context
        if (total == null) {
            // Look for the largest amount as total
            val allAmounts = lines.mapNotNull { extractAmount(it) }
            total = allAmounts.maxOrNull()
        }

        return Triple(subtotal, tax, total)
    }

    private fun extractAmount(text: String): Double? {
        val matcher = moneyPattern.matcher(text)
        var lastAmount: Double? = null
        
        while (matcher.find()) {
            val amountStr = matcher.group()
                .replace(Regex("[$€£,\\s]"), "")
            try {
                lastAmount = amountStr.toDouble()
            } catch (e: Exception) {
                continue
            }
        }
        
        return lastAmount
    }

    private fun extractPaymentMethod(text: String): String? {
        for (keyword in creditCardKeywords) {
            if (text.contains(keyword)) {
                return keyword
            }
        }
        for (keyword in cashKeywords) {
            if (text.contains(keyword)) {
                return "CASH"
            }
        }
        return null
    }

    private fun extractLineItems(lines: List<String>): List<ReceiptLineItem> {
        val items = mutableListOf<ReceiptLineItem>()
        
        // Pattern: description followed by price at the end
        val itemPattern = Pattern.compile(
            "^(.+?)\\s+(\\d+)?\\s*[xX@]?\\s*(\\d+\\.\\d{2})\\s*$"
        )

        for (line in lines) {
            // Skip lines that are clearly headers or totals
            if (totalKeywords.any { line.uppercase().contains(it) } ||
                subtotalKeywords.any { line.uppercase().contains(it) } ||
                taxKeywords.any { line.uppercase().contains(it) }) {
                continue
            }

            val matcher = itemPattern.matcher(line)
            if (matcher.find()) {
                val description = matcher.group(1)?.trim() ?: continue
                val quantity = matcher.group(2)?.toIntOrNull()
                val price = matcher.group(3)?.toDoubleOrNull()
                
                if (description.isNotBlank() && price != null) {
                    items.add(ReceiptLineItem(
                        description = description,
                        quantity = quantity ?: 1,
                        unitPrice = if (quantity != null && quantity > 0) price / quantity else price,
                        totalPrice = price
                    ))
                }
            }
        }
        
        return items
    }

    private fun categorizeExpense(text: String, merchantName: String?): ExpenseCategory {
        val searchText = "$text ${merchantName ?: ""}".uppercase()
        
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { searchText.contains(it) }) {
                return category
            }
        }
        
        return ExpenseCategory.OTHER
    }

    private fun detectCurrency(text: String): String? {
        return when {
            text.contains("$") -> "USD"
            text.contains("€") -> "EUR"
            text.contains("£") -> "GBP"
            text.contains("¥") -> "JPY"
            else -> null
        }
    }

    private fun calculateConfidence(
        merchantName: String?,
        total: Double?,
        date: Date?,
        lineItems: List<ReceiptLineItem>
    ): Float {
        var score = 0f
        
        if (merchantName != null) score += 0.25f
        if (total != null) score += 0.35f
        if (date != null) score += 0.2f
        if (lineItems.isNotEmpty()) score += 0.2f
        
        return score
    }
}
