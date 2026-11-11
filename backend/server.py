import http.server
import socketserver
import json
import re
import datetime
import urllib.parse

PORT = 5000

# In-memory storage with realistic demo records
users = [
    {
        "_id": "demo_user_001",
        "name": "Priya Sharma",
        "email": "demo@expense.com",
        "password": "Demo1234",
        "preferredLanguage": "en"
    }
]

transactions = [
    {
        "_id": "seed_1",
        "userId": "demo_user_001",
        "amount": 50000.0,
        "type": "income",
        "category": "Salary",
        "description": "Monthly salary credited",
        "transactionDate": (datetime.datetime.now() - datetime.timedelta(days=5)).isoformat(),
        "paymentMethod": "Net Banking",
        "source": "manual",
        "syncStatus": "synced"
    },
    {
        "_id": "seed_2",
        "userId": "demo_user_001",
        "amount": 250.0,
        "type": "expense",
        "category": "Food",
        "description": "Lunch at cafe",
        "transactionDate": datetime.datetime.now().isoformat(),
        "paymentMethod": "UPI",
        "source": "voice",
        "rawVoiceText": "I spent 250 rupees on lunch today",
        "syncStatus": "synced"
    },
    {
        "_id": "seed_3",
        "userId": "demo_user_001",
        "amount": 50.0,
        "type": "expense",
        "category": "Food",
        "description": "Tea with snacks",
        "transactionDate": datetime.datetime.now().isoformat(),
        "paymentMethod": "Cash",
        "source": "voice",
        "rawVoiceText": "Today I spent 50 rupees on tea",
        "syncStatus": "synced"
    },
    {
        "_id": "seed_4",
        "userId": "demo_user_001",
        "amount": 100.0,
        "type": "expense",
        "category": "Travel",
        "description": "Petrol for bike",
        "transactionDate": (datetime.datetime.now() - datetime.timedelta(days=1)).isoformat(),
        "paymentMethod": "UPI",
        "source": "voice",
        "rawVoiceText": "Yesterday I paid 100 rupees for petrol",
        "syncStatus": "synced"
    },
    {
        "_id": "seed_5",
        "userId": "demo_user_001",
        "amount": 1200.0,
        "type": "expense",
        "category": "Shopping",
        "description": "Cotton shirt purchase",
        "transactionDate": (datetime.datetime.now() - datetime.timedelta(days=2)).isoformat(),
        "paymentMethod": "Credit Card",
        "source": "voice",
        "rawVoiceText": "I purchased a shirt for 1200 rupees",
        "syncStatus": "synced"
    },
    {
        "_id": "seed_6",
        "userId": "demo_user_001",
        "amount": 1450.0,
        "type": "expense",
        "category": "Bills",
        "description": "Electricity current bill",
        "transactionDate": (datetime.datetime.now() - datetime.timedelta(days=3)).isoformat(),
        "paymentMethod": "UPI",
        "source": "manual",
        "syncStatus": "synced"
    }
]

budgets = [
    { "_id": "bgt_1", "userId": "demo_user_001", "category": "Food", "monthlyLimit": 5000.0, "month": datetime.datetime.now().month, "year": datetime.datetime.now().year },
    { "_id": "bgt_2", "userId": "demo_user_001", "category": "Travel", "monthlyLimit": 2000.0, "month": datetime.datetime.now().month, "year": datetime.datetime.now().year },
    { "_id": "bgt_3", "userId": "demo_user_001", "category": "Shopping", "monthlyLimit": 3000.0, "month": datetime.datetime.now().month, "year": datetime.datetime.now().year },
    { "_id": "bgt_4", "userId": "demo_user_001", "category": "Bills", "monthlyLimit": 2500.0, "month": datetime.datetime.now().month, "year": datetime.datetime.now().year }
]

CATEGORIES = {
    'Food': ['lunch', 'dinner', 'breakfast', 'snacks', 'snack', 'coffee', 'tea', 'chai', 'food', 'restaurant', 'biryani', 'khana', 'khane', 'oota', 'thindi', 'nashta', 'bhojan'],
    'Travel': ['petrol', 'diesel', 'fuel', 'auto', 'cab', 'uber', 'ola', 'bus', 'train', 'metro', 'flight', 'ticket', 'toll', 'parking', 'gaadi', 'safar', 'yatra', 'prayanam'],
    'Shopping': ['shirt', 'pants', 'clothes', 'dress', 'shoes', 'amazon', 'flipkart', 'mall', 'shopping', 'kapde', 'battalu'],
    'Bills': ['electricity', 'water', 'internet', 'wifi', 'recharge', 'gas', 'cylinder', 'bijli', 'currentu', 'bill'],
    'Health': ['medicine', 'doctor', 'clinic', 'hospital', 'medical', 'tablets', 'dawa', 'mandulu'],
    'Education': ['book', 'books', 'tuition', 'fee', 'school', 'college', 'course', 'exam', 'pustakalu', 'kitaben'],
    'Entertainment': ['movie', 'cinema', 'theater', 'game', 'party', 'streaming'],
    'Rent': ['rent', 'house rent', 'room rent', 'pg rent', 'kiraya', 'vadaka']
}

