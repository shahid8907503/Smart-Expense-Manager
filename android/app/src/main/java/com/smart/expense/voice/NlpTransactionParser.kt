package com.smart.expense.voice

import java.util.Calendar
import java.util.Date
import java.util.regex.Pattern

data class ParsedVoiceTransaction(
    val amount: Double,
    val type: String, // "expense" or "income"
    val category: String,
    val description: String,
    val timestamp: Long,
    val relativeDateLabel: String,
    val isComplete: Boolean,
    val clarificationField: String? = null,
    val rawText: String,
    val speechFeedbackPrompt: String,
    val paymentMethod: String = "Cash", // Cash, UPI, Debit Card, Credit Card, Net Banking
    val paymentType: String = "EXTERNAL" // EXTERNAL or INTERNAL
)

object NlpTransactionParser {

    private val NUMBER_WORDS: Map<String, Double> = mapOf(
        "one" to 1.0, "two" to 2.0, "three" to 3.0, "four" to 4.0, "five" to 5.0,
        "six" to 6.0, "seven" to 7.0, "eight" to 8.0, "nine" to 9.0, "ten" to 10.0,
        "twenty" to 20.0, "thirty" to 30.0, "forty" to 40.0, "fifty" to 50.0,
        "sixty" to 60.0, "seventy" to 70.0, "eighty" to 80.0, "ninety" to 90.0,
        "hundred" to 100.0, "thousand" to 1000.0, "lakh" to 100000.0,
        // Hindi transliterated
        "ek" to 1.0, "do" to 2.0, "teen" to 3.0, "chaar" to 4.0, "paanch" to 5.0,
        "chhe" to 6.0, "saat" to 7.0, "aath" to 8.0, "nau" to 9.0, "dus" to 10.0,
        "bees" to 20.0, "tees" to 30.0, "chalis" to 40.0, "pachas" to 50.0,
        "sau" to 100.0, "hazaar" to 1000.0, "hazar" to 1000.0,
        // Telugu transliterated
        "okati" to 1.0, "rendu" to 2.0, "moodu" to 3.0, "naalugu" to 4.0, "aidu" to 5.0,
        "aaru" to 6.0, "yedu" to 7.0, "enimidi" to 8.0, "tommidi" to 9.0, "padi" to 10.0,
        "iravai" to 20.0, "mupphai" to 30.0, "nalabhai" to 40.0, "yaabhai" to 50.0,
        "vandha" to 100.0, "vandhalu" to 100.0, "velu" to 1000.0, "vela" to 1000.0,
        // Tamil transliterated
        "ondru" to 1.0, "onnu" to 1.0, "moonu" to 3.0, "naalu" to 4.0, "anju" to 5.0,
        "ombadhu" to 9.0, "irubadhu" to 20.0, "muppadhu" to 30.0, "naarpadhu" to 40.0,
        "aimpadhu" to 50.0, "nooru" to 100.0, "aayiram" to 1000.0, "latcham" to 100000.0
    )

    private val INCOME_KEYWORDS = listOf(
        "salary", "income", "received", "got", "earned", "credited", "bonus", "refund",
        "cashback", "mili", "mila", "mile", "aamadni", "vetan", "vachindi", "vachayi",
        "sampadinchanu", "jeetham", "sambalam", "varumaanam", "vanthathu", "kidaithathu",
        "சம்பளம்", "வருமானம்", "வந்தது", "கிடைத்தது"
    )

    fun parse(text: String, language: String = "en"): ParsedVoiceTransaction {
        val trimmed = text.trim()
        val amount = extractAmount(trimmed)
        val type = extractType(trimmed)
        val (timestamp, dateLabel) = extractDate(trimmed)
        val category = if (type == "income") "Salary" else KeywordCategorizer.categorize(trimmed)
        val description = extractCleanDescription(trimmed, amount, category)
        val paymentMethod = extractPaymentMethod(trimmed)
        val paymentType = extractPaymentType(trimmed)

        val isComplete = amount > 0.0
        val clarificationField = if (!isComplete) "amount" else null

        val prompt = when {
            !isComplete -> "Could not detect the amount. Please say how much you spent."
            language == "ta" -> "$dateLabel $category ($paymentMethod வழியாக) ₹${amount.toInt()} பதிவு செய்யட்டுமா?"
            language == "te" -> "$dateLabel $category kosam $paymentMethod dwara ₹${amount.toInt()} record cheyyamantara?"
            language == "hi" -> "Kya main $dateLabel ke liye $category me $paymentMethod dwara ₹${amount.toInt()} save karun?"
            else -> "Recorded ₹${amount.toInt()} via $paymentMethod for $description under $category. Confirm to save."
        }

        return ParsedVoiceTransaction(
            amount = amount,
            type = type,
            category = category,
            description = description,
            timestamp = timestamp,
            relativeDateLabel = dateLabel,
            isComplete = isComplete,
            clarificationField = clarificationField,
            rawText = trimmed,
            speechFeedbackPrompt = prompt,
            paymentMethod = paymentMethod,
            paymentType = paymentType
        )
    }

