const mongoose = require('mongoose');

const budgetSchema = new mongoose.Schema({
  userId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
    index: true,
  },
  category: {
    type: String,
    required: [true, 'Please specify a category for this budget'],
    trim: true,
  },
  monthlyLimit: {
    type: Number,
    required: [true, 'Please provide monthly limit'],
    min: [1, 'Limit must be positive'],
  },
  month: {
    type: Number, // 1 to 12
    required: true,
    min: 1,
    max: 12,
  },
  year: {
    type: Number,
    required: true,
  },
  alertThresholdPercent: {
    type: Number,
    default: 80,
  },
  createdAt: {
    type: Date,
    default: Date.now,
  },
});

budgetSchema.index({ userId: 1, category: 1, month: 1, year: 1 }, { unique: true });

module.exports = mongoose.model('Budget', budgetSchema);

