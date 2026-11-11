const { categorizeText } = require('./categorizer');

// Number word dictionaries for English, Hindi, Telugu transliterated
const NUMBER_WORDS = {
  // English
  'one': 1, 'two': 2, 'three': 3, 'four': 4, 'five': 5,
  'six': 6, 'seven': 7, 'eight': 8, 'nine': 9, 'ten': 10,
  'twenty': 20, 'thirty': 30, 'forty': 40, 'fifty': 50,
  'sixty': 60, 'seventy': 70, 'eighty': 80, 'ninety': 90,
  'hundred': 100, 'thousand': 1000, 'lakh': 100000,
  // Hindi transliterated & native
  'ek': 1, 'do': 2, 'teen': 3, 'chaar': 4, 'paanch': 5,
  'chhe': 6, 'saat': 7, 'aath': 8, 'nau': 9, 'dus': 10,
  'bees': 20, 'tees': 30, 'chalis': 40, 'pachas': 50,
  'saath': 60, 'sattar': 70, 'assi': 80, 'nabbe': 90,
  'sau': 100, 'hazaar': 1000, 'hazar': 1000,
  'एक': 1, 'दो': 2, 'तीन': 3, 'चार': 4, 'पांच': 5,
  'सौ': 100, 'हजार': 1000, 'हज़ार': 1000,
  // Telugu transliterated & native
  'okati': 1, 'rendu': 2, 'moodu': 3, 'naalugu': 4, 'aidu': 5,
  'aaru': 6, 'yedu': 7, 'enimidi': 8, 'tommidi': 9, 'padi': 10,
  'iravai': 20, 'mupphai': 30, 'nalabhai': 40, 'yaabhai': 50,
  'aravai': 60, 'debbhai': 70, 'yenabhai': 80, 'tombhai': 90,
  'vandha': 100, 'vandhalu': 100, 'velu': 1000, 'vela': 1000,
  'ఒకటి': 1, 'రెండు': 2, 'మూడు': 3, 'నాలుగు': 4, 'ఐదు': 5,
  'వంద': 100, 'వేలు': 1000
};

// Income keywords across languages
const INCOME_KEYWORDS = [
  'salary', 'income', 'received', 'got', 'earned', 'credited', 'bonus', 'refund',
  'cashback', 'interest', 'dividend', 'profit', 'freelance', 'stipend',
  // Hindi
  'mili', 'mila', 'mile', 'aamadni', 'vetan', 'kamaya', 'aaye', 'jama hue',
  'मिले', 'मिला', 'वेतन', 'कमाई', 'आमदनी',
  // Telugu
  'vachindi', 'vachayi', 'vachindhi', 'sampadinchanu', 'andindi', 'jeetham',
  'వచ్చింది', 'వచ్చాయి', 'జీతం'
];

// Expense indicators
const EXPENSE_KEYWORDS = [
  'spent', 'spend', 'paid', 'pay', 'bought', 'buy', 'purchase', 'purchased',
  'cost', 'debited', 'gave', 'given',
  // Hindi
  'kharch', 'kharcha', 'kharchi', 'kharch kiye', 'diya', 'diye', 'lagaya',
  'lagi', 'kharida', 'kharidi', 'ख़र्च', 'खर्च', 'दिए', 'दिया', 'खरीदा',
  // Telugu
  'kharchu', 'kharchu chesanu', 'ichanu', 'pettanu', 'konnanu', 'karchu',
  'చెల్లించాను', 'ఖర్చు', 'కొన్నాను'
];

/**
 * Extracts numeric amount from input string.
 * @param {string} text
 * @returns {number|null}
 */
function extractAmount(text) {
  if (!text) return null;

  // 1. Direct Regex for numbers e.g. "₹ 250", "250 rupees", "250.00", "250", "rs 250"
  // Currency symbols or keywords: ₹, rs, inr, rupees, rupaye, rupayalu, taka, bucks
  const currencyPatterns = [
    /(?:₹|rs\.?|inr)\s*([\d,]+(?:\.\d+)?)/i,
    /([\d,]+(?:\.\d+)?)\s*(?:₹|rs\.?|inr|rupees|rupee|rupaye|rupayalu|rupai|rpk|రూపాయలు|रुपये|रुपए)/i,
    /\b(\d+(?:\.\d+)?)\b/
  ];

  for (const pattern of currencyPatterns) {
    const match = text.match(pattern);
    if (match && match[1]) {
      const numStr = match[1].replace(/,/g, '');
      const parsed = parseFloat(numStr);
      if (!isNaN(parsed) && parsed > 0) {
        return parsed;
      }
    }
  }

  // 2. Fallback: Parse spoken number words (e.g. "two hundred fifty", "paanch hazaar", "rendu vandhalu")
  const tokens = text.toLowerCase().split(/\s+/);
  let total = 0;
  let currentGroup = 0;
  let foundWord = false;

  for (const token of tokens) {
    const cleanToken = token.replace(/[^a-z\u0900-\u097F\u0C00-\u0C7F]/gi, '');
    if (NUMBER_WORDS[cleanToken] !== undefined) {
      foundWord = true;
      const val = NUMBER_WORDS[cleanToken];
      if (val === 100) {
        currentGroup = (currentGroup === 0 ? 1 : currentGroup) * 100;
      } else if (val === 1000 || val === 100000) {
        currentGroup = (currentGroup === 0 ? 1 : currentGroup) * val;
        total += currentGroup;
        currentGroup = 0;
      } else {
        currentGroup += val;
      }
    }
  }
  total += currentGroup;

  return foundWord && total > 0 ? total : null;
}

