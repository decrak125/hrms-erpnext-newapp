package com.newapp.Erpnext.models;

import java.time.LocalDateTime;

public class PurchaseOrder {
    private String name;
    private String status;
    private LocalDateTime creation;
    private String supplier;
    private String company;
    private double percentReceived;

    public PurchaseOrder() {
    }

    public PurchaseOrder(String name, String status, LocalDateTime creation, String supplier, String company) {
        this.name = name;
        this.status = status;
        this.creation = creation;
        this.supplier = supplier;
        this.company = company;
        this.percentReceived = 0.0;
    }

    // Getters and Setters
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

    public double getPercentReceived() {
        return percentReceived;
    }

    public void setPercentReceived(double percentReceived) {
        this.percentReceived = percentReceived;
    }
}