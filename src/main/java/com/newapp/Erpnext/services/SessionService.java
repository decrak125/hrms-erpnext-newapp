package com.newapp.Erpnext.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Map;

@Service
public class SessionService {

    private static final String SESSION_USER_KEY = "currentUser";
    private static final String SESSION_TOKEN_KEY = "sessionToken";
    private static final String IMPORT_SESSION_SID_KEY = "importSid";
    
    @Value("${erpnext.api.url:http://erpnext.localhost:8000/api}")
    private String erpnextApiUrl;
    
    @Value("${erpnext.username:Administrator}")
    private String erpnextUsername;
    
    @Value("${erpnext.password:admin}")
    private String erpnextPassword;
    
    @Autowired
    private RestTemplate restTemplate;

    public void createSession(String username, String fullName, String sessionToken) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        HttpSession session = request.getSession(true);
        
        session.setAttribute(SESSION_USER_KEY, fullName);
        session.setAttribute(SESSION_TOKEN_KEY, sessionToken);
    }
    
    public String getSessionToken() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            HttpSession session = request.getSession(false);
            
            if (session != null) {
                return (String) session.getAttribute(SESSION_TOKEN_KEY);
            }
        } catch (Exception e) {
            System.out.println("Erreur lors de la récupération du jeton de session: " + e.getMessage());
        }
        return null;
    }
    
    public boolean isAuthenticated() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            HttpSession session = request.getSession(false);
            
            return session != null && session.getAttribute(SESSION_USER_KEY) != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    public void invalidateSession() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            HttpSession session = request.getSession(false);
            
            if (session != null) {
                session.invalidate();
            }
        } catch (Exception e) {
            System.out.println("Erreur lors de l'invalidation de la session: " + e.getMessage());
        }
    }

    public String getOrCreateImportSid(HttpSession session) {
        String sid = (String) session.getAttribute(IMPORT_SESSION_SID_KEY);
        if (sid == null || !isValidErpnextSid(sid)) {
            sid = loginToErpnext();
            if (sid != null) {
                session.setAttribute(IMPORT_SESSION_SID_KEY, sid);
                System.out.println("Nouveau sid ERPNext généré pour ImportController: " + sid);
            } else {
                System.out.println("Échec de l'obtention du sid ERPNext");
            }
        }
        return sid;
    }

    public boolean isImportSidValid(HttpSession session) {
        if (session == null) {
            return false;
        }
        String sid = (String) session.getAttribute(IMPORT_SESSION_SID_KEY);
        return sid != null && isValidErpnextSid(sid);
    }

    public void invalidateImportSid(HttpSession session) {
        if (session != null) {
            session.removeAttribute(IMPORT_SESSION_SID_KEY);
            System.out.println("sid de ImportController invalidé");
        }
    }

    private String loginToErpnext() {
        try {
            String loginUrl = erpnextApiUrl + "/method/login";
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("usr", erpnextUsername);
            map.add("pwd", erpnextPassword);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(loginUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
                if (cookies != null) {
                    for (String cookie : cookies) {
                        if (cookie.contains("sid=")) {
                            String sid = cookie.split("sid=")[1].split(";")[0];
                            System.out.println("Connexion ERPNext réussie, sid obtenu: " + sid);
                            return sid;
                        }
                    }
                }
                System.out.println("Aucun sid trouvé dans les cookies de la réponse ERPNext");
            } else {
                System.out.println("Échec de la connexion ERPNext, code: " + response.getStatusCodeValue());
            }
        } catch (HttpClientErrorException e) {
            System.out.println("Erreur HTTP lors de la connexion à ERPNext: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.out.println("Erreur inattendue lors de la connexion à ERPNext: " + e.getMessage());
        }
        return null;
    }

    private boolean isValidErpnextSid(String sid) {
        try {
            String checkUrl = erpnextApiUrl + "/method/frappe.auth.get_logged_user";
            HttpHeaders headers = new HttpHeaders();
            headers.add("Cookie", "sid=" + sid);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(checkUrl, HttpMethod.GET, request, Map.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.out.println("sid ERPNext invalide ou expiré: " + e.getMessage());
            return false;
        }
    }

    public String ensureImportSessionValid(HttpSession session) {
        String sid = (String) session.getAttribute(IMPORT_SESSION_SID_KEY);
        
        if (sid == null || !isValidErpnextSid(sid)) {
            try {
                String newSid = loginToErpnext();
                if (newSid != null) {
                    session.setAttribute(IMPORT_SESSION_SID_KEY, newSid);
                    System.out.println("Nouvelle session d'importation créée avec succès");
                    return newSid;
                } else {
                    System.out.println("Échec de création de la session d'importation");
                    return null;
                }
            } catch (Exception e) {
                System.out.println("Erreur lors de la création de la session d'importation: " + e.getMessage());
                return null;
            }
        }
        
        return sid;
    }

    public boolean hasValidImportSession(HttpSession session) {
        String sid = (String) session.getAttribute(IMPORT_SESSION_SID_KEY);
        return sid != null && isValidErpnextSid(sid);
    }
}