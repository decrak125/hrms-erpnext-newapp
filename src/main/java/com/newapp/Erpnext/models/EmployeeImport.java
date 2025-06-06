package com.newapp.Erpnext.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public class EmployeeImport {
    private Integer ref;
    private String first_name;
    private String last_name;
    private String gender;
    private LocalDate hire_date;
    private LocalDate date_of_birth;
    private String company;
    private String name;

    @JsonProperty("ref")
    public Integer getRef() {
        return ref;
    }

    public void setRef(Integer ref) {
        this.ref = ref;
    }

    @JsonProperty("first_name")
    public String getFirstName() {
        return first_name;
    }

    public void setFirstName(String first_name) {
        this.first_name = first_name;
    }

    @JsonProperty("last_name")
    public String getLastName() {
        return last_name;
    }

    public void setLastName(String last_name) {
        this.last_name = last_name;
    }

    @JsonProperty("gender")
    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    @JsonProperty("date_of_joining")
    public LocalDate getHireDate() {
        return hire_date;
    }

    public void setHireDate(LocalDate hire_date) {
        this.hire_date = hire_date;
    }

    @JsonProperty("date_of_birth")
    public LocalDate getDateOfBirth() {
        return date_of_birth;
    }

    public void setDateOfBirth(LocalDate date_of_birth) {
        this.date_of_birth = date_of_birth;
    }

    @JsonProperty("company")
    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}