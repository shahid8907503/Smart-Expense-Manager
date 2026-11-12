package com.smart.expense.controller;

import com.smart.expense.model.Transaction;
import com.smart.expense.repository.BudgetRepository;
import com.smart.expense.repository.TransactionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class WebDashboardController {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;

    public WebDashboardController(TransactionRepository transactionRepository, BudgetRepository budgetRepository) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        List<Transaction> transactions = transactionRepository.findAllByOrderByCreatedAtDesc();

        double totalIncome = transactions.stream()
                .filter(t -> "income".equalsIgnoreCase(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
        double totalExpense = transactions.stream()
                .filter(t -> "expense".equalsIgnoreCase(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();

        model.addAttribute("transactions", transactions);
        model.addAttribute("budgets", budgetRepository.findAll());
        model.addAttribute("totalIncome", totalIncome);
        model.addAttribute("totalExpense", totalExpense);
        model.addAttribute("netBalance", totalIncome - totalExpense);
        return "dashboard";
    }

    @PostMapping("/web/transactions/add")
    public String addTransactionFromWeb(
            @RequestParam String description,
            @RequestParam Double amount,
            @RequestParam String type,
            @RequestParam String category,
            @RequestParam(defaultValue = "UPI") String paymentMethod,
            @RequestParam(defaultValue = "EXTERNAL") String paymentType
    ) {
        transactionRepository.save(new Transaction(description, amount, type, category, paymentMethod, paymentType));
        return "redirect:/";
    }
}

