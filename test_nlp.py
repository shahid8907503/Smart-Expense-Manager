import re
import datetime

# Categories
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

INCOME_WORDS = ['salary', 'income', 'received', 'got', 'earned', 'credited', 'mili', 'mila', 'aamadni', 'vachindi', 'sampadinchanu', 'jeetham']

def categorize(text):
    lower = text.lower()
    for cat, kws in CATEGORIES.items():
        for kw in kws:
            if re.search(r'\b' + re.escape(kw) + r'\b', lower):
                return cat
    return 'Other'

def extract_amount(text):
    # Regex 1: ₹ or Rs followed by digits
    m = re.search(r'(?:₹|rs\.?|inr)\s*([\d,]+(?:\.\d+)?)', text, re.IGNORECASE)
    if m:
        return float(m.group(1).replace(',', ''))
    # Regex 2: digits followed by rupees/rupaye/rupayalu
    m = re.search(r'([\d,]+(?:\.\d+)?)\s*(?:₹|rs\.?|inr|rupees|rupee|rupaye|rupayalu|rupai)', text, re.IGNORECASE)
    if m:
        return float(m.group(1).replace(',', ''))
    # Regex 3: standalone digits
    m = re.search(r'\b(\d+(?:\.\d+)?)\b', text)
    if m:
        return float(m.group(1))
    return None

def extract_type(text):
    lower = text.lower()
    for kw in INCOME_WORDS:
        if kw in lower:
            return 'income'
    return 'expense'

def extract_date(text):
    lower = text.lower()
    if 'day before yesterday' in lower or 'parso' in lower or 'monna' in lower:
        return 'Day before yesterday'
    if 'yesterday' in lower or 'kal' in lower or 'ninna' in lower:
        return 'Yesterday'
    return 'Today'

def extract_description(text, amount, category):
    clean = text
    if amount:
        clean = re.sub(r'\b' + str(int(amount)) + r'\b', '', clean)
    clean = re.sub(r'(?i)\b(i|have|spent|paid|bought|purchased|received|got|for|on|as|in|at|a|an|the|rupees|rupee|rs|inr|rupaye|rupayalu|rupai|today|yesterday)\b', ' ', clean)
    clean = re.sub(r'(?i)\b(nenu|ninna|eeroju|monna|kharchu|chesanu|ichanu|pettanu|kosam|ki|lo|vachindi)\b', ' ', clean)
    clean = re.sub(r'(?i)\b(maine|aaj|kal|parso|kharch|kharcha|kiye|kiya|diye|diya|ke liye|par|ko)\b', ' ', clean)
    clean = re.sub(r'[^\w\s]', ' ', clean)
    clean = re.sub(r'\s+', ' ', clean).strip()
    return clean.capitalize() if clean else category

# Test test cases
test_cases = [
    "I spent 250 rupees on lunch today.",
    "Yesterday I paid 100 rupees for petrol.",
    "I purchased a shirt for 1200 rupees.",
    "I received 5000 rupees as salary.",
    "Today I spent 50 rupees on tea.",
    "Nenu food kosam 200 rupayalu kharchu chesanu.",
    "Maine khane par 200 rupaye kharch kiye."
]

print("=== MULTILINGUAL NLP VERIFICATION TEST ===")
for tc in test_cases:
    amt = extract_amount(tc)
    tx_type = extract_type(tc)
    dt = extract_date(tc)
    cat = 'Salary' if tx_type == 'income' else categorize(tc)
    desc = extract_description(tc, amt, cat)
    print(f"Input: \"{tc}\"")
    print(f"  -> Amount: ₹{amt} | Type: {tx_type.upper()} | Category: {cat} | Date: {dt} | Description: {desc}")
    print("-" * 50)
