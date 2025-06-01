package com.newapp.Erpnext.models;
import java.time.LocalDateTime;
import java.util.UUID;

public class Supplier {
    private String id;
    private String name;
    private String owner;
    private String supplierGroup;
    private String supplierType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructeur par défaut
    public Supplier() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.supplierType = "Company";
    }
    
    // Constructeur avec paramètres essentiels
    public Supplier(String name, String owner, String supplierGroup, String supplierType) {
        this();
        this.name = name;
        this.owner = owner;
        this.supplierGroup = supplierGroup;
        this.supplierType = supplierType;
    }
    
    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getSupplierGroup() {
        return supplierGroup;
    }

    public void setSupplierGroup(String supplierGroup) {
        this.supplierGroup = supplierGroup;
    }

    public String getSupplierType() {
        return supplierType;
    }

    public void setSupplierType(String supplierType) {
        this.supplierType = supplierType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Méthode pour mettre à jour la date de modification
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return name; // Simply return the supplier name
    }
}