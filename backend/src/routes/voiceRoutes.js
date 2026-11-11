const express = require('express');
const router = express.Router();
const { parseVoiceTransaction, processVoiceQuery } = require('../controllers/voiceController');
const { protect } = require('../middleware/auth');

router.use(protect);

router.post('/parse', parseVoiceTransaction);
router.post('/query', processVoiceQuery);

module.exports = router;

