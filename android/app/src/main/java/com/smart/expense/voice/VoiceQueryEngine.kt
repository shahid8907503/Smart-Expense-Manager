package com.smart.expense.voice

import com.smart.expense.data.local.BudgetDao
import com.smart.expense.data.local.TransactionDao
import java.util.Calendar

data class VoiceQueryLocalResult(
    val spokenAnswer: String,
    val headline: String,
    val amountFormatted: String,
    val detailSubtitle: String,
    val intent: String
)

class VoiceQueryEngine(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao
) {

    suspend fun executeQuery(
        query: String,
        userId: String,
        language: String = "en"
    ): VoiceQueryLocalResult {
        val text = query.lowercase().trim()

        // 1. Identify date range
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        val (start, end, dateLabel) = when {
            text.contains("today") || text.contains("eeroju") || text.contains("aaj") -> {
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                val s = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
                Triple(s, cal.timeInMillis, "today")
            }
            text.contains("yesterday") || text.contains("ninna") || text.contains("kal") -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                val s = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
                Triple(s, cal.timeInMillis, "yesterday")
            }
            text.contains("last week") || text.contains("pichle hafte") || text.contains("kinda varam") -> {
                cal.add(Calendar.WEEK_OF_YEAR, -1)
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val s = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6)
                Triple(s, cal.timeInMillis, "last week")
            }
            else -> {
                // This month default
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                val s = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
                Triple(s, cal.timeInMillis, "this month")
            }
        }

        val rangeTransactions = transactionDao.getTransactionsInRange(userId, start, end)

        // Intent detection
        return when {
            // Compare spending
            text.contains("compare") || (text.contains("this month") && text.contains("last month")) -> {
                val calThis = Calendar.getInstance()
                calThis.set(Calendar.DAY_OF_MONTH, 1)
                val thisStart = calThis.timeInMillis
                val thisMonthTx = transactionDao.getTransactionsInRange(userId, thisStart, now)
                val thisSpent = thisMonthTx.filter { it.type == "expense" }.sumOf { it.amount }

                val calLast = Calendar.getInstance()
                calLast.add(Calendar.MONTH, -1)
                calLast.set(Calendar.DAY_OF_MONTH, 1)
                val lastStart = calLast.timeInMillis
                calLast.set(Calendar.DAY_OF_MONTH, calLast.getActualMaximum(Calendar.DAY_OF_MONTH))
                val lastEnd = calLast.timeInMillis
                val lastMonthTx = transactionDao.getTransactionsInRange(userId, lastStart, lastEnd)
                val lastSpent = lastMonthTx.filter { it.type == "expense" }.sumOf { it.amount }

                val diff = thisSpent - lastSpent
                val spoken = when (language) {
                    "ta" -> if (diff >= 0) "கடந்த மாதத்தை விட இந்த மாதம் ₹${diff.toInt()} அதிகம் செலவு செய்துள்ளீர்கள்." else "கடந்த மாதத்தை விட இந்த மாதம் ₹${(-diff).toInt()} குறைவாக செலவு செய்துள்ளீர்கள். சிறந்த சேமிப்பு!"
                    "te" -> if (diff >= 0) "Gatha nelatho polisthe ee nela meeru ₹${diff.toInt()} ekkuva kharchu chesaru." else "Gatha nela kante ee nela ₹${(-diff).toInt()} thakkuva kharchu chesaru."
                    "hi" -> if (diff >= 0) "Pichle mahine ki tulna me is mahine aapne ₹${diff.toInt()} zyada kharch kiye." else "Pichle mahine se ₹${(-diff).toInt()} kam kharch huye."
                    else -> if (diff >= 0) "You spent ₹${diff.toInt()} more this month compared to last month." else "You spent ₹${(-diff).toInt()} less this month compared to last month. Great savings!"
                }
                VoiceQueryLocalResult(
                    spokenAnswer = spoken,
                    headline = "Monthly Comparison",
                    amountFormatted = "₹${thisSpent.toInt()} vs ₹${lastSpent.toInt()}",
                    detailSubtitle = if (diff >= 0) "+₹${diff.toInt()} higher this month" else "-₹${(-diff).toInt()} lower this month",
                    intent = "MONTHLY_COMPARISON"
                )
            }

            // Remaining budget
            text.contains("remaining") || text.contains("budget") || text.contains("migilina") || text.contains("மீதம்") || text.contains("meetham") -> {
                val category = KeywordCategorizer.CATEGORIES.firstOrNull { text.contains(it.lowercase()) } ?: "Food"
                val calNow = Calendar.getInstance()
                val month = calNow.get(Calendar.MONTH) + 1
                val year = calNow.get(Calendar.YEAR)

                val budget = budgetDao.getBudgetByCategory(userId, category, month, year)
                val limit = budget?.monthlyLimit ?: 5000.0

                calNow.set(Calendar.DAY_OF_MONTH, 1)
                val monthTx = transactionDao.getTransactionsInRange(userId, calNow.timeInMillis, now)
                val spent = monthTx.filter { it.type == "expense" && it.category.equals(category, ignoreCase = true) }
                    .sumOf { it.amount }
                val remaining = (limit - spent).coerceAtLeast(0.0)

                val spoken = when (language) {
                    "ta" -> "$category பட்ஜெட்டில் இன்னும் ₹${remaining.toInt()} ரூபாய் மீதம் உள்ளது."
                    "te" -> "$category budget lo inka ₹${remaining.toInt()} rupayalu migili unnay."
                    "hi" -> "$category budget me abhi ₹${remaining.toInt()} rupaye bache hain."
                    else -> "You have ₹${remaining.toInt()} remaining in your $category budget."
                }

                VoiceQueryLocalResult(
                    spokenAnswer = spoken,
                    headline = "$category Remaining Budget",
                    amountFormatted = "₹${remaining.toInt()}",
                    detailSubtitle = "Spent ₹${spent.toInt()} of ₹${limit.toInt()} limit",
                    intent = "REMAINING_BUDGET"
                )
            }

            // Highest category
            text.contains("highest") || text.contains("most") || text.contains("ekkuva") || text.contains("sabse") || text.contains("அதிகம்") || text.contains("athigam") -> {
                val catMap = mutableMapOf<String, Double>()
                rangeTransactions.filter { it.type == "expense" }.forEach {
                    catMap[it.category] = (catMap[it.category] ?: 0.0) + it.amount
                }
                val topEntry = catMap.maxByOrNull { it.value }
                val topCat = topEntry?.key ?: "None"
                val topAmt = topEntry?.value ?: 0.0

                val spoken = when (language) {
                    "ta" -> "$dateLabel அதிகபட்சமாக $topCat பிரிவில் ₹${topAmt.toInt()} செலவிடப்பட்டுள்ளது."
                    "te" -> "$dateLabel lo sabse ekkuva $topCat paina ₹${topAmt.toInt()} kharchu chesaru."
                    "hi" -> "$dateLabel me sabse zyada kharch $topCat par ₹${topAmt.toInt()} hua hai."
                    else -> "Your highest spending category $dateLabel is $topCat at ₹${topAmt.toInt()}."
                }

                VoiceQueryLocalResult(
                    spokenAnswer = spoken,
                    headline = "Top Spending: $topCat",
                    amountFormatted = "₹${topAmt.toInt()}",
                    detailSubtitle = "Highest expenditure $dateLabel",
                    intent = "HIGHEST_CATEGORY"
                )
            }

            // Total income
            text.contains("income") || text.contains("salary") || text.contains("aamadni") || text.contains("jeetham") || text.contains("வருமானம்") || text.contains("sambalam") -> {
                val income = rangeTransactions.filter { it.type == "income" }.sumOf { it.amount }
                val spoken = when (language) {
                    "ta" -> "நீங்கள் $dateLabel ₹${income.toInt()} ரூபாய் வருமானம் பெற்றுள்ளீர்கள்."
                    "te" -> "Meeru $dateLabel lo ₹${income.toInt()} rupayala aadhayam pondharu."
                    "hi" -> "Aapko $dateLabel me ₹${income.toInt()} rupaye ki aamadni hui."
                    else -> "You received ₹${income.toInt()} in income $dateLabel."
                }

                VoiceQueryLocalResult(
                    spokenAnswer = spoken,
                    headline = "Total Income ($dateLabel)",
                    amountFormatted = "₹${income.toInt()}",
                    detailSubtitle = "Recorded incoming credits",
                    intent = "TOTAL_INCOME"
                )
            }

            // Specific category spending
            KeywordCategorizer.CATEGORIES.any { text.contains(it.lowercase()) } -> {
                val category = KeywordCategorizer.CATEGORIES.first { text.contains(it.lowercase()) }
                val sum = rangeTransactions.filter {
                    it.type == "expense" && it.category.equals(category, ignoreCase = true)
                }.sumOf { it.amount }

                val spoken = when (language) {
                    "ta" -> "நீங்கள் $dateLabel $category செலவிற்கு ₹${sum.toInt()} ரூபாய் செலவு செய்துள்ளீர்கள்."
                    "te" -> "Meeru $dateLabel lo $category kosam ₹${sum.toInt()} rupayalu kharchu chesaru."
                    "hi" -> "Aapne $dateLabel me $category par ₹${sum.toInt()} rupaye kharch kiye."
                    else -> "You spent ₹${sum.toInt()} on $category $dateLabel."
                }

                VoiceQueryLocalResult(
                    spokenAnswer = spoken,
                    headline = "$category Spending",
                    amountFormatted = "₹${sum.toInt()}",
                    detailSubtitle = "Total expense $dateLabel",
                    intent = "CATEGORY_SPENDING"
                )
            }

            // Default: Total spending
            else -> {
                val sum = rangeTransactions.filter { it.type == "expense" }.sumOf { it.amount }
                val spoken = when (language) {
                    "ta" -> "$dateLabel உங்கள் மொத்த செலவு ₹${sum.toInt()} ரூபாய் ஆகும்."
                    "te" -> "Meeru $dateLabel lo motham ₹${sum.toInt()} rupayalu kharchu chesaru."
                    "hi" -> "Aapka $dateLabel ka kul kharch ₹${sum.toInt()} rupaye hai."
                    else -> "Your total spending $dateLabel is ₹${sum.toInt()}."
                }

                VoiceQueryLocalResult(
                    spokenAnswer = spoken,
                    headline = "Total Spending ($dateLabel)",
                    amountFormatted = "₹${sum.toInt()}",
                    detailSubtitle = "All expenses incurred $dateLabel",
                    intent = "TOTAL_SPENDING"
                )
            }
        }
    }
}