/**
 * Extracts transaction type (income / expense).
 * @param {string} text
 * @returns {'income'|'expense'}
 */
function extractType(text) {
  const lower = (text || '').toLowerCase();

  for (const kw of INCOME_KEYWORDS) {
    if (lower.includes(kw.toLowerCase())) {
      return 'income';
    }
  }

  for (const kw of EXPENSE_KEYWORDS) {
    if (lower.includes(kw.toLowerCase())) {
      return 'expense';
    }
  }

  // Default to expense for standard finance tracking
  return 'expense';
}

/**
 * Extracts transaction date.
 * @param {string} text
 * @returns {{ date: Date, relativeLabel: string }}
 */
function extractDate(text) {
  const lower = (text || '').toLowerCase();
  const now = new Date();

  // Day before yesterday / Parso / Monna
  if (lower.includes('day before yesterday') || lower.includes('parso') || lower.includes('monna')) {
    const d = new Date(now);
    d.setDate(d.getDate() - 2);
    return { date: d, relativeLabel: 'Day before yesterday' };
  }

  // Yesterday / Kal / Ninna
  if (lower.includes('yesterday') || lower.includes('kal') || lower.includes('ninna') || lower.includes('ninnalo') || lower.includes('कल') || lower.includes('నిన్న')) {
    const d = new Date(now);
    d.setDate(d.getDate() - 1);
    return { date: d, relativeLabel: 'Yesterday' };
  }

  // Today / Aaj / Eeroju
  return { date: now, relativeLabel: 'Today' };
}

/**
 * Extracts and cleans the description.
 * @param {string} text
 * @param {number|null} amount
 * @param {string} category
 * @returns {string}
 */
function extractDescription(text, amount, category) {
  if (!text) return category || 'Expense';

  let cleaned = text;

  // Remove common prefix / filler phrases in English, Hindi, Telugu
  const stopPhrases = [
    // English
    /^i\s+(?:have\s+)?(?:spent|paid|bought|purchased|received|got)\b/i,
    /\b(?:rupees|rupee|rs|inr|rupaye|rupayalu|rupai)\b/gi,
    /\b(?:today|yesterday|tomorrow|day before yesterday)\b/gi,
    /\b(?:for|on|as|in|at|a|an|the)\b/gi,
    // Hindi
    /\b(?:maine|maine\s+aaj|kal|aaj|mujhe|parso|rupaye|rupayee|kharch\s+kiye|kharch\s+kiya|diye|diya|ke\s+liye|par|ko|hua)\b/gi,
    /\b(?:मैंने|रुपये|रुपए|खर्च|किए|किया|दिए|दिया|पर|के लिए|आज|कल)\b/gi,
    // Telugu
    /\b(?:nenu|eeroju|ninna|ninnalo|monna|naku|rupayalu|rupailu|kharchu|chesanu|ichanu|pettanu|kosam|ki|lo|vachindi)\b/gi,
    /\b(?:నేను|ఈరోజు|నిన్న|రూపాయలు|ఖర్చు|చేశాను|కోసం)\b/gi,
  ];

  // Remove amount number string
  if (amount) {
    cleaned = cleaned.replace(new RegExp(`\\b${amount}\\b`, 'g'), '');
  }

  for (const regex of stopPhrases) {
    cleaned = cleaned.replace(regex, ' ');
  }

  // Clean extra punctuation and multiple spaces
  cleaned = cleaned.replace(/[^\w\s\u0900-\u097F\u0C00-\u0C7F]/g, ' ').replace(/\s+/g, ' ').trim();

  // Capitalize first letter
  if (cleaned.length > 0) {
    return cleaned.charAt(0).toUpperCase() + cleaned.slice(1);
  }

  return category || 'Expense';
}

/**
 * Main parseTransaction function.
 * @param {string} rawText
 * @param {string} preferredLanguage
 * @param {Array<string>} [customCategories]
 * @returns {object} Extracted transaction details
 */
function parseTransaction(rawText, preferredLanguage = 'en', customCategories = []) {
  if (!rawText || typeof rawText !== 'string' || !rawText.trim()) {
    return {
      success: false,
      isComplete: false,
      message: 'Empty voice input received. Please speak clearly.',
      missingField: 'input',
    };
  }

  const text = rawText.trim();
  const amount = extractAmount(text);
  const type = extractType(text);
  const { date, relativeLabel } = extractDate(text);
  const category = (type === 'income') ? 'Salary' : categorizeText(text, customCategories);
  const description = extractDescription(text, amount, category);

  const isComplete = amount !== null && amount > 0;
  let clarificationNeeded = null;

  if (!amount || amount <= 0) {
    clarificationNeeded = 'amount';
  }

  return {
    success: true,
    isComplete,
    clarificationNeeded,
    data: {
      amount: amount || 0,
      type,
      category,
      description,
      transactionDate: date.toISOString(),
      dateLabel: relativeLabel,
      rawVoiceText: text,
      source: 'voice',
      confidence: isComplete ? 0.95 : 0.6,
    },
    suggestedSpeechResponse: isComplete
      ? (preferredLanguage === 'te'
          ? `${relativeLabel} ${category} kosam ₹${amount} record cheyyamantara?`
          : preferredLanguage === 'hi'
          ? `Kya main ${relativeLabel} ke liye ${category} me ₹${amount} save karun?`
          : `Recorded ₹${amount} for ${description} under ${category}. Confirm to save.`)
      : 'Could not catch the amount clearly. Please mention how much you spent.'
  };
}

module.exports = {
  parseTransaction,
  extractAmount,
  extractType,
  extractDate,
  extractDescription,
};