def categorize(text):
    lower = text.lower()
    for cat, kws in CATEGORIES.items():
        for kw in kws:
            if re.search(r'\b' + re.escape(kw) + r'\b', lower):
                return cat
    return 'Other'

def parse_voice_text(text):
    lower = text.lower()
    # Amount
    amt = 0.0
    m = re.search(r'(?:₹|rs\.?|inr)\s*([\d,]+(?:\.\d+)?)', text, re.IGNORECASE)
    if m:
        amt = float(m.group(1).replace(',', ''))
    else:
        m2 = re.search(r'([\d,]+(?:\.\d+)?)\s*(?:₹|rs\.?|inr|rupees|rupee|rupaye|rupayalu|rupai)', text, re.IGNORECASE)
        if m2:
            amt = float(m2.group(1).replace(',', ''))
        else:
            m3 = re.search(r'\b(\d+(?:\.\d+)?)\b', text)
            if m3:
                amt = float(m3.group(1))

    # Type
    tx_type = 'expense'
    for kw in ['salary', 'income', 'received', 'got', 'earned', 'credited', 'mili', 'mila', 'aamadni', 'vachindi']:
        if kw in lower:
            tx_type = 'income'
            break

    # Date
    date_label = 'Today'
    if 'day before yesterday' in lower or 'parso' in lower or 'monna' in lower:
        date_label = 'Day before yesterday'
    elif 'yesterday' in lower or 'kal' in lower or 'ninna' in lower:
        date_label = 'Yesterday'

    # Category
    category = 'Salary' if tx_type == 'income' else categorize(text)

    # Description
    clean = text
    if amt > 0:
        clean = re.sub(r'\b' + str(int(amt)) + r'\b', '', clean)
    clean = re.sub(r'(?i)\b(i|have|spent|paid|bought|purchased|received|got|for|on|as|in|at|a|an|the|rupees|rupee|rs|inr|rupaye|rupayalu|rupai|today|yesterday)\b', ' ', clean)
    clean = re.sub(r'(?i)\b(nenu|ninna|eeroju|monna|kharchu|chesanu|ichanu|pettanu|kosam|ki|lo|vachindi)\b', ' ', clean)
    clean = re.sub(r'(?i)\b(maine|aaj|kal|parso|kharch|kharcha|kiye|kiya|diye|diya|ke liye|par|ko)\b', ' ', clean)
    clean = re.sub(r'[^\w\s]', ' ', clean)
    clean = re.sub(r'\s+', ' ', clean).strip()
    description = clean.capitalize() if clean else category

    return {
        "amount": amt,
        "type": tx_type,
        "category": category,
        "description": description,
        "dateLabel": date_label,
        "rawVoiceText": text
    }