    private fun extractAmount(text: String): Double {
        // Match numbers preceded/followed by currency or standalone numbers
        val patterns = listOf(
            Pattern.compile("""(?:₹|rs\.?|inr|ரூபாய்|ரூ|roobai|rubai)\s*([\d,]+(?:\.\d+)?)""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""([\d,]+(?:\.\d+)?)\s*(?:₹|rs\.?|inr|rupees|rupee|rupaye|rupayalu|rupai|ரூபாய்|ரூ|roobai|rubai)""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""\b(\d+(?:\.\d+)?)\b""")
        )

        for (pat in patterns) {
            val matcher = pat.matcher(text)
            if (matcher.find()) {
                val numStr = matcher.group(1)?.replace(",", "")
                val parsed = numStr?.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    return parsed
                }
            }
        }

        // Spoken words check
        val tokens = text.lowercase().split("\\s+".toRegex())
        var total = 0.0
        var currentGroup = 0.0
        var found = false

        for (token in tokens) {
            val clean = token.replace("""[^a-z]""".toRegex(), "")
            if (NUMBER_WORDS.containsKey(clean)) {
                found = true
                val v = NUMBER_WORDS[clean] ?: 0.0
                if (v == 100.0) {
                    currentGroup = (if (currentGroup == 0.0) 1.0 else currentGroup) * 100.0
                } else if (v >= 1000.0) {
                    currentGroup = (if (currentGroup == 0.0) 1.0 else currentGroup) * v
                    total += currentGroup
                    currentGroup = 0.0
                } else {
                    currentGroup += v
                }
            }
        }
        total += currentGroup

        return if (found && total > 0.0) total else 0.0
    }

    private fun extractType(text: String): String {
        val lower = text.lowercase()
        for (kw in INCOME_KEYWORDS) {
            if (lower.contains(kw)) {
                return "income"
            }
        }
        return "expense"
    }

    private fun extractDate(text: String): Pair<Long, String> {
        val lower = text.lowercase()
        val cal = Calendar.getInstance()

        return when {
            lower.contains("day before yesterday") || lower.contains("parso") || lower.contains("monna") || lower.contains("munthina naal") || lower.contains("முந்தாநாள்") -> {
                cal.add(Calendar.DAY_OF_YEAR, -2)
                Pair(cal.timeInMillis, "Day before yesterday")
            }
            lower.contains("yesterday") || lower.contains("kal") || lower.contains("ninna") || lower.contains("ninnalo") || lower.contains("netru") || lower.contains("நேற்று") -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                Pair(cal.timeInMillis, "Yesterday")
            }
            else -> {
                Pair(cal.timeInMillis, "Today")
            }
        }
    }

    private fun extractCleanDescription(text: String, amount: Double, category: String): String {
        var clean = text
        if (amount > 0) {
            clean = clean.replace(amount.toInt().toString(), "")
            clean = clean.replace(amount.toString(), "")
        }

        val stopWords = listOf(
            "(?i)\\b(i|have|spent|paid|bought|purchased|received|got|for|on|as|in|at|a|an|the)\\b",
            "(?i)\\b(rupees|rupee|rs|inr|rupaye|rupayalu|rupai|roobai|rubai|today|yesterday|tomorrow)\\b",
            "(?i)\\b(nenu|ninna|eeroju|monna|kharchu|chesanu|ichanu|pettanu|kosam|ki|lo|vachindi)\\b",
            "(?i)\\b(maine|aaj|kal|parso|kharch|kharcha|kiye|kiya|diye|diya|ke liye|par|ko|hua)\\b",
            "(?i)\\b(naan|netru|indru|selavu|seithen|koduthen|vaanginen|kaga|aachu|vanthathu)\\b",
            "ரூபாய்", "ரூ", "செலவு", "செய்தேன்", "வாங்கினேன்", "கொடுத்தேன்", "நேற்று", "இன்று"
        )

        for (pattern in stopWords) {
            clean = clean.replace(pattern.toRegex(), " ")
        }

        clean = clean.replace("""[^\w\s]""".toRegex(), " ").replace("""\s+""".toRegex(), " ").trim()

        return if (clean.isNotBlank()) {
            clean.replaceFirstChar { it.uppercase() }
        } else {
            category
        }
    }

    private fun extractPaymentMethod(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("upi") || lower.contains("gpay") || lower.contains("google pay") ||
            lower.contains("phonepe") || lower.contains("paytm") || lower.contains("scan") || 
            lower.contains("qr") -> "UPI"

            lower.contains("credit card") || lower.contains("credit") -> "Credit Card"
            lower.contains("debit card") || lower.contains("card") || lower.contains("atm") -> "Debit Card"
            lower.contains("net banking") || lower.contains("netbanking") || lower.contains("neft") || lower.contains("imps") -> "Net Banking"
            lower.contains("cash") || lower.contains("nagad") || lower.contains("rokda") || lower.contains("ரோக்கம்") -> "Cash"
            else -> "UPI" // Default payment mode in modern fintech
        }
    }

    private fun extractPaymentType(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("self") || lower.contains("transfer to bank") || lower.contains("to wallet") ||
            lower.contains("to cash") || lower.contains("internal") || lower.contains("from bank to") ||
            lower.contains("between accounts") -> "INTERNAL"
            else -> "EXTERNAL"
        }
    }
}
