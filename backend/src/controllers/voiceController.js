const { parseTransaction } = require('../nlp/multilingualParser');
const { parseFinancialQuery, getDateBounds, generateVoiceResponse } = require('../nlp/queryEngine');
const Transaction = require('../models/Transaction');
const Budget = require('../models/Budget');
const { mockTransactions } = require('./transactionController');
const { mockBudgets } = require('./budgetController');

// Helper to query user transactions
async function getUserTransactions(userId) {
  try {
    return await Transaction.find({ userId });
  } catch (err) {
    return mockTransactions.filter((t) => t.userId.toString() === userId.toString());
  }
}

// @desc Parse natural voice input for expense/income entry
// @route POST /api/voice/parse
const parseVoiceTransaction = async (req, res) => {
  try {
    const { text, language, customCategories } = req.body;

    if (!text) {
      return res.status(400).json({
        success: false,
        message: 'No voice text provided for parsing',
      });
    }

    const preferredLanguage = language || req.user?.preferredLanguage || 'en';
    const result = parseTransaction(text, preferredLanguage, customCategories || []);

    return res.json(result);
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: error.message || 'Error processing voice input',
    });
  }
};

// @desc Process voice-based financial query with real database calculations
// @route POST /api/voice/query
const processVoiceQuery = async (req, res) => {
  try {
    const userId = req.user._id.toString();
    const { query, language } = req.body;

    if (!query) {
      return res.status(400).json({
        success: false,
        message: 'No query text provided',
      });
    }

    const preferredLanguage = language || req.user?.preferredLanguage || 'en';
    const parsedQuery = parseFinancialQuery(query, preferredLanguage);

    const { intent, category, dateRange } = parsedQuery;
    const { start, end } = getDateBounds(dateRange);

    const allTx = await getUserTransactions(userId);

    // Filter transactions by date range
    const rangeTx = allTx.filter((t) => {
      const d = new Date(t.transactionDate);
      return d >= start && d <= end;
    });

    let spokenAnswer = '';
    let responseData = {
      intent,
      dateRange,
      category,
      amount: 0,
    };

    const dateRangeLabels = {
      today: 'today',
      yesterday: 'yesterday',
      this_week: 'this week',
      last_week: 'last week',
      this_month: 'this month',
      last_month: 'last month',
      all_time: 'overall',
    };
    const dateRangeLabel = dateRangeLabels[dateRange] || 'in this period';

    switch (intent) {
      case 'CATEGORY_SPENDING': {
        const targetCat = category || 'Food';
        const sum = rangeTx
          .filter((t) => t.type === 'expense' && t.category.toLowerCase() === targetCat.toLowerCase())
          .reduce((acc, t) => acc + t.amount, 0);

        responseData.category = targetCat;
        responseData.amount = sum;
        responseData.dateRangeLabel = dateRangeLabel;

        spokenAnswer = generateVoiceResponse('CATEGORY_SPENDING', responseData, preferredLanguage);
        break;
      }

      case 'TOTAL_SPENDING': {
        const sum = rangeTx
          .filter((t) => t.type === 'expense')
          .reduce((acc, t) => acc + t.amount, 0);

        responseData.amount = sum;
        responseData.dateRangeLabel = dateRangeLabel;

        spokenAnswer = generateVoiceResponse('TOTAL_SPENDING', responseData, preferredLanguage);
        break;
      }

      case 'TOTAL_INCOME': {
        const sum = rangeTx
          .filter((t) => t.type === 'income')
          .reduce((acc, t) => acc + t.amount, 0);

        responseData.amount = sum;
        responseData.dateRangeLabel = dateRangeLabel;

        spokenAnswer = generateVoiceResponse('TOTAL_INCOME', responseData, preferredLanguage);
        break;
      }

      case 'HIGHEST_CATEGORY': {
        const catMap = {};
        rangeTx
          .filter((t) => t.type === 'expense')
          .forEach((t) => {
            catMap[t.category] = (catMap[t.category] || 0) + t.amount;
          });

        let highestCat = 'None';
        let maxVal = 0;
        for (const [cat, amt] of Object.entries(catMap)) {
          if (amt > maxVal) {
            maxVal = amt;
            highestCat = cat;
          }
        }

        responseData.highestCategory = highestCat;
        responseData.amount = maxVal;
        responseData.dateRangeLabel = dateRangeLabel;

        spokenAnswer = generateVoiceResponse('HIGHEST_CATEGORY', responseData, preferredLanguage);
        break;
      }

      case 'REMAINING_BUDGET': {
        const targetCat = category || 'Food';
        const now = new Date();
        const startMonth = new Date(now.getFullYear(), now.getMonth(), 1);
        const endMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59);

        let budgetLimit = 5000;
        try {
          const b = await Budget.findOne({
            userId,
            category: new RegExp(`^${targetCat}$`, 'i'),
            month: now.getMonth() + 1,
            year: now.getFullYear(),
          });
          if (b) budgetLimit = b.monthlyLimit;
        } catch (e) {
          const b = mockBudgets.find((m) => m.category.toLowerCase() === targetCat.toLowerCase());
          if (b) budgetLimit = b.monthlyLimit;
        }

        const spentMonth = allTx
          .filter((t) => {
            const d = new Date(t.transactionDate);
            return t.type === 'expense' && t.category.toLowerCase() === targetCat.toLowerCase() && d >= startMonth && d <= endMonth;
          })
          .reduce((acc, t) => acc + t.amount, 0);

        const remaining = Math.max(0, budgetLimit - spentMonth);
        responseData.category = targetCat;
        responseData.budgetLimit = budgetLimit;
        responseData.spent = spentMonth;
        responseData.remainingBudget = remaining;
        responseData.amount = remaining;

        spokenAnswer = generateVoiceResponse('REMAINING_BUDGET', responseData, preferredLanguage);
        break;
      }

      case 'MONTHLY_COMPARISON': {
        const now = new Date();
        const thisMonthStart = new Date(now.getFullYear(), now.getMonth(), 1);
        const thisMonthEnd = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59);

        const lastMonthStart = new Date(now.getFullYear(), now.getMonth() - 1, 1);
        const lastMonthEnd = new Date(now.getFullYear(), now.getMonth(), 0, 23, 59, 59);

        const thisMonthExp = allTx
          .filter((t) => {
            const d = new Date(t.transactionDate);
            return t.type === 'expense' && d >= thisMonthStart && d <= thisMonthEnd;
          })
          .reduce((acc, t) => acc + t.amount, 0);

        const lastMonthExp = allTx
          .filter((t) => {
            const d = new Date(t.transactionDate);
            return t.type === 'expense' && d >= lastMonthStart && d <= lastMonthEnd;
          })
          .reduce((acc, t) => acc + t.amount, 0);

        const diff = thisMonthExp - lastMonthExp;
        responseData.thisMonth = thisMonthExp;
        responseData.lastMonth = lastMonthExp;
        responseData.compareDiff = diff;
        responseData.amount = Math.abs(diff);

        spokenAnswer = generateVoiceResponse('MONTHLY_COMPARISON', responseData, preferredLanguage);
        break;
      }

      case 'CURRENT_BALANCE':
      default: {
        const income = allTx.filter((t) => t.type === 'income').reduce((acc, t) => acc + t.amount, 0);
        const expense = allTx.filter((t) => t.type === 'expense').reduce((acc, t) => acc + t.amount, 0);
        const balance = income - expense;

        responseData.amount = balance;
        spokenAnswer = generateVoiceResponse('CURRENT_BALANCE', responseData, preferredLanguage);
        break;
      }
    }

    return res.json({
      success: true,
      query,
      spokenAnswer,
      data: responseData,
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: error.message || 'Error executing voice financial query',
    });
  }
};

module.exports = {
  parseVoiceTransaction,
  processVoiceQuery,
};

