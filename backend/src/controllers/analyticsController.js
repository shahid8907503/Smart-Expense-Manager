const Transaction = require('../models/Transaction');
const Budget = require('../models/Budget');
const { mockTransactions } = require('./transactionController');
const { mockBudgets } = require('./budgetController');

// Helper to get user transactions
async function getUserTransactions(userId) {
  try {
    return await Transaction.find({ userId });
  } catch (err) {
    return mockTransactions.filter((t) => t.userId.toString() === userId.toString());
  }
}

// @desc Overall dashboard summary
// @route GET /api/analytics/summary
const getDashboardSummary = async (req, res) => {
  try {
    const userId = req.user._id.toString();
    const transactions = await getUserTransactions(userId);

    const now = new Date();
    const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
    const endOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59, 999);

    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0);
    const endOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59, 999);

    let totalIncome = 0;
    let totalExpense = 0;
    let todaySpending = 0;
    let thisMonthSpending = 0;

    transactions.forEach((tx) => {
      const txDate = new Date(tx.transactionDate);
      if (tx.type === 'income') {
        totalIncome += tx.amount;
      } else if (tx.type === 'expense') {
        totalExpense += tx.amount;

        if (txDate >= startOfToday && txDate <= endOfToday) {
          todaySpending += tx.amount;
        }

        if (txDate >= startOfMonth && txDate <= endOfMonth) {
          thisMonthSpending += tx.amount;
        }
      }
    });

    const balance = totalIncome - totalExpense;

    // Budget remaining
    let monthlyBudgetTotal = 0;
    try {
      const budgets = await Budget.find({
        userId,
        month: now.getMonth() + 1,
        year: now.getFullYear(),
      });
      monthlyBudgetTotal = budgets.reduce((acc, b) => acc + b.monthlyLimit, 0);
    } catch (bErr) {
      const budgets = mockBudgets.filter(
        (b) => b.userId.toString() === userId && b.month === now.getMonth() + 1 && b.year === now.getFullYear()
      );
      monthlyBudgetTotal = budgets.reduce((acc, b) => acc + b.monthlyLimit, 0);
    }

    const remainingBudget = Math.max(0, monthlyBudgetTotal - thisMonthSpending);

    return res.json({
      success: true,
      data: {
        totalIncome,
        totalExpense,
        balance,
        todaySpending,
        thisMonthSpending,
        monthlyBudgetTotal,
        remainingBudget,
        currency: '₹',
      },
    });
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

// @desc Category-wise expense breakdown for pie chart
// @route GET /api/analytics/categories
const getCategoryBreakdown = async (req, res) => {
  try {
    const userId = req.user._id.toString();
    const { month, year } = req.query;

    const now = new Date();
    const targetMonth = month ? parseInt(month) - 1 : now.getMonth();
    const targetYear = year ? parseInt(year) : now.getFullYear();

    const start = new Date(targetYear, targetMonth, 1, 0, 0, 0);
    const end = new Date(targetYear, targetMonth + 1, 0, 23, 59, 59, 999);

    const allTx = await getUserTransactions(userId);
    const expenses = allTx.filter((t) => {
      const d = new Date(t.transactionDate);
      return t.type === 'expense' && d >= start && d <= end;
    });

    const categoryMap = {};
    let totalExpenseMonth = 0;

    expenses.forEach((tx) => {
      categoryMap[tx.category] = (categoryMap[tx.category] || 0) + tx.amount;
      totalExpenseMonth += tx.amount;
    });

    const result = Object.entries(categoryMap).map(([category, amount]) => ({
      category,
      amount,
      percentage: totalExpenseMonth > 0 ? Math.round((amount / totalExpenseMonth) * 1000) / 10 : 0,
    }));

    // Sort descending
    result.sort((a, b) => b.amount - a.amount);

    return res.json({
      success: true,
      totalExpense: totalExpenseMonth,
      month: targetMonth + 1,
      year: targetYear,
      data: result,
    });
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

// @desc Monthly expense and income trends (last 6 months) for bar chart
// @route GET /api/analytics/monthly
const getMonthlyTrends = async (req, res) => {
  try {
    const userId = req.user._id.toString();
    const allTx = await getUserTransactions(userId);
    const now = new Date();

    const months = [];
    for (let i = 5; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      months.push({
        monthName: d.toLocaleString('en-US', { month: 'short' }),
        month: d.getMonth() + 1,
        year: d.getFullYear(),
        income: 0,
        expense: 0,
      });
    }

    allTx.forEach((tx) => {
      const d = new Date(tx.transactionDate);
      const m = d.getMonth() + 1;
      const y = d.getFullYear();

      const bucket = months.find((b) => b.month === m && b.year === y);
      if (bucket) {
        if (tx.type === 'income') {
          bucket.income += tx.amount;
        } else if (tx.type === 'expense') {
          bucket.expense += tx.amount;
        }
      }
    });

    // Spending insights
    const thisMonthBucket = months[months.length - 1];
    const lastMonthBucket = months[months.length - 2];
    let insight = null;

    if (lastMonthBucket && lastMonthBucket.expense > 0 && thisMonthBucket) {
      const diff = thisMonthBucket.expense - lastMonthBucket.expense;
      const pct = Math.round((Math.abs(diff) / lastMonthBucket.expense) * 100);
      if (diff > 0) {
        insight = `You spent ${pct}% more this month compared to last month.`;
      } else {
        insight = `You spent ${pct}% less this month compared to last month. Good financial discipline!`;
      }
    }

    return res.json({
      success: true,
      data: months,
      insight,
    });
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

module.exports = {
  getDashboardSummary,
  getCategoryBreakdown,
  getMonthlyTrends,
};

