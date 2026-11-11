const mongoose = require('mongoose');

const transactionSchema = new mongoose.Schema({
  userId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
    index: true,
  },
  amount: {
    type: Number,
    required: [true, 'Please add transaction amount'],
    min: [0.01, 'Amount must be greater than zero'],
  },
  type: {
    type: String,
    enum: ['expense', 'income'],
    default: 'expense',
    required: true,
    index: true,
  },
  category: {
    type: String,
    required: [true, 'Please select a category'],
    default: 'Other',
    trim: true,
    index: true,
  },
  description: {
    type: String,
    required: [true, 'Please add a description'],
    trim: true,
    maxlength: [200, 'Description cannot exceed 200 characters'],
  },
  transactionDate: {
    type: Date,
    default: Date.now,
    index: true,
  },
  paymentMethod: {
    type: String,
    enum: ['UPI', 'Cash', 'Credit Card', 'Debit Card', 'Net Banking', 'Other'],
    default: 'Cash',
  },
  source: {
    type: String,
    enum: ['voice', 'manual'],
    default: 'manual',
  },
  syncStatus: {
    type: String,
    enum: ['synced', 'pending'],
    default: 'synced',
  },
  rawVoiceText: {
    type: String,
    default: null,
  },
  createdAt: {
    type: Date,
    default: Date.now,
  },
});

transactionSchema.index({ userId: 1, transactionDate: -1 });

module.exports = mongoose.model('Transaction', transactionSchema);

