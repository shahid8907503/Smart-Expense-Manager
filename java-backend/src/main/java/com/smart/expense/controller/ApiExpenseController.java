package com.smart.expense.controller;

import com.smart.expense.model.Budget;
import com.smart.expense.model.Transaction;
import com.smart.expense.repository.BudgetRepository;
import com.smart.expense.repository.TransactionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        res.put("status", "UP");
        res.put("stack", "Java 21 + Spring Boot 3 + Spring Data JPA + H2 SQL");
        return res;
    }

    @GetMapping("/transactions")
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/transactions")
    public Transaction createTransaction(@RequestBody Transaction tx) {
        return transactionRepository.save(tx);
    }

    @DeleteMapping("/transactions/{id}")
    public void deleteTransaction(@PathVariable Long id) {
        transactionRepository.deleteById(id);
    }

    @GetMapping("/budgets")
    public List<Budget> getAllBudgets() {
        return budgetRepository.findAll();
    }

    @PostMapping("/budgets")
    public Budget createBudget(@RequestBody Budget budget) {
        return budgetRepository.save(budget);
    }

    @GetMapping("/analytics/summary")
    public Map<String, Object> getSummary() {
        List<Transaction> all = transactionRepository.findAll();
        double totalIncome = all.stream()
                .filter(t -> "income".equalsIgnoreCase(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
        double totalExpense = all.stream()
                .filter(t -> "expense".equalsIgnoreCase(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpense", totalExpense);
        summary.put("netBalance", totalIncome - totalExpense);
        summary.put("transactionCount", all.size());
        return summary;
    }
}

