package com.newapp.Erpnext.models;

import java.time.LocalDate;

public class PayrollRequest {
    private String embloyeeId;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    public PayrollRequest() {
    }
    public PayrollRequest(String embloyeeId, LocalDate dateDebut, LocalDate dateFin) {
        this.embloyeeId = embloyeeId;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }
    public String getEmbloyeeId() {
        return embloyeeId;
    }
    public void setEmbloyeeId(String embloyeeId) {
        this.embloyeeId = embloyeeId;
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

    
}
