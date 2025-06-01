package com.newapp.Erpnext.models;

import java.time.LocalDate;
import java.util.List;

public class Employee {
    private String id;
    private String name;
    private String department;
    private String position;
    private String email;
    private String phone;
    private String address;
    private LocalDate hireDate;
    private String status; // Active, On Leave, Terminated, etc.
    private String contractType;
    private List<Salary> salaries;
    
    // Constructeurs
    public Employee() {
    }
    
    public Employee(String id, String name, String department, String position, 
                   String email, String phone, String address, LocalDate hireDate, 
                   String status, String contractType) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.position = position;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.hireDate = hireDate;
        this.status = status;
        this.contractType = contractType;
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
    
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public String getPosition() {
        return position;
    }
    
    public void setPosition(String position) {
        this.position = position;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public LocalDate getHireDate() {
        return hireDate;
    }
    
    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getContractType() {
        return contractType;
    }
    
    public void setContractType(String contractType) {
        this.contractType = contractType;
    }
    
    public List<Salary> getSalaries() {
        return salaries;
    }
    
    public void setSalaries(List<Salary> salaries) {
        this.salaries = salaries;
    }
}