class SmartExpenseHandler(http.server.SimpleHTTPRequestHandler):
    def _send_json(self, data, status=200):
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization')
        self.end_headers()
        self.wfile.write(json.dumps(data).encode('utf-8'))

    def do_OPTIONS(self):
        self._send_json({"status": "OK"})

    def do_GET(self):
        parsed_url = urllib.parse.urlparse(self.path)
        path = parsed_url.path

        if path == '/api/health':
            self._send_json({ "status": "ONLINE", "app": "Voice Expense Manager API (Python Server)", "version": "1.0.0" })

        elif path == '/api/auth/profile':
            self._send_json({ "success": True, "data": users[0] })

        elif path == '/api/transactions':
            self._send_json({
                "success": True,
                "count": len(transactions),
                "total": len(transactions),
                "data": sorted(transactions, key=lambda x: x.get('transactionDate', ''), reverse=True)
            })

        elif path == '/api/budgets':
            now = datetime.datetime.now()
            # Calculate spent per category
            cat_spent = {}
            for t in transactions:
                if t.get('type') == 'expense':
                    c = t.get('category', 'Other')
                    cat_spent[c] = cat_spent.get(c, 0.0) + float(t.get('amount', 0))

            enriched = []
            for b in budgets:
                spent = cat_spent.get(b['category'], 0.0)
                remaining = max(0.0, b['monthlyLimit'] - spent)
                pct = round((spent / b['monthlyLimit']) * 100, 1) if b['monthlyLimit'] > 0 else 0
                status = "EXCEEDED" if pct >= 100 else ("WARNING" if pct >= 80 else "HEALTHY")
                enriched.append({
                    **b,
                    "spent": spent,
                    "remaining": remaining,
                    "percentageUsed": pct,
                    "status": status
                })
            self._send_json({ "success": True, "month": now.month, "year": now.year, "data": enriched })

        elif path == '/api/analytics/summary':
            total_income = sum(t['amount'] for t in transactions if t.get('type') == 'income')
            total_expense = sum(t['amount'] for t in transactions if t.get('type') == 'expense')
            self._send_json({
                "success": True,
                "data": {
                    "totalIncome": total_income,
                    "totalExpense": total_expense,
                    "balance": total_income - total_expense,
                    "todaySpending": 300.0,
                    "thisMonthSpending": total_expense,
                    "monthlyBudgetTotal": sum(b['monthlyLimit'] for b in budgets),
                    "remainingBudget": max(0.0, sum(b['monthlyLimit'] for b in budgets) - total_expense),
                    "currency": "₹"
                }
            })

        elif path == '/api/analytics/categories':
            cat_map = {}
            total = 0.0
            for t in transactions:
                if t.get('type') == 'expense':
                    c = t.get('category', 'Other')
                    amt = float(t.get('amount', 0))
                    cat_map[c] = cat_map.get(c, 0.0) + amt
                    total += amt

            data = [
                {"category": k, "amount": v, "percentage": round((v / total) * 100, 1) if total > 0 else 0}
                for k, v in sorted(cat_map.items(), key=lambda item: item[1], reverse=True)
            ]
            self._send_json({ "success": True, "totalExpense": total, "data": data })

        elif path == '/api/analytics/monthly':
            self._send_json({
                "success": True,
                "data": [
                    {"monthName": "Jul", "month": 7, "year": 2026, "income": 45000, "expense": 18000},
                    {"monthName": "Aug", "month": 8, "year": 2026, "income": 48000, "expense": 21000},
                    {"monthName": "Sep", "month": 9, "year": 2026, "income": 50000, "expense": 19500},
                    {"monthName": "Oct", "month": 10, "year": 2026, "income": 52000, "expense": 24000},
                    {"monthName": "Nov", "month": 11, "year": 2026, "income": 50000, "expense": 3050}
                ],
                "insight": "You spent 15% less this month compared to last month. Great job!"
            })

        else:
            self._send_json({"success": False, "message": "Not Found"}, status=404)

    def do_POST(self):
        parsed_url = urllib.parse.urlparse(self.path)
        path = parsed_url.path
        length = int(self.headers.get('Content-Length', 0))
        body = json.loads(self.rfile.read(length).decode('utf-8')) if length > 0 else {}

        if path == '/api/auth/login':
            email = body.get('email', '')
            self._send_json({
                "success": True,
                "message": "Login successful",
                "data": {
                    "_id": "demo_user_001",
                    "name": "Priya Sharma",
                    "email": email,
                    "preferredLanguage": "en",
                    "token": "demo_jwt_token_2026"
                }
            })

        elif path == '/api/auth/register':
            name = body.get('name', 'User')
            email = body.get('email', 'user@example.com')
            lang = body.get('preferredLanguage', 'en')
            self._send_json({
                "success": True,
                "message": "Registration successful",
                "data": {
                    "_id": f"user_{int(datetime.datetime.now().timestamp())}",
                    "name": name,
                    "email": email,
                    "preferredLanguage": lang,
                    "token": "demo_jwt_token_2026"
                }
            }, status=201)

        elif path == '/api/transactions':
            if isinstance(body, list):
                # Batch sync
                for item in body:
                    item['_id'] = item.get('_id', f"tx_{int(datetime.datetime.now().timestamp()*1000)}")
                    item['syncStatus'] = 'synced'
                    transactions.insert(0, item)
                self._send_json({"success": True, "count": len(body), "data": body}, status=201)
            else:
                body['_id'] = f"tx_{int(datetime.datetime.now().timestamp()*1000)}"
                body['syncStatus'] = 'synced'
                body['transactionDate'] = body.get('transactionDate', datetime.datetime.now().isoformat())
                transactions.insert(0, body)
                self._send_json({"success": True, "message": "Transaction created", "data": body}, status=201)

        elif path == '/api/voice/parse':
            text = body.get('text', '')
            parsed = parse_voice_text(text)
            self._send_json({
                "success": True,
                "isComplete": parsed['amount'] > 0,
                "data": parsed,
                "suggestedSpeechResponse": f"Recorded ₹{int(parsed['amount'])} for {parsed['description']} under {parsed['category']}."
            })

        elif path == '/api/voice/query':
            query = body.get('query', '').lower()
            amt = sum(t['amount'] for t in transactions if t.get('type') == 'expense' and t.get('category', '').lower() == 'food')
            ans = f"You spent ₹{int(amt)} on food this month."
            self._send_json({
                "success": True,
                "query": query,
                "spokenAnswer": ans,
                "data": { "amount": amt, "category": "Food" }
            })

        elif path == '/api/budgets':
            cat = body.get('category', 'Other')
            limit = float(body.get('monthlyLimit', 1000))
            bgt = {
                "_id": f"bgt_{cat}",
                "userId": "demo_user_001",
                "category": cat,
                "monthlyLimit": limit,
                "month": datetime.datetime.now().month,
                "year": datetime.datetime.now().year
            }
            budgets.append(bgt)
            self._send_json({"success": True, "data": bgt}, status=201)

        else:
            self._send_json({"success": False, "message": "Not Found"}, status=404)

if __name__ == '__main__':
    with socketserver.TCPServer(("", PORT), SmartExpenseHandler) as httpd:
        print("=" * 60)
        print(f"🚀 Smart Expense Manager Backend is running on port {PORT}")
        print(f"📍 Local access: http://localhost:{PORT}")
        print(f"📱 Android Emulator access: http://10.0.2.2:{PORT}")
        print("=" * 60)
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nServer stopped.")

