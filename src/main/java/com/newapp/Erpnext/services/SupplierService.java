package com.newapp.Erpnext.services;

import com.newapp.Erpnext.models.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SupplierService {

    @Value("${erpnext.url}")
    private String erpNextUrl;
    
    private final RestTemplate restTemplate;
    private final ErpNextAuthService authService;
    
    public SupplierService(RestTemplateBuilder restTemplateBuilder, ErpNextAuthService authService) {
        this.restTemplate = restTemplateBuilder.build();
        this.authService = authService;
    }
    
    /**
     * Récupère la liste des fournisseurs depuis ERPNext
     * @return Liste des fournisseurs
     */
    public List<Supplier> getAllSuppliers() {
        HttpHeaders headers = createHeaders();
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        String url = erpNextUrl + "/api/resource/Supplier?fields=[\"name\",\"owner\",\"supplier_group\",\"supplier_type\"]";
        
        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            Map.class
        );
        
        List<Supplier> suppliers = new ArrayList<>();
        
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                
                for (Map<String, Object> supplierData : data) {
                    Supplier supplier = new Supplier();
                    supplier.setName((String) supplierData.get("name"));
                    supplier.setOwner((String) supplierData.get("owner"));
                    supplier.setSupplierGroup((String) supplierData.get("supplier_group"));
                    supplier.setSupplierType((String) supplierData.get("supplier_type"));
                    
                    suppliers.add(supplier);
                }
            }
        }
        
        return suppliers;
    }
    
    /**
     * Récupère un fournisseur par son nom
     * @param name Nom du fournisseur
     * @return Fournisseur trouvé ou null
     */
    public Supplier getSupplierByName(String name) {
        HttpHeaders headers = createHeaders();
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        String url = erpNextUrl + "/api/resource/Supplier/" + name;
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                if (responseBody.containsKey("data")) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                    
                    Supplier supplier = new Supplier();
                    supplier.setName((String) data.get("name"));
                    supplier.setOwner((String) data.get("owner"));
                    supplier.setSupplierGroup((String) data.get("supplier_group"));
                    supplier.setSupplierType((String) data.get("supplier_type"));
                    
                    return supplier;
                }
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération du fournisseur: " + e.getMessage());
            return null;
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