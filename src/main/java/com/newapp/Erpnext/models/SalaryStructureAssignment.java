package com.newapp.Erpnext.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public class SalaryStructureAssignment {
    private LocalDate from_date;
    private Integer employee_ref;
    private Double base;
    private String salary_structure;
    private String company;

    @JsonProperty("from_date")
    public LocalDate getFrom_date() {
        return from_date;
    }

    public void setFrom_date(LocalDate from_date) {
        this.from_date = from_date;
    }

    @JsonProperty("employee_ref")
    public Integer getEmployee_ref() {
        return employee_ref;
    }

    public void setEmployee_ref(Integer employee_ref) {
        this.employee_ref = employee_ref;
    }

    @JsonProperty("base")
    public Double getBase() {
        return base;
    }

    public void setBase(Double base) {
        this.base = base;
    }

    @JsonProperty("salary_structure")
    public String getSalary_structure() {
        return salary_structure;
    }

    public void setSalary_structure(String salary_structure) {
        this.salary_structure = salary_structure;
    }

    @JsonProperty("company")
    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }
}