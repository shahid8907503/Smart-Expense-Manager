const express = require('express');
const router = express.Router();
const {
  getDashboardSummary,
  getCategoryBreakdown,
  getMonthlyTrends,
} = require('../controllers/analyticsController');
const { protect } = require('../middleware/auth');

router.use(protect);

router.get('/summary', getDashboardSummary);
router.get('/categories', getCategoryBreakdown);
router.get('/monthly', getMonthlyTrends);

module.exports = router;

