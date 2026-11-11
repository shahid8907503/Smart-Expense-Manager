const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const User = require('../models/User');

// Resilient memory store for quick testing or offline DB demo
const mockUsers = [];

const generateToken = (id, name, email, preferredLanguage) => {
  return jwt.sign(
    { id, name, email, preferredLanguage },
    process.env.JWT_SECRET || 'super_secret_voice_expense_jwt_token_key_2026',
    { expiresIn: '30d' }
  );
};

// @desc Register user
// @route POST /api/auth/register
const registerUser = async (req, res) => {
  try {
    const { name, email, password, preferredLanguage } = req.body;

    if (!name || !email || !password) {
      return res.status(400).json({
        success: false,
        message: 'Please provide name, email, and password',
      });
    }

    // Try MongoDB first
    try {
      const userExists = await User.findOne({ email: email.toLowerCase() });
      if (userExists) {
        return res.status(400).json({
          success: false,
          message: 'An account with this email already exists',
        });
      }

      const user = await User.create({
        name,
        email: email.toLowerCase(),
        passwordHash: password, // pre-save hook handles bcrypt
        preferredLanguage: preferredLanguage || 'en',
      });

      return res.status(201).json({
        success: true,
        message: 'User registered successfully',
        data: {
          _id: user._id,
          name: user.name,
          email: user.email,
          preferredLanguage: user.preferredLanguage,
          token: generateToken(user._id, user.name, user.email, user.preferredLanguage),
        },
      });
    } catch (dbErr) {
      console.warn('[Auth] Database offline or error, using fallback store:', dbErr.message);

      const exists = mockUsers.find((u) => u.email.toLowerCase() === email.toLowerCase());
      if (exists) {
        return res.status(400).json({
          success: false,
          message: 'An account with this email already exists',
        });
      }

      const salt = await bcrypt.genSalt(10);
      const hashedPassword = await bcrypt.hash(password, salt);
      const fallbackUser = {
        _id: 'user_' + Date.now(),
        name,
        email: email.toLowerCase(),
        passwordHash: hashedPassword,
        preferredLanguage: preferredLanguage || 'en',
        createdAt: new Date(),
      };
      mockUsers.push(fallbackUser);

      return res.status(201).json({
        success: true,
        message: 'User registered successfully (Demo Mode)',
        data: {
          _id: fallbackUser._id,
          name: fallbackUser.name,
          email: fallbackUser.email,
          preferredLanguage: fallbackUser.preferredLanguage,
          token: generateToken(fallbackUser._id, fallbackUser.name, fallbackUser.email, fallbackUser.preferredLanguage),
        },
      });
    }
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: error.message || 'Server error during registration',
    });
  }
};

// @desc Login user
// @route POST /api/auth/login
const loginUser = async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({
        success: false,
        message: 'Please provide email and password',
      });
    }

    try {
      const user = await User.findOne({ email: email.toLowerCase() });
      if (user && (await user.matchPassword(password))) {
        return res.json({
          success: true,
          message: 'Login successful',
          data: {
            _id: user._id,
            name: user.name,
            email: user.email,
            preferredLanguage: user.preferredLanguage,
            token: generateToken(user._id, user.name, user.email, user.preferredLanguage),
          },
        });
      }
    } catch (dbErr) {
      console.warn('[Auth] DB lookup failed, checking fallback:', dbErr.message);
    }

    // Check mockUsers
    const mockUser = mockUsers.find((u) => u.email.toLowerCase() === email.toLowerCase());
    if (mockUser && (await bcrypt.compare(password, mockUser.passwordHash))) {
      return res.json({
        success: true,
        message: 'Login successful (Demo Mode)',
        data: {
          _id: mockUser._id,
          name: mockUser.name,
          email: mockUser.email,
          preferredLanguage: mockUser.preferredLanguage,
          token: generateToken(mockUser._id, mockUser.name, mockUser.email, mockUser.preferredLanguage),
        },
      });
    }

    // Default demo user fallback for effortless testing if unseeded
    if (email.toLowerCase() === 'demo@expense.com' && password === 'Demo1234') {
      const demoId = 'demo_user_001';
      return res.json({
        success: true,
        message: 'Demo login successful',
        data: {
          _id: demoId,
          name: 'Priya Sharma',
          email: 'demo@expense.com',
          preferredLanguage: 'en',
          token: generateToken(demoId, 'Priya Sharma', 'demo@expense.com', 'en'),
        },
      });
    }

    return res.status(401).json({
      success: false,
      message: 'Invalid email or password',
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: error.message || 'Server error during login',
    });
  }
};

// @desc Get user profile
// @route GET /api/auth/profile
const getUserProfile = async (req, res) => {
  try {
    return res.json({
      success: true,
      data: req.user,
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: error.message,
    });
  }
};

module.exports = {
  registerUser,
  loginUser,
  getUserProfile,
};

