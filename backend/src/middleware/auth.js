const jwt = require('jsonwebtoken');
const User = require('../models/User');

const protect = async (req, res, next) => {
  let token;

  if (
    req.headers.authorization &&
    req.headers.authorization.startsWith('Bearer')
  ) {
    token = req.headers.authorization.split(' ')[1];
  }

  if (!token) {
    return res.status(401).json({
      success: false,
      message: 'Not authorized to access this route. No token provided.',
    });
  }

  try {
    const decoded = jwt.verify(
      token,
      process.env.JWT_SECRET || 'super_secret_voice_expense_jwt_token_key_2026'
    );

    // Try finding in Mongo DB
    try {
      const user = await User.findById(decoded.id).select('-passwordHash');
      if (user) {
        req.user = user;
        return next();
      }
    } catch (dbErr) {
      // In fallback mode or if DB offline
    }

    // Resilient fallback with payload decoded
    req.user = {
      _id: decoded.id,
      name: decoded.name || 'Demo User',
      email: decoded.email || 'user@example.com',
      preferredLanguage: decoded.preferredLanguage || 'en',
    };
    next();
  } catch (error) {
    return res.status(401).json({
      success: false,
      message: 'Not authorized, token validation failed or token expired.',
    });
  }
};

module.exports = { protect };

