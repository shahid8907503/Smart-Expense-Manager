package com.smart.expense.controller;

import com.smart.expense.model.Budget;
import com.smart.expense.model.Transaction;
import com.smart.expense.repository.BudgetRepository;
import com.smart.expense.repository.TransactionRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiExpenseController {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;

    public ApiExpenseController(TransactionRepository transactionRepository, BudgetRepository budgetRepository) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> res = new HashMap<>();
        res.put("status", "ONLINE");
        res.put("stack", "Java 21 + Spring Boot 3 + Spring Data JPA + H2 SQL + Thymeleaf");
        res.put("timestamp", java.time.Instant.now().toString());
        return res;
    }

    // ---- Transactions ----

    @GetMapping("/transactions")
    public Map<String, Object> getAllTransactions(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search
    ) {
        List<Transaction> list = transactionRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(t -> type == null || type.isEmpty() || type.equalsIgnoreCase(t.getType()))
                .filter(t -> category == null || category.isEmpty() || category.equalsIgnoreCase(t.getCategory()))
                .filter(t -> search == null || search.isEmpty() || t.getDescription().toLowerCase().contains(search.toLowerCase()))
                .collect(Collectors.toList());

        List<Map<String, Object>> dtoList = list.stream().map(this::toTransactionDto).collect(Collectors.toList());

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("count", dtoList.size());
        res.put("total", dtoList.size());
        res.put("data", dtoList);
        return res;
    }

    @PostMapping("/transactions")
    public Map<String, Object> createTransaction(@RequestBody Map<String, Object> req) {
        double amount = req.get("amount") != null ? Double.parseDouble(req.get("amount").toString()) : 0.0;
        String type = String.valueOf(req.getOrDefault("type", "expense"));
        String category = String.valueOf(req.getOrDefault("category", "Other"));
        String description = String.valueOf(req.getOrDefault("description", "Expense"));
        String paymentMethod = String.valueOf(req.getOrDefault("paymentMethod", "UPI"));
        String paymentType = String.valueOf(req.getOrDefault("paymentType", "EXTERNAL"));

        Transaction saved = transactionRepository.save(
                new Transaction(description, amount, type, category, paymentMethod, paymentType)
        );

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Saved to Java H2 SQL DB");
        res.put("data", toTransactionDto(saved));
        return res;
    }

    @PutMapping("/transactions/{id}")
    public Map<String, Object> updateTransaction(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        Transaction tx = transactionRepository.findById(id).orElse(new Transaction());
        if (req.containsKey("amount")) tx.setAmount(Double.parseDouble(req.get("amount").toString()));
        if (req.containsKey("type")) tx.setType(String.valueOf(req.get("type")));
        if (req.containsKey("category")) tx.setCategory(String.valueOf(req.get("category")));
        if (req.containsKey("description")) tx.setDescription(String.valueOf(req.get("description")));
        if (req.containsKey("paymentMethod")) tx.setPaymentMethod(String.valueOf(req.get("paymentMethod")));
        Transaction saved = transactionRepository.save(tx);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("data", toTransactionDto(saved));
        return res;
    }

    @DeleteMapping("/transactions/{id}")
    public void deleteTransaction(@PathVariable Long id) {
        if (transactionRepository.existsById(id)) {
            transactionRepository.deleteById(id);
        }
    }

    // ---- Budgets ----

    @GetMapping("/budgets")
    public Map<String, Object> getAllBudgets(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        int m = month != null ? month : LocalDate.now().getMonthValue();
        int y = year != null ? year : LocalDate.now().getYear();

        List<Transaction> allTx = transactionRepository.findAll();
        List<Map<String, Object>> budgetDtos = budgetRepository.findAll().stream().map(b -> {
            double spent = allTx.stream()
                    .filter(t -> "expense".equalsIgnoreCase(t.getType()) && b.getCategory().equalsIgnoreCase(t.getCategory()))
                    .mapToDouble(Transaction::getAmount).sum();
            double limit = b.getMonthlyLimit() != null ? b.getMonthlyLimit() : 0.0;
            double pct = limit > 0 ? (spent / limit) * 100.0 : 0.0;
            String status = pct >= 100 ? "EXCEEDED" : (pct >= 80 ? "WARNING" : "HEALTHY");

            Map<String, Object> dto = new HashMap<>();
            dto.put("_id", String.valueOf(b.getId()));
            dto.put("userId", "1");
            dto.put("category", b.getCategory());
            dto.put("monthlyLimit", limit);
            dto.put("month", m);
            dto.put("year", y);
            dto.put("alertThresholdPercent", 80);
            dto.put("spent", spent);
            dto.put("remaining", Math.max(0.0, limit - spent));
            dto.put("percentageUsed", pct);
            dto.put("status", status);
            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("month", m);
        res.put("year", y);
        res.put("data", budgetDtos);
        return res;
    }

    @PostMapping("/budgets")
    public Map<String, Object> createBudget(@RequestBody Map<String, Object> req) {
        String category = String.valueOf(req.getOrDefault("category", "Other"));
        double limit = req.get("monthlyLimit") != null ? Double.parseDouble(req.get("monthlyLimit").toString()) : 5000.0;
        String periodType = String.valueOf(req.getOrDefault("periodType", "MONTHLY"));

        Budget saved = budgetRepository.save(new Budget(category, limit, periodType));
        Map<String, Object> dto = new HashMap<>();
        dto.put("_id", String.valueOf(saved.getId()));
        dto.put("userId", "1");
        dto.put("category", saved.getCategory());
        dto.put("monthlyLimit", saved.getMonthlyLimit());
        dto.put("month", LocalDate.now().getMonthValue());
        dto.put("year", LocalDate.now().getYear());
        return dto;
    }

    @DeleteMapping("/budgets/{id}")
    public void deleteBudget(@PathVariable Long id) {
        if (budgetRepository.existsById(id)) {
            budgetRepository.deleteById(id);
        }
    }

    // ---- Analytics ----

    @GetMapping("/analytics/summary")
    public Map<String, Object> getSummary() {
        List<Transaction> all = transactionRepository.findAll();
        double totalIncome = all.stream().filter(t -> "income".equalsIgnoreCase(t.getType())).mapToDouble(Transaction::getAmount).sum();
        double totalExpense = all.stream().filter(t -> "expense".equalsIgnoreCase(t.getType())).mapToDouble(Transaction::getAmount).sum();
        double budgetTotal = budgetRepository.findAll().stream().mapToDouble(b -> b.getMonthlyLimit() != null ? b.getMonthlyLimit() : 0.0).sum();

        Map<String, Object> summaryData = new HashMap<>();
        summaryData.put("totalIncome", totalIncome);
        summaryData.put("totalExpense", totalExpense);
        summaryData.put("balance", totalIncome - totalExpense);
        summaryData.put("todaySpending", totalExpense);
        summaryData.put("thisMonthSpending", totalExpense);
        summaryData.put("monthlyBudgetTotal", budgetTotal);
        summaryData.put("remainingBudget", Math.max(0.0, budgetTotal - totalExpense));
        summaryData.put("currency", "₹");

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("data", summaryData);
        return res;
    }

    @GetMapping("/analytics/categories")
    public Map<String, Object> getCategoryBreakdown() {
        List<Transaction> expenses = transactionRepository.findAll().stream()
                .filter(t -> "expense".equalsIgnoreCase(t.getType()))
                .collect(Collectors.toList());
        double totalExpense = expenses.stream().mapToDouble(Transaction::getAmount).sum();

        Map<String, Double> grouped = expenses.stream()
                .collect(Collectors.groupingBy(Transaction::getCategory, Collectors.summingDouble(Transaction::getAmount)));

        List<Map<String, Object>> breakdown = grouped.entrySet().stream().map(e -> {
            Map<String, Object> item = new HashMap<>();
            item.put("category", e.getKey());
            item.put("amount", e.getValue());
            item.put("percentage", totalExpense > 0 ? (e.getValue() / totalExpense) * 100.0 : 0.0);
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("totalExpense", totalExpense);
        res.put("month", LocalDate.now().getMonthValue());
        res.put("year", LocalDate.now().getYear());
        res.put("data", breakdown);
        return res;
    }

    @GetMapping("/analytics/monthly")
    public Map<String, Object> getMonthlyTrends() {
        LocalDate now = LocalDate.now();
        List<Transaction> all = transactionRepository.findAll();
        double totalIncome = all.stream().filter(t -> "income".equalsIgnoreCase(t.getType())).mapToDouble(Transaction::getAmount).sum();
        double totalExpense = all.stream().filter(t -> "expense".equalsIgnoreCase(t.getType())).mapToDouble(Transaction::getAmount).sum();

        Map<String, Object> bucket = new HashMap<>();
        bucket.put("monthName", Month.of(now.getMonthValue()).name().substring(0, 3));
        bucket.put("month", now.getMonthValue());
        bucket.put("year", now.getYear());
        bucket.put("income", totalIncome);
        bucket.put("expense", totalExpense);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("data", Collections.singletonList(bucket));
        res.put("insight", "Calculated in real time by Java Spring Boot Analytics Engine.");
        return res;
    }

    private Map<String, Object> toTransactionDto(Transaction t) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("_id", String.valueOf(t.getId()));
        dto.put("userId", "1");
        dto.put("amount", t.getAmount());
        dto.put("type", t.getType());
        dto.put("category", t.getCategory());
        dto.put("description", t.getDescription());
        dto.put("transactionDate", t.getCreatedAt().toString());
        dto.put("paymentMethod", t.getPaymentMethod());
        dto.put("source", "api");
        dto.put("syncStatus", "synced");
        dto.put("createdAt", t.getCreatedAt().toString());
        return dto;
    }
}
