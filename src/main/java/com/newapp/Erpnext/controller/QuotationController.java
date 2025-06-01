package com.newapp.Erpnext.controller;

import com.newapp.Erpnext.services.QuotationService;
import com.newapp.Erpnext.services.SupplierService;
import com.newapp.Erpnext.services.SessionService;
import com.newapp.Erpnext.models.Quotation;
import com.newapp.Erpnext.models.Supplier;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/quotations")
public class QuotationController {

    private final QuotationService quotationService;
    private final SupplierService supplierService;
    private final SessionService sessionService;

    public QuotationController(QuotationService quotationService, SupplierService supplierService, SessionService sessionService) {
        this.quotationService = quotationService;
        this.supplierService = supplierService;
        this.sessionService = sessionService;
    }


    @GetMapping("/{name}")
    public String getQuotationDetails(@PathVariable String name, Model model) {
        if (!sessionService.isAuthenticated()) {
            return "redirect:/login";
        }
        
        Quotation quotation = quotationService.getQuotationByName(name);
        if (quotation == null) {
            return "redirect:/quotations?error=not_found";
        }
        
        boolean isEditable = "Draft".equals(quotation.getStatus());
        model.addAttribute("quotation", quotation);
        model.addAttribute("isEditable", isEditable);
        return "quotation-details";
    }

    @PostMapping("/{name}/update-rate")
    public String updateItemRate(@PathVariable String name,
                               @RequestParam String itemCode,
                               @RequestParam BigDecimal newRate,
                               RedirectAttributes redirectAttributes) {
        try {
            boolean updated = quotationService.updateItemRate(name, itemCode, newRate);
            
            if (updated) {
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Le prix a été mis à jour avec succès");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Impossible de mettre à jour le prix. Vérifiez que le devis est en brouillon.");
            }
        } catch (Exception e) {
            String errorDetails = String.format("Code erreur: %s - %s", 
                e.getMessage().contains("417") ? "417 Expectation Failed" : "Erreur système",
                e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors de la mise à jour du prix. " + errorDetails);
        }
        
        return "redirect:/quotations/" + name;
    }
    
    
    
}