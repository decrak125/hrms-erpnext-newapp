package com.newapp.Erpnext.models;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Map;

import org.springframework.cglib.core.Local;

import java.util.HashMap;

public class Salary {
    private String id;
    private String employeeId;
    private LocalDate paymentDate;
    private String month; // Format: "YYYY-MM"
    private BigDecimal grossAmount;
    private BigDecimal netAmount;
    private BigDecimal taxAmount;
    private LocalDate startDate;
    private LocalDate endDate;
     private String status; // Draft, Paid, etc.
    private Map<String, BigDecimal> earnings;
    private Map<String, BigDecimal> deductions;
    private String company; // Ajout des composants de salaire
    private String payrollFrequency; // Mensuel, Hebdomadaire, etc.
    private LocalDate postingDate;
    
    public LocalDate getPostingDate() {
        return postingDate;
    }

    public void setPostingDate(LocalDate postingDate) {
        this.postingDate = postingDate;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

   
    public Map<String, BigDecimal> getEarnings() {
        return earnings;
    }

    public void setEarnings(Map<String, BigDecimal> earnings) {
        this.earnings = earnings;
    }

    public Map<String, BigDecimal> getDeductions() {
        return deductions;
    }

    public void setDeductions(Map<String, BigDecimal> deductions) {
        this.deductions = deductions;
    }

    // Constructeurs
    public Salary() {
    }
    
    public Salary(String id, String employeeId, LocalDate paymentDate, String month, 
                 BigDecimal grossAmount, BigDecimal netAmount, BigDecimal taxAmount, 
                 String status) {
        this.id = id;
        this.employeeId = employeeId;
        this.paymentDate = paymentDate;
        this.month = month;
        this.grossAmount = grossAmount;
        this.netAmount = netAmount;
        this.taxAmount = taxAmount;
        this.status = status;
    }
    
    // Getters et Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
    
    public LocalDate getPaymentDate() {
        return paymentDate;
    }
    
    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }
    
    public String getMonth() {
        return month;
    }
    
    public void setMonth(String month) {
        this.month = month;
    }
    
    public BigDecimal getGrossAmount() {
        return grossAmount;
    }
    
    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }
    
    public BigDecimal getNetAmount() {
        return netAmount;
    }
    
    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }
    
    public BigDecimal getTaxAmount() {
        return taxAmount;
    }
    
    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }

    public String getPayrollFrequency() {
        return this.payrollFrequency != null ? this.payrollFrequency : "Monthly";
    }

    public void setPayrollFrequency(String payrollFrequency) {
        this.payrollFrequency = payrollFrequency;
    }

    
}
