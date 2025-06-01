package com.newapp.Erpnext.controller;

import com.newapp.Erpnext.models.Supplier;
import com.newapp.Erpnext.models.Quotation;
import com.newapp.Erpnext.services.QuotationService;
import com.newapp.Erpnext.services.SupplierService;
import com.newapp.Erpnext.services.SessionService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;
    private final SessionService sessionService;
    private final QuotationService quotationService;

    public SupplierController(SupplierService supplierService, SessionService sessionService, QuotationService quotationService) {
        this.supplierService = supplierService;
        this.sessionService = sessionService;
        this.quotationService = quotationService;
    }

    @GetMapping
    public String getAllSuppliers(Model model) {
        if (!sessionService.isAuthenticated()) {
            return "redirect:/login";
        }
        
        List<Supplier> suppliers = supplierService.getAllSuppliers();
        model.addAttribute("suppliers", suppliers);
        return "suppliers";
    }

    @GetMapping("/{name}")
    public String getSupplierDetails(@PathVariable String name, Model model) {
        if (!sessionService.isAuthenticated()) {
            return "redirect:/login";
        }
        
        Supplier supplier = supplierService.getSupplierByName(name);
        if (supplier == null) {
            return "redirect:/suppliers?error=not_found";
        }
        
        model.addAttribute("supplier", supplier);
        return "supplier-details";
    }

    // Ajouter cette méthode à la classe SupplierController existante

    @GetMapping("/{name}/quotations")
    public String getSupplierQuotations(@PathVariable String name, Model model) {
        if (!sessionService.isAuthenticated()) {
            return "redirect:/login";
        }
        
        Supplier supplier = supplierService.getSupplierByName(name);
        if (supplier == null) {
            return "redirect:/suppliers?error=not_found";
        }
        
        List<Quotation> quotations = quotationService.getQuotationsBySupplier(name);
        
        model.addAttribute("supplierName", supplier.getName());
        model.addAttribute("quotations", quotations);
        return "supplier-quotations";
    }
}