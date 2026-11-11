const Transaction = require('../models/Transaction');

// In-memory fallback list for resilient demo execution
let mockTransactions = [
  {
    _id: 'tx_seed_001',
    userId: 'demo_user_001',
    amount: 250,
    type: 'expense',
    category: 'Food',
    description: 'Lunch with colleagues',
    transactionDate: new Date(),
    paymentMethod: 'UPI',
    source: 'voice',
    syncStatus: 'synced',
    createdAt: new Date(),
  },
  {
    _id: 'tx_seed_002',
    userId: 'demo_user_001',
    amount: 120,
    type: 'expense',
    category: 'Travel',
    description: 'Auto fare to office',
    transactionDate: new Date(Date.now() - 86400000),
    paymentMethod: 'Cash',
    source: 'voice',
    syncStatus: 'synced',
    createdAt: new Date(Date.now() - 86400000),
  },
  {
    _id: 'tx_seed_003',
    userId: 'demo_user_001',
    amount: 45000,
    type: 'income',
    category: 'Salary',
    description: 'Monthly salary credited',
    transactionDate: new Date(new Date().getFullYear(), new Date().getMonth(), 1),
    paymentMethod: 'Net Banking',
    source: 'manual',
    syncStatus: 'synced',
    createdAt: new Date(new Date().getFullYear(), new Date().getMonth(), 1),
  },
  {
    _id: 'tx_seed_004',
    userId: 'demo_user_001',
    amount: 1450,
    type: 'expense',
    category: 'Shopping',
    description: 'Cotton shirt from mall',
    transactionDate: new Date(Date.now() - 172800000),
    paymentMethod: 'Credit Card',
    source: 'voice',
    syncStatus: 'synced',
    createdAt: new Date(Date.now() - 172800000),
  },
  {
    _id: 'tx_seed_005',
    userId: 'demo_user_001',
    amount: 850,
    type: 'expense',
    category: 'Bills',
    description: 'Broadband internet bill',
    transactionDate: new Date(Date.now() - 259200000),
    paymentMethod: 'UPI',
    source: 'manual',
    syncStatus: 'synced',
    createdAt: new Date(Date.now() - 259200000),
  }
];

// @desc Create new transaction (single or batch sync)
// @route POST /api/transactions
const createTransaction = async (req, res) => {
  try {
    const userId = req.user._id;

    // Check if body is batch array (sync queue)
    if (Array.isArray(req.body)) {
      const items = req.body.map((item) => ({
        ...item,
        userId,
        syncStatus: 'synced',
      }));

      try {
        const savedBatch = await Transaction.insertMany(items);
        return res.status(201).json({
          success: true,
          count: savedBatch.length,
          data: savedBatch,
        });
      } catch (dbErr) {
        items.forEach((item) => {
          mockTransactions.unshift({
            _id: item._id || 'tx_' + Date.now() + Math.random().toString(36).substr(2, 4),
            ...item,
          });
        });
        return res.status(201).json({
          success: true,
          count: items.length,
          data: items,
        });
      }
    }

    const { amount, type, category, description, transactionDate, paymentMethod, source, rawVoiceText } = req.body;

    if (!amount || !description) {
      return res.status(400).json({
        success: false,
        message: 'Please provide both amount and description',
      });
    }

    const newTx = {
      userId,
      amount: parseFloat(amount),
      type: type || 'expense',
      category: category || 'Other',
      description: description.trim(),
      transactionDate: transactionDate ? new Date(transactionDate) : new Date(),
      paymentMethod: paymentMethod || 'Cash',
      source: source || 'manual',
      rawVoiceText: rawVoiceText || null,
      syncStatus: 'synced',
    };

    try {
      const transaction = await Transaction.create(newTx);
      return res.status(201).json({
        success: true,
        message: 'Transaction saved successfully',
        data: transaction,
      });
    } catch (dbErr) {
      const fallbackTx = {
        _id: 'tx_' + Date.now(),
        ...newTx,
        createdAt: new Date(),
      };
      mockTransactions.unshift(fallbackTx);
      return res.status(201).json({
        success: true,
        message: 'Transaction saved successfully (Demo Mode)',
        data: fallbackTx,
      });
    }
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: error.message || 'Error creating transaction',
    });
  }
};

