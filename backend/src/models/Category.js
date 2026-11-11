const mongoose = require('mongoose');

const categorySchema = new mongoose.Schema({
  userId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    default: null, // null means global system default category
    index: true,
  },
  categoryName: {
    type: String,
    required: true,
    trim: true,
  },
  categoryType: {
    type: String,
    enum: ['expense', 'income', 'both'],
    default: 'expense',
  },
  icon: {
    type: String,
    default: 'category',
  },
  color: {
    type: String,
    default: '#1B4D3E',
  },
  isCustom: {
    type: Boolean,
    default: false,
  },
  createdAt: {
    type: Date,
    default: Date.now,
  },
});

module.exports = mongoose.model('Category', categorySchema);

