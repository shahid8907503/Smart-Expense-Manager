package com.smart.expense;

import com.smart.expense.model.Budget;
import com.smart.expense.model.Transaction;
import com.smart.expense.repository.BudgetRepository;
import com.smart.expense.repository.TransactionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SmartExpenseJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartExpenseJavaApplication.class, args);
    }

    @Bean
    CommandLineRunner seedDemoData(TransactionRepository txRepo, BudgetRepository budgetRepo) {
        return args -> {
            if (txRepo.count() == 0) {
                txRepo.save(new Transaction("Internship Stipend", 15000.0, "income", "Salary", "Net Banking", "EXTERNAL"));
                txRepo.save(new Transaction("College Canteen Lunch", 250.0, "expense", "Food & Dining", "UPI", "EXTERNAL"));
                txRepo.save(new Transaction("Metro Travel Pass", 500.0, "expense", "Transport", "Debit Card", "EXTERNAL"));
                txRepo.save(new Transaction("Savings Transfer", 2000.0, "expense", "Transfer", "UPI", "INTERNAL"));
            }
            if (budgetRepo.count() == 0) {
                budgetRepo.save(new Budget("Food & Dining", 5000.0, "MONTHLY"));
                budgetRepo.save(new Budget("Transport", 2000.0, "MONTHLY"));
                budgetRepo.save(new Budget("Shopping", 3000.0, "WEEKLY"));
            }
        };
    }
}

