package com.newapp.Erpnext.controller;

import com.newapp.Erpnext.services.PurchaseInvoiceService;
import com.newapp.Erpnext.services.SessionService;
import com.newapp.Erpnext.models.PurchaseInvoice;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/invoices")
public class PurchaseInvoiceController {

    private final PurchaseInvoiceService purchaseInvoiceService;
    private final SessionService sessionService;

    public PurchaseInvoiceController(PurchaseInvoiceService purchaseInvoiceService, SessionService sessionService) {
        this.purchaseInvoiceService = purchaseInvoiceService;
        this.sessionService = sessionService;
    }

    @GetMapping
    public String getPurchaseInvoicesPage(Model model) {
        if (!sessionService.isAuthenticated()) {
            return "redirect:/login";
        }
        
        // Récupérer les factures directement pour le modèle
        try {
            List<PurchaseInvoice> invoices = purchaseInvoiceService.getAllPurchaseInvoices();
            model.addAttribute("purchaseInvoices", invoices);
            System.out.println("Added " + invoices.size() + " purchase invoices to the model");
        } catch (Exception e) {
            System.err.println("Error retrieving purchase invoices for page: " + e.getMessage());
            model.addAttribute("error", "Failed to load purchase invoices");
        }
        
        return "invoices";
    }
    
    @GetMapping("/api")
    @ResponseBody
    public List<PurchaseInvoice> getAllPurchaseInvoices() {
        if (!sessionService.isAuthenticated()) {
            System.out.println("Utilisateur non authentifié");
            return List.of();
        }
        
        try {
            List<PurchaseInvoice> invoices = purchaseInvoiceService.getAllPurchaseInvoices();
            System.out.println("Nombre de factures récupérées: " + invoices.size());
            return invoices;
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des factures: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
    
    @PostMapping("/api/pay/{id}")
    @ResponseBody
    public Map<String, Object> markInvoiceAsPaid(@PathVariable("id") String invoiceId) {
        Map<String, Object> response = new HashMap<>();
        
        if (!sessionService.isAuthenticated()) {
            response.put("success", false);
            response.put("message", "Utilisateur non authentifié");
            return response;
        }
        
        try {
            boolean success = purchaseInvoiceService.markAsPaid(invoiceId);
            response.put("success", success);
            if (success) {
                response.put("message", "Facture marquée comme payée avec succès");
            } else {
                response.put("message", "Échec de la mise à jour de la facture");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
        }
        
        return response;
    }
    
    @GetMapping("/details/{id}")
    public String getInvoiceDetails(@PathVariable("id") String invoiceId, Model model) {
        if (!sessionService.isAuthenticated()) {
            return "redirect:/login";
        }
        
        try {
            PurchaseInvoice invoice = purchaseInvoiceService.getPurchaseInvoiceById(invoiceId);
            if (invoice != null) {
                model.addAttribute("invoice", invoice);
            } else {
                model.addAttribute("error", "Facture non trouvée");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors du chargement de la facture: " + e.getMessage());
        }
        
        return "invoice-details";
    }
    
    // Add a new endpoint for PDF generation
    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> generateInvoicePdf(@PathVariable("id") String invoiceId) {
        if (!sessionService.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            byte[] pdfBytes = purchaseInvoiceService.generateInvoicePdf(invoiceId);
            if (pdfBytes != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDispositionFormData("attachment", "invoice-" + invoiceId + ".pdf");
                headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
                
                return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("Error generating PDF: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}