package com.smart.expense.java_core;

/**
 * Pure Java Financial Computation Engine (JVM Core Module)
 * Provides EMI amortization and financial health calculation utilities.
 */
public class FinancialMathJavaService {

    /**
     * Computes monthly reducing-balance EMI:
     * EMI = [P * r * (1 + r)^n] / [(1 + r)^n - 1]
     */
    public static double calculateMonthlyEmi(double principal, double annualRatePercent, int tenureMonths) {
        if (tenureMonths <= 0 || principal <= 0) {
            return 0.0;
        }
        if (annualRatePercent <= 0) {
            return principal / tenureMonths;
        }

        double monthlyRate = (annualRatePercent / 12.0) / 100.0;
        double factor = Math.pow(1.0 + monthlyRate, tenureMonths);

        if (factor > 1.0) {
            return (principal * monthlyRate * factor) / (factor - 1.0);
        }
        return principal / tenureMonths;
    }

    /**
     * Computes total interest payable over the loan tenure.
     */
    public static double calculateTotalInterest(double principal, double monthlyEmi, int tenureMonths) {
        double totalPayment = monthlyEmi * tenureMonths;
        return Math.max(0.0, totalPayment - principal);
    }
}

