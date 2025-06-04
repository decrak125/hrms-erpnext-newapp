package com.newapp.Erpnext.models;

import java.time.LocalDate;

public class SalaryStructureAssignment {
    private LocalDate from_date;
    private int employee_ref;
    private double base;
    private String salary_structure;
    public SalaryStructureAssignment() {
    }
    public LocalDate getFrom_date() {
        return from_date;
    }
    public void setFrom_date(LocalDate from_date) {
        this.from_date = from_date;
    }
    public int getEmployee_ref() {
        return employee_ref;
    }
    public void setEmployee_ref(int employee_ref) {
        this.employee_ref = employee_ref;
    }
    public double getBase() {
        return base;
    }
    public void setBase(double base) {
        this.base = base;
    }
    public String getSalary_structure() {
        return salary_structure;
    }
    public void setSalary_structure(String salary_structure) {
        this.salary_structure = salary_structure;
    }
    public SalaryStructureAssignment(LocalDate from_date, int employee_ref, double base, String salary_structure) {
        this.from_date = from_date;
        this.employee_ref = employee_ref;
        this.base = base;
        this.salary_structure = salary_structure;
    }

}