const { categorizeText } = require('./categorizer');

/**
 * Parses user financial voice query to identify intent, category, and date range.
 * @param {string} queryText
 * @param {string} language
 * @returns {object} { intent, category, dateRange, language }
 */
function parseFinancialQuery(queryText, language = 'en') {
  const text = (queryText || '').toLowerCase().trim();

  // 1. Identify Date Range
  let dateRange = 'this_month'; // default
  if (text.includes('today') || text.includes('eeroju') || text.includes('aaj')) {
    dateRange = 'today';
  } else if (text.includes('yesterday') || text.includes('ninna') || text.includes('kal')) {
    dateRange = 'yesterday';
  } else if (text.includes('last week') || text.includes('pichle hafte') || text.includes('kinda vāram')) {
    dateRange = 'last_week';
  } else if (text.includes('this week') || text.includes('is hafte') || text.includes('ee vāram')) {
    dateRange = 'this_week';
  } else if (text.includes('last month') || text.includes('pichle mahine') || text.includes('poyina nela')) {
    dateRange = 'last_month';
  } else if (text.includes('this month') || text.includes('is mahine') || text.includes('ee nela')) {
    dateRange = 'this_month';
  } else if (text.includes('all time') || text.includes('mothaniki') || text.includes('kul')) {
    dateRange = 'all_time';
  }

  // 2. Identify Intent
  let intent = 'TOTAL_SPENDING';

  if (text.includes('compare') || text.includes('comparison') || (text.includes('this month') && text.includes('last month'))) {
    intent = 'MONTHLY_COMPARISON';
  } else if (text.includes('remaining') || text.includes('budget') || text.includes('migilina')) {
    intent = 'REMAINING_BUDGET';
  } else if (text.includes('highest') || text.includes('most') || text.includes('sabse zyada') || text.includes('ekkuva')) {
    intent = 'HIGHEST_CATEGORY';
  } else if (text.includes('income') || text.includes('salary') || text.includes('earn') || text.includes('aamadni') || text.includes('jeetham') || text.includes('kamayi')) {
    intent = 'TOTAL_INCOME';
  } else if (text.includes('balance') || text.includes('khate me') || text.includes('account balance')) {
    intent = 'CURRENT_BALANCE';
  } else {
    // Check if category is mentioned
    const detectedCategory = categorizeText(text);
    if (detectedCategory && detectedCategory !== 'Other') {
      intent = 'CATEGORY_SPENDING';
    } else {
      intent = 'TOTAL_SPENDING';
    }
  }

  // 3. Extract Category if relevant
  let targetCategory = null;
  const categories = ['Food', 'Travel', 'Shopping', 'Bills', 'Health', 'Education', 'Entertainment', 'Rent'];
  for (const cat of categories) {
    if (text.includes(cat.toLowerCase()) || categorizeText(text) === cat) {
      targetCategory = cat;
      break;
    }
  }

  return {
    originalText: queryText,
    intent,
    category: targetCategory,
    dateRange,
    language,
  };
}

/**
 * Calculates date boundaries for a given dateRange key.
 */
function getDateBounds(dateRange) {
  const now = new Date();
  let start = new Date(now);
  let end = new Date(now);

  switch (dateRange) {
    case 'today':
      start.setHours(0, 0, 0, 0);
      end.setHours(23, 59, 59, 999);
      break;
    case 'yesterday':
      start.setDate(start.getDate() - 1);
      start.setHours(0, 0, 0, 0);
      end.setDate(end.getDate() - 1);
      end.setHours(23, 59, 59, 999);
      break;
    case 'this_week': {
      const day = start.getDay();
      const diff = start.getDate() - day + (day === 0 ? -6 : 1); // Monday
      start.setDate(diff);
      start.setHours(0, 0, 0, 0);
      end.setHours(23, 59, 59, 999);
      break;
    }
    case 'last_week': {
      const day = start.getDay();
      const diff = start.getDate() - day - 6;
      start.setDate(diff);
      start.setHours(0, 0, 0, 0);
      end = new Date(start);
      end.setDate(end.getDate() + 6);
      end.setHours(23, 59, 59, 999);
      break;
    }
    case 'last_month':
      start = new Date(now.getFullYear(), now.getMonth() - 1, 1, 0, 0, 0);
      end = new Date(now.getFullYear(), now.getMonth(), 0, 23, 59, 59, 999);
      break;
    case 'this_month':
    default:
      start = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0);
      end = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59, 999);
      break;
  }

  return { start, end };
}

