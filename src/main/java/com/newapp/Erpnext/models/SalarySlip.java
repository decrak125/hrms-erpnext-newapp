package com.newapp.Erpnext.models;

import java.time.LocalDate;

public class SalarySlip {
    private String name;
    private String employee;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Double grossPay;
    private Double netPay;
    public double getNetPay() {
        return netPay;
    }
    public void setNetPay(double netPay) {
        this.netPay = netPay;
    }
    private String status;
    
    public SalarySlip() {
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getEmployee() {
        return employee;
    }
    public void setEmployee(String employee) {
        this.employee = employee;
    }
    public LocalDate getDateDebut() {
        return dateDebut;
    }
    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }
    public LocalDate getDateFin() {
        return dateFin;
    }
    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }
    public double getGrossPay() {
        return grossPay;
    }
    public void setGrossPay(double grossPay) {
        this.grossPay = grossPay;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public SalarySlip(String name, String employee, LocalDate dateDebut, LocalDate dateFin, double grossPay,
            String status) {
        this.name = name;
        this.employee = employee;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.grossPay = grossPay;
        this.status = status;
    }
    public Object getOrDefault(String string, double d) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getOrDefault'");
    }


}
