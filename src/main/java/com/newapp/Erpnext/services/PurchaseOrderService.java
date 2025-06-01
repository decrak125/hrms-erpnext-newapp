package com.newapp.Erpnext.services;

import com.newapp.Erpnext.models.PurchaseOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PurchaseOrderService {

    @Value("${erpnext.url}")
    private String erpNextUrl;
    
    private final RestTemplate restTemplate;
    private final ErpNextAuthService authService;
    
    public PurchaseOrderService(RestTemplateBuilder restTemplateBuilder, ErpNextAuthService authService) {
        this.restTemplate = restTemplateBuilder.build();
        this.authService = authService;
    }
    
    /**
     * Récupère toutes les commandes d'achat
     * @return Liste des commandes d'achat
     */
    public List<PurchaseOrder> getAllPurchaseOrders() {
        HttpHeaders headers = createHeaders();
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        String url = erpNextUrl + "/api/resource/Purchase Order?fields=[\"name\",\"status\",\"creation\",\"supplier\",\"company\"]";
        
        try {
            System.out.println("Calling API: " + url);
            System.out.println("Headers: " + headers);
            System.out.println("Auth token: " + headers.getFirst("Authorization").replace("token ", "t***"));
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            System.out.println("Response status: " + response.getStatusCode());
            if (response.getBody() != null) {
                System.out.println("Response body: " + response.getBody());
                System.out.println("Response body keys: " + response.getBody().keySet());
            } else {
                System.out.println("Response body is null");
            }
            
            List<PurchaseOrder> purchaseOrders = new ArrayList<>();
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                if (responseBody.containsKey("data")) {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                    System.out.println("Number of purchase orders found: " + data.size());
                    
                    for (Map<String, Object> poData : data) {
                        System.out.println("Purchase order data: " + poData);
                        String name = (String) poData.get("name");
                        String status = (String) poData.get("status");
                        String creationStr = (String) poData.get("creation");
                        String supplier = (String) poData.get("supplier");
                        String company = (String) poData.get("company");
                        
                        System.out.println("Processing purchase order: " + name);
                        System.out.println("  Status: " + status);
                        System.out.println("  Creation: " + creationStr);
                        System.out.println("  Supplier: " + supplier);
                        System.out.println("  Company: " + company);
                        
                        // Create a custom formatter for the date format returned by the API
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
                        
                        // Convertir la date de création
                        LocalDateTime creation;
                        try {
                            creation = LocalDateTime.parse(creationStr, formatter);
                        } catch (Exception e) {
                            System.err.println("Error parsing date: " + creationStr + " - " + e.getMessage());
                            // Fallback to current date if parsing fails
                            creation = LocalDateTime.now();
                        }
                        
                        PurchaseOrder purchaseOrder = new PurchaseOrder(name, status, creation, supplier, company);
                        purchaseOrder.setPercentReceived(0.0); // Valeur par défaut
                        
                        purchaseOrders.add(purchaseOrder);
                    }
                } else {
                    System.err.println("No 'data' key found in response");
                    System.err.println("Available keys: " + responseBody.keySet());
                }
            }
            
            System.out.println("Returning " + purchaseOrders.size() + " purchase orders");
            return purchaseOrders;
        } catch (Exception e) {
            System.err.println("Error retrieving purchase orders: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
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
        headers.set("Authorization", "token " + authToken);
        
        return headers;
    }
}