/**
 * Formats verbal answer based on intent, calculated numbers, and language.
 */
function generateVoiceResponse(intent, resultData, language = 'en') {
  const { amount, category, dateRangeLabel, highestCategory, compareDiff, budgetLimit, remainingBudget } = resultData;
  const formattedAmount = `₹${(amount || 0).toLocaleString('en-IN')}`;

  if (language === 'te') {
    switch (intent) {
      case 'CATEGORY_SPENDING':
        return `Meeru ${dateRangeLabel} lo ${category} kosam ${formattedAmount} rupayalu kharchu chesaru.`;
      case 'TOTAL_SPENDING':
        return `Meeru ${dateRangeLabel} lo motham ${formattedAmount} rupayalu kharchu chesaru.`;
      case 'TOTAL_INCOME':
        return `Meeru ${dateRangeLabel} lo ${formattedAmount} rupayala aadhayam pondharu.`;
      case 'HIGHEST_CATEGORY':
        return `Meeru ${dateRangeLabel} lo sabase ekkuva ${highestCategory} paina ${formattedAmount} kharchu chesaru.`;
      case 'REMAINING_BUDGET':
        return `${category} budget lo inka ₹${(remainingBudget || 0).toLocaleString('en-IN')} migili undi.`;
      default:
        return `Motham lekka: ${formattedAmount}.`;
    }
  }

  if (language === 'hi') {
    switch (intent) {
      case 'CATEGORY_SPENDING':
        return `Aapne ${dateRangeLabel} me ${category} par ${formattedAmount} rupaye kharch kiye hain.`;
      case 'TOTAL_SPENDING':
        return `Aapka ${dateRangeLabel} ka kul kharch ${formattedAmount} rupaye hai.`;
      case 'TOTAL_INCOME':
        return `Aapko ${dateRangeLabel} me ${formattedAmount} rupaye ki aamadni hui hai.`;
      case 'HIGHEST_CATEGORY':
        return `${dateRangeLabel} me sabse zyada kharch ${highestCategory} par ${formattedAmount} rupaye hua hai.`;
      case 'REMAINING_BUDGET':
        return `${category} budget me abhi ₹${(remainingBudget || 0).toLocaleString('en-IN')} rupaye bache hain.`;
      default:
        return `Kul rashi: ${formattedAmount}.`;
    }
  }

  // Default English
  switch (intent) {
    case 'CATEGORY_SPENDING':
      return `You spent ${formattedAmount} on ${category} ${dateRangeLabel}.`;
    case 'TOTAL_SPENDING':
      return `Your total spending ${dateRangeLabel} is ${formattedAmount}.`;
    case 'TOTAL_INCOME':
      return `You received ${formattedAmount} in income ${dateRangeLabel}.`;
    case 'HIGHEST_CATEGORY':
      return `Your highest spending category ${dateRangeLabel} is ${highestCategory} at ${formattedAmount}.`;
    case 'REMAINING_BUDGET':
      return `You have ₹${(remainingBudget || 0).toLocaleString('en-IN')} remaining in your ${category} budget.`;
    case 'MONTHLY_COMPARISON':
      return compareDiff >= 0
        ? `You spent ₹${Math.abs(compareDiff).toLocaleString('en-IN')} more this month compared to last month.`
        : `You spent ₹${Math.abs(compareDiff).toLocaleString('en-IN')} less this month compared to last month. Great job!`;
    case 'CURRENT_BALANCE':
      return `Your current available balance is ${formattedAmount}.`;
    default:
      return `Your total is ${formattedAmount}.`;
  }
}

module.exports = {
  parseFinancialQuery,
  getDateBounds,
  generateVoiceResponse,
};

