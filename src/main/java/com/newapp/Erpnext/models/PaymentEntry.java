package com.newapp.Erpnext.models;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class PaymentEntry {
    private String name;
    private String paymentType;
    private String party;
    private String partyType;
    private String company;
    private String mode;
    private Double paidAmount;
    private String reference;
    private LocalDateTime paymentDate;
    private String status;
    private String invoiceReference;
    private String modeOfPayment; // Add this field for proper mapping
    private String referenceNo;   // Add this field for proper mapping
    private String paidFrom;      // Add this field for account mapping
    private String paidTo;        // Add this field for account mapping

    public PaymentEntry() {
        this.paymentDate = LocalDateTime.now();
        this.status = "Draft";
        this.paymentType = "Pay";
        this.partyType = "Supplier";
        this.mode = "Cash";
        this.modeOfPayment = "Cash";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getParty() {
        return party;
    }

    public void setParty(String party) {
        this.party = party;
    }

    public String getPartyType() {
        return partyType;
    }

    public void setPartyType(String partyType) {
        this.partyType = partyType;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(Double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInvoiceReference() {
        return invoiceReference;
    }

    public void setInvoiceReference(String invoiceReference) {
        this.invoiceReference = invoiceReference;
    }

    public String getModeOfPayment() {
        return modeOfPayment;
    }

    public void setModeOfPayment(String modeOfPayment) {
        this.modeOfPayment = modeOfPayment;
        this.mode = modeOfPayment; // Keep both in sync
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
        this.reference = referenceNo; // Keep both in sync
    }

    public String getPaidFrom() {
        return paidFrom;
    }

    public void setPaidFrom(String paidFrom) {
        this.paidFrom = paidFrom;
    }

    public String getPaidTo() {
        return paidTo;
    }

    public void setPaidTo(String paidTo) {
        this.paidTo = paidTo;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("doctype", "Payment Entry");
        map.put("payment_type", this.paymentType);
        map.put("party_type", this.partyType);
        map.put("party", this.party);
        map.put("company", this.company);
        map.put("paid_amount", this.paidAmount);
        map.put("mode_of_payment", this.modeOfPayment != null ? this.modeOfPayment : this.mode);
        map.put("reference_no", this.referenceNo != null ? this.referenceNo : this.reference);
        map.put("reference_date", this.paymentDate);
        map.put("paid_from", this.paidFrom);
        map.put("paid_to", this.paidTo);
        map.put("remarks", "Paiement pour facture " + this.invoiceReference);
        
        return map;
    }

    
}