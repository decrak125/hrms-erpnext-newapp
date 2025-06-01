package com.newapp.Erpnext.models;

import java.time.LocalDate;
import java.math.BigDecimal;

public class Salary {
    private String id;
    private String employeeId;
    private LocalDate paymentDate;
    private String month; // Format: "YYYY-MM"
    private BigDecimal grossAmount;
    private BigDecimal netAmount;
    private BigDecimal taxAmount;
    private String status; // Draft, Paid, etc.
    
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
}
