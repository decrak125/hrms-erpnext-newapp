package com.newapp.Erpnext.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import java.util.Map;
import java.util.HashMap;
import org.springframework.http.MediaType;
@Service
public class ErpNextAuthService {

    @Value("${erpnext.url}")
    private String erpNextUrl;
    
    @Value("${erpnext.api.key}")
    private String apiKey;
    
    @Value("${erpnext.api.secret}")
    private String apiSecret;
    
    private final RestTemplate restTemplate;

    public ErpNextAuthService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public LoginResponse authenticate(String username, String password) {
        // 1. Préparation de la requête
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("usr", username);
        loginRequest.put("pwd", password);
    
        HttpEntity<Map<String, String>> request = new HttpEntity<>(loginRequest, headers);
    
        // 2. Appel à l'API ERPNext
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                erpNextUrl + "/api/method/login",
                request,
                Map.class
            );
    
            // 3. Traitement de la réponse
            if (response.getStatusCode() == HttpStatus.OK) {
                // Extraction plus robuste du sessionId
                String sessionId = null;
                if (response.getHeaders().get("Set-Cookie") != null) {
                    sessionId = response.getHeaders().get("Set-Cookie")
                        .stream()
                        .filter(c -> c.startsWith("sid="))
                        .findFirst()
                        .orElse(null);
                    
                    if (sessionId != null) {
                        // Extraire juste la valeur du cookie sans les attributs supplémentaires
                        sessionId = sessionId.split(";")[0];
                    }
                }
                
                if (sessionId == null) {
                    throw new AuthenticationException("Session ID not found in response");
                }
    
                // Vérification que le corps de la réponse contient les données attendues
                Map<String, Object> body = response.getBody();
                String fullName = body != null && body.containsKey("full_name") ? 
                                  (String) body.get("full_name") : "Unknown User";
    
                return new LoginResponse(
                    true,
                    "Authentication successful",
                    sessionId,
                    fullName
                );
            } else {
                throw new AuthenticationException("Invalid credentials");
            }
        } catch (Exception e) {
            throw new AuthenticationException("Authentication failed: " + e.getMessage());
        }
    }
    // Méthode pour authentification par API key (alternative)
    public String getApiAuthToken() {
        return apiKey + ":" + apiSecret;
    }

    public static class LoginResponse {
        private boolean success;
        private String message;
        private String sessionId;
        private String fullName;

        public LoginResponse(boolean success, String message, String sessionId, String fullName) {
            this.success = success;
            this.message = message;
            this.sessionId = sessionId;
            this.fullName = fullName;
        }

        // Getters et Setters
        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getFullName() {
            return fullName;
        }
    }
    public String getApiKey() {
        return apiKey;
    }
    
    public String getApiSecret() {
        return apiSecret;
    }
}