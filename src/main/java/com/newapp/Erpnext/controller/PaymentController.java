package com.newapp.Erpnext.controller;

import com.newapp.Erpnext.models.PaymentEntry;
import com.newapp.Erpnext.services.PaymentService;
import com.newapp.Erpnext.services.PurchaseInvoiceService;
import com.newapp.Erpnext.services.SessionService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.BindingResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.HashMap;
import org.springframework.web.client.HttpClientErrorException;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.web.bind.annotation.InitBinder;
import java.beans.PropertyEditorSupport;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PurchaseInvoiceService invoiceService;
    private final SessionService sessionService;
    
    public PaymentController(PaymentService paymentService, 
                            PurchaseInvoiceService invoiceService,
                            SessionService sessionService) {
        this.paymentService = paymentService;
        this.invoiceService = invoiceService;
        this.sessionService = sessionService;
    }
    
    @GetMapping("/create/{invoiceId}")
    public String showPaymentForm(@PathVariable String invoiceId, Model model) {
        PaymentEntry payment = paymentService.preparePaymentForInvoice(invoiceId);
        
        if (payment == null) {
            return "redirect:/invoices?error=invoice_not_found";
        }
        
        model.addAttribute("payment", payment);
        return "payment-form";
    }
    
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(LocalDateTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                if (text == null || text.isEmpty()) {
                    setValue(null);
                } else {
                    try {
                        LocalDate date = LocalDate.parse(text);
                        setValue(date.atStartOfDay());
                    } catch (DateTimeParseException e) {
                        throw new IllegalArgumentException("Invalid date format: " + text);
                    }
                }
            }
        });
    }
    
    @PostMapping("/submit")
    public String submitPayment(@ModelAttribute PaymentEntry payment, 
                               RedirectAttributes redirectAttributes,
                               Model model) {

        if (payment.getParty() == null || payment.getParty().isEmpty()) {
            model.addAttribute("error", "Le champ 'Partie' est obligatoire");
            return "payment-form";
        }
    
        if (payment.getPaidAmount() == null || payment.getPaidAmount() <= 0) {
            model.addAttribute("error", "Le montant doit être positif");
            return "payment-form";
        }
                                
        try {
            // Create the payment
            String paymentId = paymentService.createPayment(payment);
            
            if (paymentId != null) {
                // Ajouter un délai court pour éviter les problèmes de concurrence
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                boolean submitted = paymentService.submitPaymentWithValidation(paymentId);
                
                if (submitted) {
                    redirectAttributes.addFlashAttribute("success", true);
                    redirectAttributes.addFlashAttribute("message", "Paiement " + paymentId + " créé et soumis avec succès");
                    return "redirect:/invoices";
                } else {
                    model.addAttribute("error", "Le paiement a été créé mais n'a pas pu être soumis. ID: " + paymentId);
                    model.addAttribute("payment", payment);
                    model.addAttribute("logs", paymentService.getLastErrorLogs());
                    model.addAttribute("paymentId", paymentId);
                    return "payment-form";
                }
            } else {
                model.addAttribute("error", "Échec de la création du paiement");
                model.addAttribute("payment", payment);
                model.addAttribute("logs", paymentService.getLastErrorLogs());
                return "payment-form";
            }
        } catch (HttpClientErrorException e) {
            // Handle HTTP client errors (like validation errors from the API)
            String errorDetails = "Erreur de configuration comptable: ";
            if (e.getMessage().contains("LinkValidationError")) {
                errorDetails += "Veuillez vérifier que les comptes 'Caisse' et 'Fournisseurs' sont configurés dans ERPNext";
            } else {
                errorDetails += e.getMessage();
            }
            model.addAttribute("error", errorDetails);
            model.addAttribute("payment", payment);
            model.addAttribute("logs", paymentService.getLastErrorLogs());
            model.addAttribute("apiResponse", e.getResponseBodyAsString());
            return "payment-form";
        } catch (Exception e) {
            // Handle other exceptions
            String errorDetails = "Erreur: " + e.getMessage();
            System.err.println(errorDetails);
            e.printStackTrace();
            model.addAttribute("error", errorDetails);
            model.addAttribute("payment", payment);
            model.addAttribute("logs", paymentService.getLastErrorLogs());
            model.addAttribute("stackTrace", e.getStackTrace());
            return "payment-form";
        }
    }

    // Ajouter un nouveau endpoint pour soumettre manuellement un paiement existant
    @PostMapping("/submit-existing/{paymentId}")
    public String submitExistingPayment(@PathVariable String paymentId, 
                                       RedirectAttributes redirectAttributes) {
        try {
            boolean submitted = paymentService.submitPaymentWithValidation(paymentId);
            
            if (submitted) {
                redirectAttributes.addFlashAttribute("success", true);
                redirectAttributes.addFlashAttribute("message", "Paiement " + paymentId + " soumis avec succès");
            } else {
                redirectAttributes.addFlashAttribute("error", "Échec de la soumission du paiement " + paymentId);
                redirectAttributes.addFlashAttribute("logs", paymentService.getLastErrorLogs());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        
        return "redirect:/invoices";
    }
    
    @GetMapping("/api/invoice-details/{invoiceId}")
    @ResponseBody
    public Map<String, Object> getInvoiceDetails(@PathVariable("invoiceId") String invoiceId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> details = invoiceService.getInvoiceDetails(invoiceId);
            
            if (details != null) {
                response.put("success", true);
                response.put("data", details);
            } else {
                response.put("success", false);
                response.put("message", "Détails de la facture non trouvés");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
        }
        
        return response;
    }
}