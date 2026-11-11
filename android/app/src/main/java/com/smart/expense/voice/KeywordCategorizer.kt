package com.smart.expense.voice

object KeywordCategorizer {

    val CATEGORIES = listOf(
        "Food",
        "Travel",
        "Shopping",
        "Bills",
        "Health",
        "Education",
        "Entertainment",
        "Rent",
        "Other"
    )

    private val KEYWORDS: Map<String, List<String>> = mapOf(
        "Food" to listOf(
            "lunch", "dinner", "breakfast", "snacks", "snack", "coffee", "tea", "chai",
            "restaurant", "cafe", "swiggy", "zomato", "groceries", "grocery", "vegetables",
            "fruits", "milk", "bread", "egg", "eggs", "chicken", "mutton", "fish", "biryani",
            "pizza", "burger", "tiffin", "hotel", "bhojan", "khana", "khane", "nashta", "rasoi",
            "sabji", "roti", "oota", "thindi", "tiffinlu", "bhojanam", "kura", "pappu", "food",
            "paneer", "ice cream", "juice", "bakery", "sweet", "sweets", "supermarket",
            // Tamil Food Keywords
            "சாப்பாடு", "உணவு", "ஹோட்டல்", "டிபன்", "மதிய உணவு", "இரவு உணவு", "பிரியாணி", "டீ",
            "காபி", "பால்", "முட்டை", "காய்கறி", "மளிகை", "சாப்பிட", "sappadu", "unavu", "tiffin",
            "kaapi", "paal", "muttai", "kaikari", "saapadu"
        ),
        "Travel" to listOf(
            "petrol", "diesel", "fuel", "cng", "auto", "cab", "uber", "ola", "rapido",
            "bus", "train", "metro", "flight", "ticket", "toll", "parking", "fare",
            "taxi", "railway", "irctc", "bike service", "car service", "puncture",
            "mechanic", "travel", "trip", "prayanam", "yatra", "safar", "gaadi",
            "vahanam", "petrolu", "rikshaw",
            // Tamil Travel Keywords
            "ஆட்டோ", "பஸ்", "ரயில்", "பெட்ரோல்", "டீசல்", "பயணம்", "டிக்கெட்", "வாடகை",
            "வண்டி", "விமானம்", "payanam", "vandi", "rail", "ooto"
        ),
        "Shopping" to listOf(
            "shirt", "t-shirt", "pants", "trousers", "jeans", "clothes", "clothing", "dress",
            "shoes", "sandals", "footwear", "watch", "bag", "amazon", "flipkart", "myntra",
            "meesho", "ajio", "mall", "shopping", "electronics", "mobile", "phone", "laptop",
            "headphone", "earbuds", "perfume", "cosmetics", "kapde", "kharidari", "battalu",
            // Tamil Shopping Keywords
            "துணி", "துணிகள்", "ஆடை", "ஷாப்பிங்", "மளிகைப் பொருட்கள்", "கடை", "பொருட்கள்",
            "thuni", "aadaigal", "kadai", "porutkal", "maligai"
        ),
        "Bills" to listOf(
            "electricity", "current", "power", "water", "water bill", "internet", "wifi",
            "broadband", "recharge", "mobile recharge", "airtel", "jio", "vi", "gas",
            "cylinder", "lpg", "indane", "hp", "maintenance", "subscription", "netflix",
            "prime", "spotify", "hotstar", "youtube", "cable", "dth", "bijli", "currentu",
            "bill", "bills", "challan", "tax",
            // Tamil Bills Keywords
            "மின்சாரம்", "கரண்ட் பில்", "மின்சார கட்டணம்", "ரீசார்ஜ்", "பில்", "தண்ணீர் பில்",
            "கேஸ்", "minsaram", "current bill", "recharge", "kattanam"
        ),
        "Health" to listOf(
            "medicine", "medicines", "doctor", "clinic", "hospital", "pharmacy", "medical",
            "chemist", "apollo", "medplus", "tablets", "syrup", "injection", "consultation",
            "checkup", "test", "blood test", "lab", "dentist", "eye checkup", "spectacles",
            "dawa", "dawakhana", "mandulu", "chikitsa", "vaidyam",
            // Tamil Health Keywords
            "மருந்து", "மாத்திரை", "டாக்டர்", "மருத்துவமனை", "மருத்துவ", "பரிசோதனை",
            "marunthu", "maathirai", "maruthuvamanai"
        ),
        "Education" to listOf(
            "book", "books", "tuition", "fee", "fees", "school", "college", "course",
            "udemy", "coursera", "exam", "coaching", "stationery", "pen", "notebook",
            "pustakalu", "kitaben", "padhai", "vidya", "chaduvu",
            // Tamil Education Keywords
            "புத்தகம்", "படிப்பு", "கல்லூரி", "பள்ளி", "கல்விக் கட்டணம்", "டியூஷன்", "பீஸ்",
            "puthagam", "kalluri", "palli", "padippu"
        ),
        "Entertainment" to listOf(
            "movie", "cinema", "theater", "theatre", "pvr", "inox", "concert", "game",
            "gaming", "outing", "party", "club", "pub", "amusement", "picnic", "streaming",
            "vinodam", "manoranjan", "khel", "show",
            // Tamil Entertainment Keywords
            "சினிமா", "படம்", "திரையரங்கம்", "தியேட்டர்", "விளையாட்டு", "சுற்றுலா",
            "padam", "thirayarangam", "vilaiyaattu"
        ),
        "Rent" to listOf(
            "rent", "house rent", "room rent", "flat rent", "apartment rent", "pg rent",
            "hostel fee", "office rent", "kiraya", "vadaka", "bhadige", "rentu",
            // Tamil Rent Keywords
            "வீட்டு வாடகை", "வாடகை", "அறை வாடகை", "veettu vaadagai", "vaadagai"
        )
    )

    fun categorize(text: String): String {
        val lower = text.lowercase()
        for ((category, keywords) in KEYWORDS) {
            for (kw in keywords) {
                if (lower.contains(kw)) {
                    return category
                }
            }
        }
        return "Other"
    }
}
