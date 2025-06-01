package com.newapp.Erpnext.controller;

import com.newapp.Erpnext.services.PurchaseOrderService;
import com.newapp.Erpnext.services.SessionService;
import com.newapp.Erpnext.models.PurchaseOrder;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/purchase")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final SessionService sessionService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService, SessionService sessionService) {
        this.purchaseOrderService = purchaseOrderService;
        this.sessionService = sessionService;
    }

    @GetMapping
    public String getPurchaseOrdersPage(Model model) {
        if (!sessionService.isAuthenticated()) {
            return "redirect:/login";
        }
        
        // Call the service directly to retrieve purchase orders
        try {
            List<PurchaseOrder> orders = purchaseOrderService.getAllPurchaseOrders();
            model.addAttribute("purchaseOrders", orders);
            System.out.println("Added " + orders.size() + " purchase orders to the model");
        } catch (Exception e) {
            System.err.println("Error retrieving purchase orders for page: " + e.getMessage());
            model.addAttribute("error", "Failed to load purchase orders");
        }
        
        return "purchase";
    }
    
    @GetMapping("/api")
    @ResponseBody
    public List<PurchaseOrder> getAllPurchaseOrders() {
        if (!sessionService.isAuthenticated()) {
            System.out.println("Utilisateur non authentifié");
            return List.of();
        }
        
        try {
            List<PurchaseOrder> orders = purchaseOrderService.getAllPurchaseOrders();
            System.out.println("Nombre de commandes récupérées: " + orders.size());
            return orders;
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des commandes: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
}