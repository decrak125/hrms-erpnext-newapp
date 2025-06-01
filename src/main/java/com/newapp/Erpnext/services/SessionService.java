package com.newapp.Erpnext.services;

import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Service
public class SessionService {

    private static final String SESSION_USER_KEY = "currentUser";
    private static final String SESSION_TOKEN_KEY = "sessionToken";

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
            // Gérer le cas où il n'y a pas de contexte de requête
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
            // Gérer le cas où il n'y a pas de contexte de requête
        }
    }
}