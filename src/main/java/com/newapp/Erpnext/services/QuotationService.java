package com.newapp.Erpnext.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newapp.Erpnext.models.Quotation;
import com.newapp.Erpnext.models.Supplier;
import com.newapp.Erpnext.models.QuotationItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuotationService {

    @Value("${erpnext.url}")
    private String erpNextUrl;
    
    private final RestTemplate restTemplate;
    private final ErpNextAuthService authService;
    
    public QuotationService(RestTemplateBuilder restTemplateBuilder, ErpNextAuthService authService) {
        this.restTemplate = restTemplateBuilder.build();
        this.authService = authService;
    }
    
    /**
     * Récupère tous les devis fournisseurs
     * @return Liste des devis
     */
    public List<Quotation> getAllQuotations() {
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        // Modifié pour inclure tous les devis (docstatus 0=brouillon, 1=validé, 2=annulé)
        String url = erpNextUrl + "/api/resource/Supplier Quotation?fields=[\"name\",\"supplier\",\"transaction_date\",\"grand_total\",\"status\"]&filters=[[\"docstatus\",\"in\",[0,1]]]";
        
        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            Map.class
        );
        
        return processQuotationsResponse(response);
    }

    // Même modification pour getQuotationsBySupplier
    public List<Quotation> getQuotationsBySupplier(String supplierName) {
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        String url = erpNextUrl + "/api/resource/Supplier Quotation?fields=[\"name\",\"supplier\",\"transaction_date\",\"grand_total\",\"status\"]&filters=[[\"supplier\",\"=\",\"" + supplierName + "\"],[\"docstatus\",\"in\",[0,1]]]";
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            return processQuotationsResponse(response);
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des devis: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Récupère un devis spécifique par son nom
     * @param name Nom du devis
     * @return Devis trouvé ou null
     */
    public Quotation getQuotationByName(String name) {
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        String url = erpNextUrl + "/api/resource/Supplier Quotation/" + name + "?fields=[\"name\",\"supplier\",\"transaction_date\",\"grand_total\",\"status\",\"items\"]";
        
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
                    
                    List<QuotationItem> items = new ArrayList<>();
                    if (data.containsKey("items")) {
                        List<Map<String, Object>> itemsData = (List<Map<String, Object>>) data.get("items");
                        for (Map<String, Object> itemData : itemsData) {
                            QuotationItem item = new QuotationItem();
                            item.setItemCode((String) itemData.get("item_code"));
                            item.setItemName((String) itemData.get("item_name"));
                            item.setQty(new BigDecimal(itemData.get("qty").toString()));
                            item.setUom((String) itemData.get("uom"));
                            item.setRate(new BigDecimal(itemData.get("rate").toString()));
                            item.setAmount(new BigDecimal(itemData.get("amount").toString()));
                            item.setWarehouse((String) itemData.get("warehouse")); // Ajout de la récupération de l'entrepôt
                            items.add(item);
                        }
                    }
                    
                    return new Quotation(
                        (String) data.get("name"),
                        (String) data.get("supplier"),
                        LocalDate.parse((String) data.get("transaction_date"), DateTimeFormatter.ISO_DATE),
                        new BigDecimal(data.get("grand_total").toString()),
                        (String) data.get("status"),
                        items
                    );
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération du devis: " + e.getMessage());
            return null;
        }
    }

    

    public boolean updateItemRate(String quotationName, String itemCode, BigDecimal newRate) {
        Quotation quotation = getQuotationByName(quotationName);
        if (!isQuotationEditable(quotationName)) {
            return false;
        }

        QuotationItem itemToUpdate = quotation.getItems().stream()
                .filter(item -> item.getItemCode().equals(itemCode))
                .findFirst()
                .orElse(null);

        if (itemToUpdate == null) {
            return false;
        }

        HttpHeaders headers = createHeaders();
        Map<String, Object> itemMap = new HashMap<>();
        itemMap.put("item_code", itemCode);
        itemMap.put("rate", newRate);
        itemMap.put("qty", itemToUpdate.getQty());
        itemMap.put("warehouse", itemToUpdate.getWarehouse());

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("items", List.of(itemMap));
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(updateData, headers);
        String url = erpNextUrl + "/api/resource/Supplier Quotation/" + quotationName;
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("Prix mis à jour avec succès pour " + itemCode + " dans le devis " + quotationName);
                
                // Ajout de la validation automatique après mise à jour
                boolean submissionSuccess = submitQuotation(quotationName);
                if (submissionSuccess) {
                    System.out.println("Devis validé avec succès: " + quotationName);
                } else {
                    System.err.println("Échec de la validation du devis: " + quotationName);
                }
                return submissionSuccess;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour du prix: " + e.getMessage());
            return false;
        }
    }


    // public boolean updateItemRate(String quotationName, String itemCode, BigDecimal newRate) {
    //     Quotation quotation = getQuotationByName(quotationName);
    //     if (!isQuotationEditable(quotationName)) {
    //         return false;
    //     }

    //     QuotationItem itemToUpdate = quotation.getItems().stream()
    //             .filter(item -> item.getItemCode().equals(itemCode))
    //             .findFirst()
    //             .orElse(null);

    //     if (itemToUpdate == null) {
    //         return false;
    //     }

    //     HttpHeaders headers = createHeaders();
    //     Map<String, Object> itemMap = new HashMap<>();
    //     itemMap.put("item_code", itemCode);
    //     itemMap.put("rate", newRate);
    //     itemMap.put("qty", itemToUpdate.getQty());
    //     itemMap.put("warehouse", itemToUpdate.getWarehouse()); // Utilisation de l'entrepôt existant

    //     Map<String, Object> updateData = new HashMap<>();
    //     updateData.put("items", List.of(itemMap));
        
    //     HttpEntity<Map<String, Object>> entity = new HttpEntity<>(updateData, headers);
    //     String url = erpNextUrl + "/api/resource/Supplier Quotation/" + quotationName;
        
    //     try {
    //         ResponseEntity<Map> response = restTemplate.exchange(
    //             url,
    //             HttpMethod.PUT,
    //             entity,
    //             Map.class
    //         );

    //         if (response.getStatusCode() == HttpStatus.OK) {
    //             // After successful update, we need to get the latest version before submitting
    //             try {
    //                 // Wait a moment to ensure the update is processed
    //                 Thread.sleep(1000);
                    
    //                 // Get the updated quotation
    //                 Quotation updatedQuotation = getQuotationByName(quotationName);
    //                 if (updatedQuotation == null) {
    //                     System.err.println("Impossible de récupérer le devis mis à jour");
    //                     return false;
    //                 }
                    
    //                 // Now submit the quotation
    //                 HttpHeaders submitHeaders = createHeaders();
    //                 Map<String, Object> submitData = new HashMap<>();
    //                 Map<String, Object> docData = new HashMap<>();
    //                 docData.put("doctype", "Supplier Quotation");
    //                 docData.put("name", quotationName);
    //                 submitData.put("doc", docData);
                    
    //                 HttpEntity<Map<String, Object>> submitEntity = new HttpEntity<>(submitData, submitHeaders);
    //                 String submitUrl = erpNextUrl + "/api/method/frappe.client.submit";
                    
    //                 ResponseEntity<Map> submitResponse = restTemplate.exchange(
    //                     submitUrl,
    //                     HttpMethod.POST,
    //                     submitEntity,
    //                     Map.class
    //                 );
    
    //                 if (submitResponse.getStatusCode() == HttpStatus.OK) {
    //                     System.out.println("Devis validé avec succès: " + quotationName);
    //                     return true;
    //                 } else {
    //                     System.err.println("Erreur lors de la validation du devis: " + submitResponse.getStatusCode());
    //                     return false;
    //                 }
    //             } catch (Exception submitEx) {
    //                 System.err.println("Erreur lors de la validation du devis: " + submitEx.getMessage());
    //                 // Return true anyway since the price update was successful
    //                 return true;
    //             }
    //         }            return false;
    //     } catch (Exception e) {
    //         System.err.println("Erreur lors de la mise à jour du prix: " + e.getMessage());
    //         return false;
    //     }
    // }

    /**
     * Traite la réponse de l'API pour extraire les devis
     * @param response Réponse de l'API
     * @return Liste des devis
     */
    private List<Quotation> processQuotationsResponse(ResponseEntity<Map> response) {
        List<Quotation> quotations = new ArrayList<>();
        
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                
                for (Map<String, Object> quotationData : data) {
                    String name = (String) quotationData.get("name");
                    String supplier = (String) quotationData.get("supplier");
                    String transactionDateStr = (String) quotationData.get("transaction_date");
                    Object grandTotalObj = quotationData.get("grand_total");
                    String status = (String) quotationData.get("status");
                    
                    LocalDate transactionDate = LocalDate.parse(transactionDateStr, DateTimeFormatter.ISO_DATE);
                    BigDecimal grandTotal = new BigDecimal(grandTotalObj.toString());
                    
                    Quotation quotation = new Quotation(name, supplier, transactionDate, grandTotal, status);
                    quotations.add(quotation);
                }
            }
        }
        
        return quotations;
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

    // Ajouter cette nouvelle méthode

        /**
     * Vérifie si un devis est modifiable (en brouillon)
     * @param quotationName Nom du devis
     * @return true si le devis est modifiable, false sinon
     */
    public boolean isQuotationEditable(String quotationName) {
        Quotation quotation = getQuotationByName(quotationName);
        return quotation != null && "Draft".equals(quotation.getStatus());
    }
    
    /**
     * Récupère les informations d'un fournisseur de manière sécurisée
     * @param supplierName Nom du fournisseur
     * @return Map contenant les informations du fournisseur ou null si non trouvé
     */
    public Map<String, Object> getSupplierInfo(String supplierName) {
        // Si le nom du fournisseur est null ou vide, retourner null immédiatement
        if (supplierName == null || supplierName.trim().isEmpty()) {
            System.out.println("Nom de fournisseur non spécifié");
            return null;
        }
        
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        String url = erpNextUrl + "/api/resource/Supplier/" + supplierName;
        
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
                    return (Map<String, Object>) responseBody.get("data");
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération du fournisseur " + supplierName + ": " + e.getMessage());
            return null;
        }
    }
        /**
    /**
    /**
     * Valide (soumet) une demande de devis fournisseur
     * @param quotationName Nom du devis à valider
     * @return true si la validation a réussi, false sinon
     */
    public boolean submitQuotation(String quotationName) {
        try {
            // Étape 1 : Récupérer le devis
            String getUrl = erpNextUrl + "/api/resource/Supplier Quotation/" + quotationName;
            HttpHeaders getHeaders = createHeaders();
            
            HttpEntity<String> getEntity = new HttpEntity<>(getHeaders);
            ResponseEntity<Map> getResponse = restTemplate.exchange(
                getUrl,
                HttpMethod.GET,
                getEntity,
                Map.class
            );

            if (getResponse.getStatusCode() != HttpStatus.OK || getResponse.getBody() == null) {
                return false;
            }

            Map<String, Object> data = (Map<String, Object>) getResponse.getBody().get("data");

            // Étape 2 : Convertir data en JSON string
            ObjectMapper mapper = new ObjectMapper();
            String docJson = mapper.writeValueAsString(data);

            // Étape 3 : Préparer les headers + body pour submit
            HttpHeaders submitHeaders = createHeaders();
            submitHeaders.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> submitParams = new HashMap<>();
            submitParams.put("doc", docJson);

            HttpEntity<Map<String, Object>> submitEntity = new HttpEntity<>(submitParams, submitHeaders);

            // Étape 4 : Envoyer la requête
            String submitUrl = erpNextUrl + "/api/method/frappe.client.submit";
            ResponseEntity<Map> submitResponse = restTemplate.exchange(
                submitUrl,
                HttpMethod.POST,
                submitEntity,
                Map.class
            );

            return submitResponse.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            System.err.println("Erreur lors de la validation du devis: " + e.getMessage());
            return false;
        }
    }
/**
 * Récupère un fournisseur par son nom
 * @param name Nom du fournisseur
 * @return Supplier trouvé ou null si non trouvé ou erreur
 */
public Supplier getSupplierByName(String name) {
    if (name == null || name.trim().isEmpty()) {
        System.err.println("Le nom du fournisseur ne peut pas être null ou vide");
        return null;
    }

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
                
                // Création et retour de l'objet Supplier
                Supplier supplier = new Supplier();
                supplier.setName((String) data.get("name"));
                supplier.setName((String) data.get("supplier_name"));
                supplier.setSupplierType((String) data.get("supplier_type"));
                // Ajoutez d'autres propriétés selon votre modèle Supplier
                
                return supplier;
            }
        }
        return null;
    } catch (Exception e) {
        System.err.println("Erreur lors de la récupération du fournisseur: " + e.getMessage());
        return null;
    }
}
}


    
