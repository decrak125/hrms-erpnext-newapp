package com.newapp.Erpnext.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        
        String errorMessage = "Une erreur inattendue est survenue.";
        
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                errorMessage = "La page demandée n'existe pas (Erreur 404)";
            }
            else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                errorMessage = "Une erreur interne du serveur s'est produite (Erreur 500)";
                if (exception != null) {
                    errorMessage += "\nDétails : " + exception.toString();
                }
            }
            else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                errorMessage = "Accès refusé. Vous n'avez pas les permissions nécessaires (Erreur 403)";
            }
            else if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
                errorMessage = "Authentification requise. Veuillez vous connecter (Erreur 401)";
            }
            else if (statusCode == HttpStatus.BAD_REQUEST.value()) {
                errorMessage = "Requête invalide (Erreur 400)";
                if (message != null) {
                    errorMessage += "\nDétails : " + message.toString();
                }
            }
        }
        
        model.addAttribute("error", errorMessage);
        model.addAttribute("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        return "error";
    }
} 