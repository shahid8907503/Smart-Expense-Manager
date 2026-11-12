package com.smart.expense.controller;

import com.smart.expense.model.Transaction;
import com.smart.expense.repository.TransactionRepository;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/voice")
@CrossOrigin(origins = "*")
public class VoiceController {

    private final TransactionRepository transactionRepository;

    public VoiceController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @PostMapping("/parse")
    public Map<String, Object> parseVoice(@RequestBody Map<String, Object> req) {
        String text = String.valueOf(req.getOrDefault("text", ""));
        double amount = 0.0;
        Matcher m = Pattern.compile("(\\d+(\\.\\d+)?)").matcher(text);
        if (m.find()) {
            amount = Double.parseDouble(m.group(1));
        }

        String lower = text.toLowerCase();
        String type = (lower.contains("salary") || lower.contains("received") || lower.contains("income")) ? "income" : "expense";
        String category = lower.contains("food") || lower.contains("lunch") ? "Food" :
                (lower.contains("travel") || lower.contains("uber") || lower.contains("metro") ? "Travel" : "Other");

        Map<String, Object> data = new HashMap<>();
        data.put("amount", amount);
        data.put("type", type);
        data.put("category", category);
        data.put("description", text.isEmpty() ? category : text);
        data.put("transactionDate", Instant.now().toString());
        data.put("dateLabel", "Today");
        data.put("rawVoiceText", text);
        data.put("source", "voice");
        data.put("confidence", 0.95);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("isComplete", amount > 0);
        res.put("data", data);
        res.put("suggestedSpeechResponse", "Parsed ₹" + (int) amount + " under " + category);
        return res;
    }

    @PostMapping("/query")
    public Map<String, Object> queryVoice(@RequestBody Map<String, Object> req) {
        String query = String.valueOf(req.getOrDefault("query", ""));
        List<Transaction> all = transactionRepository.findAll();
        double totalSpent = all.stream().filter(t -> "expense".equalsIgnoreCase(t.getType())).mapToDouble(Transaction::getAmount).sum();

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("intent", "TOTAL_SPENT");
        resultData.put("dateRange", "THIS_MONTH");
        resultData.put("amount", totalSpent);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("query", query);
        res.put("spokenAnswer", "Your total recorded expense on the Java SQL server is ₹" + String.format("%.2f", totalSpent));
        res.put("data", resultData);
        return res;
    }
}

