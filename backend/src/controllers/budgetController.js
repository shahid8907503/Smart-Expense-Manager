const Budget = require('../models/Budget');
const Transaction = require('../models/Transaction');
const { mockTransactions } = require('./transactionController');

let mockBudgets = [
  {
    _id: 'bgt_001',
    userId: 'demo_user_001',
    category: 'Food',
    monthlyLimit: 5000,
    month: new Date().getMonth() + 1,
    year: new Date().getFullYear(),
    alertThresholdPercent: 80,
  },
  {
    _id: 'bgt_002',
    userId: 'demo_user_001',
    category: 'Travel',
    monthlyLimit: 2000,
    month: new Date().getMonth() + 1,
    year: new Date().getFullYear(),
    alertThresholdPercent: 80,
  },
  {
    _id: 'bgt_003',
    userId: 'demo_user_001',
    category: 'Shopping',
    monthlyLimit: 3000,
    month: new Date().getMonth() + 1,
    year: new Date().getFullYear(),
    alertThresholdPercent: 80,
  },
  {
    _id: 'bgt_004',
    userId: 'demo_user_001',
    category: 'Bills',
    monthlyLimit: 2500,
    month: new Date().getMonth() + 1,
    year: new Date().getFullYear(),
    alertThresholdPercent: 80,
  },
];

// @desc Get budgets for current or specified month with actual spending calculated
// @route GET /api/budgets
const getBudgets = async (req, res) => {
  try {
    const userId = req.user._id.toString();
    const now = new Date();
    const month = parseInt(req.query.month) || now.getMonth() + 1;
    const year = parseInt(req.query.year) || now.getFullYear();

    const startOfMonth = new Date(year, month - 1, 1);
    const endOfMonth = new Date(year, month, 0, 23, 59, 59, 999);

    let budgets = [];
    let transactions = [];

    try {
      budgets = await Budget.find({ userId, month, year });
      transactions = await Transaction.find({
        userId,
        type: 'expense',
        transactionDate: { $gte: startOfMonth, $lte: endOfMonth },
      });
    } catch (dbErr) {
      budgets = mockBudgets.filter((b) => b.userId.toString() === userId && b.month === month && b.year === year);
      transactions = mockTransactions.filter((t) => {
        const d = new Date(t.transactionDate);
        return t.userId.toString() === userId && t.type === 'expense' && d >= startOfMonth && d <= endOfMonth;
      });
    }

    // Calculate spending per category
    const categorySpending = {};
    transactions.forEach((tx) => {
      categorySpending[tx.category] = (categorySpending[tx.category] || 0) + tx.amount;
    });

    const enrichedBudgets = budgets.map((b) => {
      const budgetObj = b.toObject ? b.toObject() : { ...b };
      const spent = categorySpending[budgetObj.category] || 0;
      const remaining = Math.max(0, budgetObj.monthlyLimit - spent);
      const percentageUsed = budgetObj.monthlyLimit > 0 ? (spent / budgetObj.monthlyLimit) * 100 : 0;

      let status = 'HEALTHY'; // < 80%
      if (percentageUsed >= 100) {
        status = 'EXCEEDED';
      } else if (percentageUsed >= budgetObj.alertThresholdPercent) {
        status = 'WARNING';
      }

      return {
        ...budgetObj,
        spent,
        remaining,
        percentageUsed: Math.round(percentageUsed * 10) / 10,
        status,
      };
    });

    return res.json({
      success: true,
      month,
      year,
      count: enrichedBudgets.length,
      data: enrichedBudgets,
    });
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

// @desc Create or set monthly budget for a category
// @route POST /api/budgets
const setBudget = async (req, res) => {
  try {
    const userId = req.user._id.toString();
    const { category, monthlyLimit, month, year } = req.body;

    if (!category || !monthlyLimit) {
      return res.status(400).json({
        success: false,
        message: 'Please provide category and monthlyLimit',
      });
    }

    const now = new Date();
    const targetMonth = month || now.getMonth() + 1;
    const targetYear = year || now.getFullYear();

    try {
      const budget = await Budget.findOneAndUpdate(
        { userId, category, month: targetMonth, year: targetYear },
        {
          userId,
          category,
          monthlyLimit: parseFloat(monthlyLimit),
          month: targetMonth,
          year: targetYear,
        },
        { upsert: true, new: true, runValidators: true }
      );
      return res.status(201).json({ success: true, message: 'Budget set successfully', data: budget });
    } catch (dbErr) {
      const existingIdx = mockBudgets.findIndex(
        (b) => b.userId === userId && b.category === category && b.month === targetMonth && b.year === targetYear
      );
      const bgtItem = {
        _id: 'bgt_' + Date.now(),
        userId,
        category,
        monthlyLimit: parseFloat(monthlyLimit),
        month: targetMonth,
        year: targetYear,
        alertThresholdPercent: 80,
      };

      if (existingIdx >= 0) {
        mockBudgets[existingIdx] = bgtItem;
      } else {
        mockBudgets.push(bgtItem);
      }

      return res.status(201).json({ success: true, message: 'Budget set successfully (Demo Mode)', data: bgtItem });
    }
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

// @desc Delete budget
// @route DELETE /api/budgets/:id
const deleteBudget = async (req, res) => {
  try {
    const { id } = req.params;
    const userId = req.user._id.toString();

    try {
      await Budget.findOneAndDelete({ _id: id, userId });
      return res.json({ success: true, message: 'Budget deleted' });
    } catch (dbErr) {
      mockBudgets = mockBudgets.filter((b) => b._id !== id);
      return res.json({ success: true, message: 'Budget deleted' });
    }
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

module.exports = {
  getBudgets,
  setBudget,
  deleteBudget,
  mockBudgets,
};

