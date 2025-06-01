package com.newapp.Erpnext.models;

import java.time.LocalDateTime;

public class PurchaseInvoice {
    private String name;
    private String status;
    private LocalDateTime creation;
    private String supplier;
    private String company;
    private boolean isPaid;

    public PurchaseInvoice() {
    }

    public PurchaseInvoice(String name, String status, LocalDateTime creation, String supplier, String company) {
        this.name = name;
        this.status = status;
        this.creation = creation;
        this.supplier = supplier;
        this.company = company;
        this.isPaid = "Paid".equals(status);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.isPaid = "Paid".equals(status);
    }

    public LocalDateTime getCreation() {
        return creation;
    }

    public void setCreation(LocalDateTime creation) {
        this.creation = creation;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }
}