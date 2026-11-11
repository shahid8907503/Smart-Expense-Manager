package com.smart.expense.data.remote.dto

data class VoiceParseRequest(
    val text: String,
    val language: String = "en",
    val customCategories: List<String> = emptyList()
)

data class VoiceParseResponse(
    val success: Boolean,
    val isComplete: Boolean,
    val clarificationNeeded: String? = null,
    val message: String? = null,
    val data: ExtractedTransactionData? = null,
    val suggestedSpeechResponse: String? = null
)

data class ExtractedTransactionData(
    val amount: Double,
    val type: String,
    val category: String,
    val description: String,
    val transactionDate: String,
    val dateLabel: String = "Today",
    val rawVoiceText: String,
    val source: String = "voice",
    val confidence: Double = 0.95
)

data class VoiceQueryRequest(
    val query: String,
    val language: String = "en"
)

data class VoiceQueryResponse(
    val success: Boolean,
    val query: String,
    val spokenAnswer: String,
    val data: VoiceQueryResultData
)

data class VoiceQueryResultData(
    val intent: String,
    val dateRange: String,
    val category: String? = null,
    val amount: Double = 0.0,
    val dateRangeLabel: String? = null,
    val highestCategory: String? = null,
    val remainingBudget: Double? = null,
    val compareDiff: Double? = null
)

