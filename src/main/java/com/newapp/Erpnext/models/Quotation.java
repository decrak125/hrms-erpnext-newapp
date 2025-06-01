package com.newapp.Erpnext.models;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

public class Quotation {
    private String name;
    private String supplier;
    private LocalDate transactionDate;
    private BigDecimal grandTotal;
    private String status;
    private List<QuotationItem> items;
    
    public Quotation(String name, String supplier, LocalDate transactionDate, BigDecimal grandTotal, String status) {
        this.name = name;
        this.supplier = supplier;
        this.transactionDate = transactionDate;
        this.grandTotal = grandTotal;
        this.status = status;
    }

    // Constructeur par défaut
    public Quotation() {
    }
    
    // Constructeur avec paramètres
    public Quotation(String name, String supplier, LocalDate transactionDate, BigDecimal grandTotal, String status, List<QuotationItem> items) {
        this.name = name;
        this.supplier = supplier;
        this.transactionDate = transactionDate;
        this.grandTotal = grandTotal;
        this.status = status;
        this.items = items;
    }
    
    // Getters et Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSupplier() {
        return supplier;
    }
    
    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }
    
    public LocalDate getTransactionDate() {
        return transactionDate;
    }
    
    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }
    
    public BigDecimal getGrandTotal() {
        return grandTotal;
    }
    
    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    // Getter et Setter pour items
    public List<QuotationItem> getItems() {
        return items;
    }

    public void setItems(List<QuotationItem> items) {
        this.items = items;
    }
}

// Nouvelle classe interne pour les items
