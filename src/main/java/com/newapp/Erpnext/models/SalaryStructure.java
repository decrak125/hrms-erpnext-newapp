package com.newapp.Erpnext.models;

import java.util.List;

public class SalaryStructure {
    private String name;
    private String company;
    private List<SalaryComponent> salaryComponents;
    public SalaryStructure() {
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getCompany() {
        return company;
    }
    public void setCompany(String company) {
        this.company = company;
    }
    public List<SalaryComponent> getSalaryComponents() {
        return salaryComponents;
    }
    public void setSalaryComponents(List<SalaryComponent> salaryComponents) {
        this.salaryComponents = salaryComponents;
    }
    public SalaryStructure(String name, String company, List<SalaryComponent> salaryComponents) {
        this.name = name;
        this.company = company;
        this.salaryComponents = salaryComponents;
    }

}