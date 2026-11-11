const mongoose = require('mongoose');
const dotenv = require('dotenv');
const User = require('./models/User');
const Transaction = require('./models/Transaction');
const Budget = require('./models/Budget');
const Category = require('./models/Category');

dotenv.config({ path: __dirname + '/../.env' });

const seedData = async () => {
  try {
    await mongoose.connect(process.env.MONGODB_URI || 'mongodb://127.0.0.1:27017/voice_expense_db');
    console.log('[Seed] Connected to MongoDB');

    // Clear existing
    await User.deleteMany();
    await Transaction.deleteMany();
    await Budget.deleteMany();
    await Category.deleteMany();

    // 1. Create Default User
    const user = await User.create({
      name: 'Priya Sharma',
      email: 'demo@expense.com',
      passwordHash: 'Demo1234', // password gets hashed in pre-save hook
      preferredLanguage: 'en',
    });
    console.log('[Seed] Demo User created: demo@expense.com / Demo1234');

    const now = new Date();
    const currentMonth = now.getMonth() + 1;
    const currentYear = now.getFullYear();

    // 2. Create Default Categories
    const defaultCategories = [
      { categoryName: 'Food', categoryType: 'expense', icon: 'restaurant', color: '#1B4D3E' },
      { categoryName: 'Travel', categoryType: 'expense', icon: 'directions_car', color: '#2E7D32' },
      { categoryName: 'Shopping', categoryType: 'expense', icon: 'shopping_bag', color: '#D97706' },
      { categoryName: 'Bills', categoryType: 'expense', icon: 'receipt_long', color: '#0284C7' },
      { categoryName: 'Health', categoryType: 'expense', icon: 'local_hospital', color: '#DC2626' },
      { categoryName: 'Education', categoryType: 'expense', icon: 'school', color: '#7C3AED' },
      { categoryName: 'Entertainment', categoryType: 'expense', icon: 'movie', color: '#EC4899' },
      { categoryName: 'Rent', categoryType: 'expense', icon: 'home', color: '#4B5563' },
      { categoryName: 'Salary', categoryType: 'income', icon: 'payments', color: '#10B981' },
      { categoryName: 'Other', categoryType: 'both', icon: 'more_horiz', color: '#6B7280' },
    ];
    await Category.insertMany(defaultCategories.map((c) => ({ ...c, userId: user._id })));
    console.log('[Seed] Default categories seeded');

    // 3. Create Sample Budgets
    const budgets = [
      { userId: user._id, category: 'Food', monthlyLimit: 6000, month: currentMonth, year: currentYear },
      { userId: user._id, category: 'Travel', monthlyLimit: 2500, month: currentMonth, year: currentYear },
      { userId: user._id, category: 'Shopping', monthlyLimit: 4000, month: currentMonth, year: currentYear },
      { userId: user._id, category: 'Bills', monthlyLimit: 3000, month: currentMonth, year: currentYear },
      { userId: user._id, category: 'Entertainment', monthlyLimit: 2000, month: currentMonth, year: currentYear },
    ];
    await Budget.insertMany(budgets);
    console.log('[Seed] Sample budgets seeded');

    // 4. Create Sample Transactions across past days
    const transactions = [
      {
        userId: user._id,
        amount: 55000,
        type: 'income',
        category: 'Salary',
        description: 'Monthly salary credited',
        transactionDate: new Date(currentYear, now.getMonth(), 1),
        paymentMethod: 'Net Banking',
        source: 'manual',
      },
      {
        userId: user._id,
        amount: 250,
        type: 'expense',
        category: 'Food',
        description: 'Lunch at cafe',
        transactionDate: now,
        paymentMethod: 'UPI',
        source: 'voice',
        rawVoiceText: 'I spent 250 rupees on lunch today',
      },
      {
        userId: user._id,
        amount: 50,
        type: 'expense',
        category: 'Food',
        description: 'Tea with biscuits',
        transactionDate: now,
        paymentMethod: 'Cash',
        source: 'voice',
        rawVoiceText: 'Today I spent 50 rupees on tea',
      },
      {
        userId: user._id,
        amount: 100,
        type: 'expense',
        category: 'Travel',
        description: 'Petrol for bike',
        transactionDate: new Date(Date.now() - 86400000),
        paymentMethod: 'UPI',
        source: 'voice',
        rawVoiceText: 'Yesterday I paid 100 rupees for petrol',
      },
      {
        userId: user._id,
        amount: 1200,
        type: 'expense',
        category: 'Shopping',
        description: 'Shirt purchase',
        transactionDate: new Date(Date.now() - 172800000),
        paymentMethod: 'Credit Card',
        source: 'voice',
        rawVoiceText: 'I purchased a shirt for 1200 rupees',
      },
      {
        userId: user._id,
        amount: 1450,
        type: 'expense',
        category: 'Bills',
        description: 'Electricity bill payment',
        transactionDate: new Date(Date.now() - 259200000),
        paymentMethod: 'UPI',
        source: 'manual',
      },
      {
        userId: user._id,
        amount: 450,
        type: 'expense',
        category: 'Health',
        description: 'Pharmacy vitamins & cold tablets',
        transactionDate: new Date(Date.now() - 345600000),
        paymentMethod: 'Cash',
        source: 'manual',
      },
      {
        userId: user._id,
        amount: 380,
        type: 'expense',
        category: 'Entertainment',
        description: 'Movie ticket',
        transactionDate: new Date(Date.now() - 432000000),
        paymentMethod: 'UPI',
        source: 'manual',
      },
    ];

    await Transaction.insertMany(transactions);
    console.log('[Seed] Sample transactions successfully seeded!');

    process.exit(0);
  } catch (error) {
    console.error('[Seed Error]', error);
    process.exit(1);
  }
};

seedData();

