package com.newapp.Erpnext.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tabEmployee")
public class EmployeeEntity {
    @Id
    @Column(name = "name") // Frappe utilise "name" comme clé primaire
    private String name;
    
    @Column(name = "employee_name")
    private String employeeName;
    
    @Column(name = "employee_number")
    private String employeeNumber;
    
    @Column(name = "company")
    private String company;
    
    @Column(name = "department")
    private String department;
    
    @Column(name = "designation")
    private String designation;
    
    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "personal_email")
    private String personalEmail;
    
    @Column(name = "cell_number")
    private String cellNumber;
    
    @Column(name = "creation")
    private LocalDateTime creation;
    
    @Column(name = "modified")
    private LocalDateTime modified;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public LocalDate getDateOfJoining() {
        return dateOfJoining;
    }

    public void setDateOfJoining(LocalDate dateOfJoining) {
        this.dateOfJoining = dateOfJoining;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPersonalEmail() {
        return personalEmail;
    }

    public void setPersonalEmail(String personalEmail) {
        this.personalEmail = personalEmail;
    }

    public String getCellNumber() {
        return cellNumber;
    }

    public void setCellNumber(String cellNumber) {
        this.cellNumber = cellNumber;
    }

    public LocalDateTime getCreation() {
        return creation;
    }

    public void setCreation(LocalDateTime creation) {
        this.creation = creation;
    }

    public LocalDateTime getModified() {
        return modified;
    }

    public void setModified(LocalDateTime modified) {
        this.modified = modified;
    }

    public EmployeeEntity(String name, String employeeName, String employeeNumber, String company, String department,
            String designation, LocalDate dateOfJoining, String status, String personalEmail, String cellNumber,
            LocalDateTime creation, LocalDateTime modified) {
        this.name = name;
        this.employeeName = employeeName;
        this.employeeNumber = employeeNumber;
        this.company = company;
        this.department = department;
        this.designation = designation;
        this.dateOfJoining = dateOfJoining;
        this.status = status;
        this.personalEmail = personalEmail;
        this.cellNumber = cellNumber;
        this.creation = creation;
        this.modified = modified;
    }

    public EmployeeEntity() {
    }

      @Override
    public String toString() {
        return "Employee{name='" + name + "', employeeName='" + employeeName + "'}";
    }


    
}
