package com.newapp.Erpnext.models;

public class SalaryComponent {
    private String salary_component;
    private String salary_component_abbr;
    private String type;
    private String formula;
    public String getSalary_component() {
        return salary_component;
    }
    public void setSalary_component(String salary_component) {
        this.salary_component = salary_component;
    }
    public String getSalary_component_abbr() {
        return salary_component_abbr;
    }
    public void setSalary_component_abbr(String salary_component_abbr) {
        this.salary_component_abbr = salary_component_abbr;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getFormula() {
        return formula;
    }
    public void setFormula(String formula) {
        this.formula = formula;
    }
    public SalaryComponent(String salary_component, String salary_component_abbr, String type, String formula) {
        this.salary_component = salary_component;
        this.salary_component_abbr = salary_component_abbr;
        this.type = type;
        this.formula = formula;
    }
    public SalaryComponent() {
    }
}
