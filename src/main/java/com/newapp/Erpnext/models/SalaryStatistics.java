package com.newapp.Erpnext.models;

import java.util.Map;
import java.text.DateFormatSymbols;
import java.util.Locale;

public class SalaryStatistics {
    private Integer year;
    private Integer month;
    private Double totalSalary;
    private Map<String, Double> salaryComponentDetails;

    // Constructeurs
    public SalaryStatistics() {}

    public SalaryStatistics(Integer year, Integer month) {
        this.year = year;
        this.month = month;
    }

    // Getters et Setters
    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Double getTotalSalary() {
        return totalSalary;
    }

    public void setTotalSalary(Double totalSalary) {
        this.totalSalary = totalSalary;
    }

    public Map<String, Double> getSalaryComponentDetails() {
        return salaryComponentDetails;
    }

    public void setSalaryComponentDetails(Map<String, Double> salaryComponentDetails) {
        this.salaryComponentDetails = salaryComponentDetails;
    }

    public String getMonthName() {
        return new DateFormatSymbols(Locale.FRENCH).getMonths()[month - 1];
    }
}