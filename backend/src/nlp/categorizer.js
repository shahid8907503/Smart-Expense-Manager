// Keyword-based automatic categorization engine supporting English, Hindi, and Telugu

const CATEGORY_KEYWORDS = {
  Food: [
    'lunch', 'dinner', 'breakfast', 'snacks', 'snack', 'coffee', 'tea', 'chai',
    'restaurant', 'cafe', 'swiggy', 'zomato', 'groceries', 'grocery', 'vegetables',
    'fruits', 'milk', 'bread', 'egg', 'eggs', 'chicken', 'mutton', 'fish', 'biryani',
    'pizza', 'burger', 'tiffin', 'hotel', 'bhojan', 'khana', 'khane', 'nashta', 'rasoi',
    'sabji', 'roti', 'oota', 'thindi', 'tiffinlu', 'bhojanam', 'kura', 'pappu', 'food',
    'paneer', 'ice cream', 'juice', 'bakery', 'sweet', 'sweets', 'supermarket'
  ],
  Travel: [
    'petrol', 'diesel', 'fuel', 'cng', 'auto', 'cab', 'uber', 'ola', 'rapido',
    'bus', 'train', 'metro', 'flight', 'ticket', 'toll', 'parking', 'fare',
    'taxi', 'railway', 'irctc', 'indigo', 'air india', 'bike service', 'car service',
    'puncture', 'mechanic', 'travel', 'trip', 'prayanam', 'yatra', 'safar', 'gaadi',
    'vahanam', 'petrolu', 'rikshaw', 'rickshaw'
  ],
  Shopping: [
    'shirt', 't-shirt', 'pants', 'trousers', 'jeans', 'clothes', 'clothing', 'dress',
    'shoes', 'sandals', 'footwear', 'watch', 'bag', 'amazon', 'flipkart', 'myntra',
    'meesho', 'ajio', 'mall', 'shopping', 'electronics', 'mobile', 'phone', 'laptop',
    'headphone', 'earbuds', 'perfume', 'cosmetics', 'kapde', 'kharidari', 'battalu',
    'vastralu', 'kottha', 'apparel'
  ],
  Bills: [
    'electricity', 'current', 'power', 'water', 'water bill', 'internet', 'wifi',
    'broadband', 'recharge', 'mobile recharge', 'airtel', 'jio', 'vi', 'gas',
    'cylinder', 'lpg', 'indane', 'hp', 'maintenance', 'subscription', 'netflix',
    'prime', 'spotify', 'hotstar', 'youtube', 'cable', 'dth', 'bijli', 'currentu',
    'bill', 'bills', 'challan', 'tax', 'property tax'
  ],
  Health: [
    'medicine', 'medicines', 'doctor', 'clinic', 'hospital', 'pharmacy', 'medical',
    'chemist', 'apollo', 'medplus', 'tablets', 'syrup', 'injection', 'consultation',
    'checkup', 'test', 'blood test', 'lab', 'dentist', 'eye checkup', 'spectacles',
    'glasses', 'dawa', 'dawakhana', 'mandulu', 'chikitsa', 'vaidyam', 'aarogyam'
  ],
  Education: [
    'book', 'books', 'tuition', 'fee', 'fees', 'school', 'college', 'course',
    'udemy', 'coursera', 'exam', 'exam fee', 'coaching', 'stationery', 'pen',
    'notebook', 'notebooks', 'pustakalu', 'kitaben', 'padhai', 'vidya', 'chaduvu'
  ],
  Entertainment: [
    'movie', 'cinema', 'theater', 'theatre', 'pvr', 'inox', 'concert', 'game',
    'gaming', 'outing', 'party', 'club', 'pub', 'amusement', 'picnic', 'streaming',
    'vinodam', 'manoranjan', 'khel', 'show'
  ],
  Rent: [
    'rent', 'house rent', 'room rent', 'flat rent', 'apartment rent', 'pg rent',
    'hostel fee', 'office rent', 'kiraya', 'vadaka', 'bhadige', 'rentu'
  ]
};

/**
 * Predicts the category from text or description.
 * @param {string} text - text description
 * @param {Array<string>} [customCategories] - optional custom categories
 * @returns {string} Category name
 */
function categorizeText(text, customCategories = []) {
  if (!text || typeof text !== 'string') return 'Other';
  const clean = text.toLowerCase();

  // Check custom categories first
  if (Array.isArray(customCategories)) {
    for (const cat of customCategories) {
      if (clean.includes(cat.toLowerCase())) {
        return cat;
      }
    }
  }

  // Check predefined categories
  for (const [category, keywords] of Object.entries(CATEGORY_KEYWORDS)) {
    for (const kw of keywords) {
      const regex = new RegExp(`\\b${kw}\\b`, 'i');
      if (regex.test(clean)) {
        return category;
      }
    }
  }

  // Secondary substring check for compound terms
  for (const [category, keywords] of Object.entries(CATEGORY_KEYWORDS)) {
    for (const kw of keywords) {
      if (clean.includes(kw)) {
        return category;
      }
    }
  }

  return 'Other';
}

module.exports = {
  categorizeText,
  CATEGORY_KEYWORDS,
};
