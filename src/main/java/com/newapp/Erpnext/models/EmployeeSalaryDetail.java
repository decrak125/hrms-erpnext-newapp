package com.newapp.Erpnext.models;

import java.util.HashMap;
import java.util.Map;

public class EmployeeSalaryDetail {
    private String employeeId;
    private String employeeName;
    private double totalSalary;
    private Map<String, Double> salaryComponentDetails;

    public EmployeeSalaryDetail(String employeeId, String employeeName) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.totalSalary = 0.0;
        this.salaryComponentDetails = new HashMap<>();
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public double getTotalSalary() {
        return totalSalary;
    }

    public void setTotalSalary(double totalSalary) {
        this.totalSalary = totalSalary;
    }

    public Map<String, Double> getSalaryComponentDetails() {
        return salaryComponentDetails;
    }

    public void setSalaryComponentDetails(Map<String, Double> salaryComponentDetails) {
        this.salaryComponentDetails = salaryComponentDetails;
    }
}