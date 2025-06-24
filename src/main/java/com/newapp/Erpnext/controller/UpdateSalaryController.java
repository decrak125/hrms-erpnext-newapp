package com.newapp.Erpnext.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.newapp.Erpnext.models.Salary;
import com.newapp.Erpnext.models.SalaryComponent;
import com.newapp.Erpnext.models.SalaryStructureAssignment;
import com.newapp.Erpnext.services.SessionService;
import com.newapp.Erpnext.services.UpdateSalaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@RequestMapping("/update")
public class UpdateSalaryController {
    
    private final UpdateSalaryService updateSalaryService;
    private final SessionService sessionService;

    public UpdateSalaryController(UpdateSalaryService updateSalaryService, SessionService sessionService) {
        this.updateSalaryService = updateSalaryService;
        this.sessionService = sessionService;
    }
    
    
@GetMapping
public String showSalaryForm(Model model) {
    if (!sessionService.isAuthenticated()) {
        return "redirect:/login";
    }
    
    try {
        List<SalaryComponent> salaryComponents = updateSalaryService.getAllSalaryComponent();
        model.addAttribute("components", salaryComponents);
        return "salary-form";
    } catch (Exception e) {
        model.addAttribute("error", "Erreur lors du chargement des composants: " + e.getMessage());
        model.addAttribute("components", new ArrayList<>()); // Liste vide pour éviter les erreurs
        return "salary-form";
    }
}

@PostMapping("/update-salary")
public String updateSalary(@RequestParam String salarycomponent, 
                           @RequestParam int comparaison, 
                           @RequestParam BigDecimal amount,
                           @RequestParam int pourcentage,
                           @RequestParam int type,
                           Model model) {
    if (!sessionService.isAuthenticated()) {
        return "redirect:/login";
    }
    try {
        List<Salary> salaries = updateSalaryService.filterSalaries(salarycomponent, amount, comparaison);
        System.out.println("andeha hi filtre");
        if (salaries.isEmpty()) {
            model.addAttribute("message", "Aucun salaire trouvé pour les critères spécifiés.");
            return "salary-form";
        }
        for (Salary salary : salaries) {
            System.out.println("Salary ID: " + salary.getId());
            SalaryStructureAssignment salaryStructureAssignment = updateSalaryService.get(salary);
            System.out.println("ssa: " + salaryStructureAssignment.getName());
            updateSalaryService.remove(salary, salaryStructureAssignment);
            Double base = salaryStructureAssignment.getBase();
            if (type == 1) {
                base += base * pourcentage / 100.0;
            } else if (type == 2) {
                base -= base * pourcentage / 100.0;
                if (base < 0) {
                    base = 0.0;
                }
            }
            System.out.println("Base salary after deduction: " + base);
            salaryStructureAssignment.setBase(base);
            updateSalaryService.createSalaryStructureAssignment(salaryStructureAssignment);
            Salary updateSalary = updateSalaryService.createSalarySlip(salaryStructureAssignment);
            System.out.println("Salary updated: " + updateSalary.getId() + " for employee: " + updateSalary.getEmployeeId());
        }
    } catch (Exception e) {
        model.addAttribute("error", "Erreur lors de la mise à jour du salaire: " + e.getMessage());
        return "redirect:/update";
    }
    model.addAttribute("message", "Salaries updated successfully.");
    model.addAttribute("components", updateSalaryService.getAllSalaryComponent());
    return "redirect:/update";
}

    
}
