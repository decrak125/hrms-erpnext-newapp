package com.newapp.Erpnext.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import com.newapp.Erpnext.services.SessionService;

@Controller
public class WebController {

    private final SessionService sessionService;
    
    public WebController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/")
    public String home() {
        if (sessionService.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }
    
    @GetMapping("/login")
    public String login() {
        if (sessionService.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    // @GetMapping("/suppliers")
    // public String suppliers() {
    //     if (!sessionService.isAuthenticated()) {
    //         return "redirect:/login";
    //     }
    //     return "suppliers";  // Retourne directement la vue "suppliers", pas de redirection
    // }

    // @GetMapping("/quotations")
    // public String quotations() {
    //     if (!sessionService.isAuthenticated()) {
    //         return "redirect:/login";
    //     }
    //     return "quotations";  // Retourne directement la vue "suppliers", pas de redirection
    // }
    
    @GetMapping("/dashboard")
    public String dashboard() {
        if (!sessionService.isAuthenticated()) {
            return "redirect:/login";
        }
        return "dashboard";  // Retourne directement la vue "dashboard", pas de redirection
    }
    
    @GetMapping("/import-hrms")
    public String importHrms() {
        if (!sessionService.isAuthenticated()) {
            return "redirect:/login";
        }
        return "redirect:/import";
    }
}