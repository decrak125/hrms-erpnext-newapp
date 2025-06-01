package com.newapp.Erpnext.controller;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.*;
import com.newapp.Erpnext.services.ErpNextAuthService;
import com.newapp.Erpnext.services.ErpNextAuthService.LoginResponse;
import com.newapp.Erpnext.services.SessionService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ErpNextAuthService authService;
    private final SessionService sessionService;

    public AuthController(ErpNextAuthService authService, SessionService sessionService) {
        this.authService = authService;
        this.sessionService = sessionService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.authenticate(
                request.getUsername(),
                request.getPassword()
            );
            
            // Créer une session côté serveur
            sessionService.createSession(
                request.getUsername(),
                response.getFullName(),
                response.getSessionId()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
    
    // Ajouter un endpoint pour vérifier l'état de l'authentification
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("authenticated", sessionService.isAuthenticated());
        
        return ResponseEntity.ok(status);
    }
    
    // Ajouter un endpoint pour la déconnexion
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        sessionService.invalidateSession();
        return ResponseEntity.ok().build();
    }
    
    public static class LoginRequest {
        private String username;
        private String password;
        public String getUsername() {
            return username;
        }
        public void setUsername(String username) {
            this.username = username;
        }
        public String getPassword() {
            return password;
        }
        public void setPassword(String password) {
            this.password = password;
        }
    }
}