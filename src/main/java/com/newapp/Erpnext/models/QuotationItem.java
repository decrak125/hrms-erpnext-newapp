package com.newapp.Erpnext.models;

import java.math.BigDecimal;

public class QuotationItem {
    private String itemCode;
    private String itemName;
    private BigDecimal qty;
    private String uom;
    private BigDecimal rate;
    private BigDecimal amount;
    private String warehouse;
    
    // Ajouter getter et setter
    public String getWarehouse() {
        return warehouse;
    }
    
    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }
    public String getItemCode() {
        return itemCode;
    }
    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }
    public String getItemName() {
        return itemName;
    }
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    public BigDecimal getQty() {
        return qty;
    }
    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }
    public String getUom() {
        return uom;
    }
    public void setUom(String uom) {
        this.uom = uom;
    }
    public BigDecimal getRate() {
        return rate;
    }
    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }
    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

}