// @desc Get transactions with filtering & pagination
// @route GET /api/transactions
const getTransactions = async (req, res) => {
  try {
    const userId = req.user._id.toString();
    const { type, category, startDate, endDate, search, limit = 50, page = 1 } = req.query;

    const query = { userId };
    if (type && ['expense', 'income'].includes(type)) {
      query.type = type;
    }
    if (category) {
      query.category = category;
    }
    if (startDate || endDate) {
      query.transactionDate = {};
      if (startDate) query.transactionDate.$gte = new Date(startDate);
      if (endDate) query.transactionDate.$lte = new Date(endDate);
    }
    if (search) {
      query.description = { $regex: search, $options: 'i' };
    }

    try {
      const transactions = await Transaction.find(query)
        .sort({ transactionDate: -1, createdAt: -1 })
        .limit(parseInt(limit))
        .skip((parseInt(page) - 1) * parseInt(limit));

      const total = await Transaction.countDocuments(query);

      return res.json({
        success: true,
        count: transactions.length,
        total,
        page: parseInt(page),
        pages: Math.ceil(total / parseInt(limit)),
        data: transactions,
      });
    } catch (dbErr) {
      // Fallback in-memory filter
      let filtered = mockTransactions.filter((tx) => tx.userId.toString() === userId);

      if (type) filtered = filtered.filter((t) => t.type === type);
      if (category) filtered = filtered.filter((t) => t.category.toLowerCase() === category.toLowerCase());
      if (search) filtered = filtered.filter((t) => t.description.toLowerCase().includes(search.toLowerCase()));
      if (startDate) filtered = filtered.filter((t) => new Date(t.transactionDate) >= new Date(startDate));
      if (endDate) filtered = filtered.filter((t) => new Date(t.transactionDate) <= new Date(endDate));

      filtered.sort((a, b) => new Date(b.transactionDate) - new Date(a.transactionDate));

      return res.json({
        success: true,
        count: filtered.length,
        total: filtered.length,
        page: 1,
        pages: 1,
        data: filtered,
      });
    }
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: error.message,
    });
  }
};

// @desc Get transaction by ID
// @route GET /api/transactions/:id
const getTransactionById = async (req, res) => {
  try {
    const { id } = req.params;
    const userId = req.user._id.toString();

    try {
      const tx = await Transaction.findOne({ _id: id, userId });
      if (!tx) {
        return res.status(404).json({ success: false, message: 'Transaction not found' });
      }
      return res.json({ success: true, data: tx });
    } catch (dbErr) {
      const tx = mockTransactions.find((t) => t._id === id && t.userId.toString() === userId);
      if (!tx) {
        return res.status(404).json({ success: false, message: 'Transaction not found' });
      }
      return res.json({ success: true, data: tx });
    }
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

// @desc Update transaction
// @route PUT /api/transactions/:id
const updateTransaction = async (req, res) => {
  try {
    const { id } = req.params;
    const userId = req.user._id.toString();

    try {
      let tx = await Transaction.findOne({ _id: id, userId });
      if (!tx) {
        return res.status(404).json({ success: false, message: 'Transaction not found' });
      }

      const updated = await Transaction.findByIdAndUpdate(id, req.body, { new: true, runValidators: true });
      return res.json({ success: true, message: 'Transaction updated', data: updated });
    } catch (dbErr) {
      const index = mockTransactions.findIndex((t) => t._id === id && t.userId.toString() === userId);
      if (index === -1) {
        return res.status(404).json({ success: false, message: 'Transaction not found' });
      }
      mockTransactions[index] = { ...mockTransactions[index], ...req.body };
      return res.json({ success: true, message: 'Transaction updated (Demo Mode)', data: mockTransactions[index] });
    }
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

// @desc Delete transaction
// @route DELETE /api/transactions/:id
const deleteTransaction = async (req, res) => {
  try {
    const { id } = req.params;
    const userId = req.user._id.toString();

    try {
      const tx = await Transaction.findOneAndDelete({ _id: id, userId });
      if (!tx) {
        return res.status(404).json({ success: false, message: 'Transaction not found' });
      }
      return res.json({ success: true, message: 'Transaction removed' });
    } catch (dbErr) {
      const index = mockTransactions.findIndex((t) => t._id === id && t.userId.toString() === userId);
      if (index === -1) {
        return res.status(404).json({ success: false, message: 'Transaction not found' });
      }
      mockTransactions.splice(index, 1);
      return res.json({ success: true, message: 'Transaction removed (Demo Mode)' });
    }
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

module.exports = {
  createTransaction,
  getTransactions,
  getTransactionById,
  updateTransaction,
  deleteTransaction,
  mockTransactions,
};

