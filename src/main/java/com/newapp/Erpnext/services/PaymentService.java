package com.newapp.Erpnext.services;

import com.newapp.Erpnext.models.PaymentEntry;
import com.newapp.Erpnext.models.PurchaseInvoice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    @Value("${erpnext.url}")
    private String erpNextUrl;
    
    private final RestTemplate restTemplate;
    private final ErpNextAuthService authService;
    private final PurchaseInvoiceService invoiceService;
    
    public PaymentService(RestTemplateBuilder restTemplateBuilder, 
                          ErpNextAuthService authService,
                          PurchaseInvoiceService invoiceService) {
        this.restTemplate = restTemplateBuilder.build();
        this.authService = authService;
        this.invoiceService = invoiceService;
    }
    
    /**
     * Récupère les détails d'une facture pour préparer le paiement
     * @param invoiceId ID de la facture
     * @return Objet PaymentEntry pré-rempli
     */
    public PaymentEntry preparePaymentForInvoice(String invoiceId) {
        PurchaseInvoice invoice = invoiceService.getPurchaseInvoiceById(invoiceId);
        
        if (invoice == null) {
            return null;
        }
        
        PaymentEntry payment = new PaymentEntry();
        payment.setInvoiceReference(invoiceId);
        payment.setParty(invoice.getSupplier());
        payment.setCompany(invoice.getCompany());
        payment.setPaidAmount(invoiceService.getInvoiceAmount(invoiceId));
        payment.setPaymentType("Pay");
        payment.setPartyType("Supplier");
        
        // Modifier ces valeurs pour utiliser des comptes existants
        Map<String, String> defaultAccounts = getDefaultAccounts(invoice.getCompany());
    
        payment.setPaidFrom(defaultAccounts.get("cashAccount"));
        payment.setPaidTo(defaultAccounts.get("payableAccount"));
        
        // Compte fournisseurs existant
        
        // Remove this duplicate declaration:
        // Map<String, String> accounts = getDefaultAccounts(invoice.getCompany());
        
        // Keep only this one:
        Map<String, String> accounts = new HashMap<>();
        accounts.put("cashAccount", "Espèces - FKM");
        accounts.put("payableAccount", "Créditeurs - FKM");

        if (!verifyAccountExists(accounts.get("cashAccount")) || 
            !verifyAccountExists(accounts.get("payableAccount"))) {
            logError("Required accounts not found in ERPNext");
            return null;
        }
        
        payment.setPaidFrom(accounts.get("cashAccount"));
        payment.setPaidTo(accounts.get("payableAccount"));
        
        return payment;
    }    

    private boolean verifyAccountExists(String accountName) {
        if (accountName == null || accountName.isEmpty()) {
            return false;
        }

        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        String url = erpNextUrl + "/api/resource/Account/" + accountName;
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                Map.class
            );
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            logError("Error verifying account: " + accountName + " - " + e.getMessage());
            return false;
        }
    }

    private Map<String, String> getDefaultAccounts(String company) {
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            // Fetch cash account
            String cashUrl = erpNextUrl + "/api/resource/Account?fields=[\"name\"]&filters=[[\"account_type\",\"=\",\"Cash\"],[\"company\",\"=\",\"" + company + "\"]]";
            ResponseEntity<Map> cashResponse = restTemplate.exchange(
                cashUrl, HttpMethod.GET, entity, Map.class);
            
            // Fetch payable account
            String payableUrl = erpNextUrl + "/api/resource/Account?fields=[\"name\"]&filters=[[\"account_type\",\"=\",\"Payable\"],[\"company\",\"=\",\"" + company + "\"]]";
            ResponseEntity<Map> payableResponse = restTemplate.exchange(
                payableUrl, HttpMethod.GET, entity, Map.class);
            
            Map<String, String> accounts = new HashMap<>();
            
            // Get first cash account found
            if (cashResponse.getStatusCode() == HttpStatus.OK && cashResponse.getBody() != null) {
                List<Map<String, String>> cashAccounts = (List<Map<String, String>>) cashResponse.getBody().get("data");
                if (!cashAccounts.isEmpty()) {
                    accounts.put("cashAccount", cashAccounts.get(0).get("name"));
                }
            }
            
            // Get first payable account found
            if (payableResponse.getStatusCode() == HttpStatus.OK && payableResponse.getBody() != null) {
                List<Map<String, String>> payableAccounts = (List<Map<String, String>>) payableResponse.getBody().get("data");
                if (!payableAccounts.isEmpty()) {
                    accounts.put("payableAccount", payableAccounts.get(0).get("name"));
                }
            }
            
            // Fallback if no accounts found
            if (accounts.get("cashAccount") == null) {
                accounts.put("cashAccount", "Espèces - " + company);
            }
            if (accounts.get("payableAccount") == null) {
                accounts.put("payableAccount", "Créditeurs - " + company);
            }
            
            return accounts;
            
        } catch (Exception e) {
            logError("Error fetching accounts: " + e.getMessage());
            
            // Fallback values
            Map<String, String> accounts = new HashMap<>();
            accounts.put("cashAccount", "Espèces - " + company);
            accounts.put("payableAccount", "Créditeurs - " + company);
            return accounts;
        }
    }
        /**
     * Crée un paiement dans ERPNext
     * @param payment Objet PaymentEntry
     * @return ID du paiement créé ou null en cas d'échec
     */
    private StringBuilder errorLogs = new StringBuilder();
    
    // Add this method to capture error logs
    private void logError(String message) {
        errorLogs.append(message).append("\n");
        System.err.println(message);
    }
    
    // Add this method to retrieve the logs
    public String getLastErrorLogs() {
        String logs = errorLogs.toString();
        errorLogs = new StringBuilder(); // Clear logs after retrieving
        return logs;
    }
    
    // Then modify your createPayment method to use logError instead of System.err.println
    public String createPayment(PaymentEntry payment) {
        // First verify accounts exist
        if (!verifyAccountExists(payment.getPaidFrom())) {
            logError("Paid From account does not exist: " + payment.getPaidFrom());
            return null;
        }
        if (!verifyAccountExists(payment.getPaidTo())) {
            logError("Paid To account does not exist: " + payment.getPaidTo());
            return null;
        }

        HttpHeaders headers = createHeaders();
        
        Map<String, Object> paymentData = payment.toMap();
        
        // Ajouter les références à la facture
        Map<String, Object> reference = new HashMap<>();
        reference.put("reference_doctype", "Purchase Invoice");
        reference.put("reference_name", payment.getInvoiceReference());
        reference.put("allocated_amount", payment.getPaidAmount());
        
        Map<String, Object> references = new HashMap<>();
        references.put("references", new Object[]{reference});
        
        // Add received_amount equal to paid_amount to fix validation error
        paymentData.put("received_amount", payment.getPaidAmount());
        
        paymentData.putAll(references);
        
        // Debug the payment data
        System.out.println("Complete payment data: " + paymentData);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(paymentData, headers);
        
        String url = erpNextUrl + "/api/resource/Payment Entry";
        
        try {
            System.out.println("Creating payment for invoice: " + payment.getInvoiceReference());
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            // Debug the response
            System.out.println("API Response: " + response.getBody());
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseData = response.getBody();
                if (responseData.containsKey("data")) {
                    Map<String, Object> data = (Map<String, Object>) responseData.get("data");
                    String paymentId = (String) data.get("name");
                    System.out.println("Payment created with ID: " + paymentId);
                    return paymentId;
                }
            }
            
            System.err.println("Failed to create payment. Response: " + response.getBody());
            return null;
        } catch (Exception e) {
            logError("Error creating payment: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }    
    /**
     * Alternative approach inspired by the example
     */
    public boolean submitPaymentWithValidation(String paymentId) {
        try {
            // 1. Create headers once
            HttpHeaders headers = createHeaders();
            
            // 2. Submit using dedicated endpoint
            String submitUrl = erpNextUrl + "/api/resource/Payment Entry/" + paymentId + "?run_method=submit";
            
            ResponseEntity<Map> response = restTemplate.exchange(
                submitUrl,
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Map.class
            );
            
            return response.getStatusCode() == HttpStatus.OK;
            
        } catch (Exception e) {
            logError("Erreur lors de la soumission: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Crée les en-têtes HTTP avec l'authentification
     * @return En-têtes HTTP
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Utilisation de l'authentification par API key
        String authToken = authService.getApiAuthToken();
        if (authToken == null || authToken.isEmpty()) {
            System.err.println("Erreur d'authentification: Token API non disponible");
        }
        headers.set("Authorization", "token " + authToken);
        
        return headers;
    }
    
    /**
     * Vérifie si les identifiants de connexion sont valides
     * @param username Nom d'utilisateur
     * @param password Mot de passe
     * @return true si les identifiants sont valides, false sinon
     */
    public boolean verifyCredentials(String username, String password) {
        try {
            // Créer les en-têtes pour l'authentification de base
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Préparer les données d'authentification
            Map<String, String> authData = new HashMap<>();
            authData.put("usr", username);
            authData.put("pwd", password);
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(authData, headers);
            
            // URL pour la vérification des identifiants
            String url = erpNextUrl + "/api/method/login";
            
            System.out.println("Tentative de vérification des identifiants pour l'utilisateur: " + username);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            boolean isValid = response.getStatusCode() == HttpStatus.OK;
            System.out.println("Résultat de la vérification des identifiants: " + isValid);
            
            if (isValid && response.getBody() != null) {
                System.out.println("Réponse de connexion: " + response.getBody());
            }
            
            return isValid;
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification des identifiants: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}