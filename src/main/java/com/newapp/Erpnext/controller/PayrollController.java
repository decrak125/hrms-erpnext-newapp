package com.newapp.Erpnext.controller;


import com.newapp.Erpnext.models.Employee;
import com.newapp.Erpnext.models.PayrollRequest;
import com.newapp.Erpnext.services.PayrollService;
import com.newapp.Erpnext.models.SalarySlip;
import com.newapp.Erpnext.services.SessionService;
import com.newapp.Erpnext.services.EmployeeService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;


import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/payroll")
public class PayrollController {

    private final EmployeeService employeeService;
    private final PayrollService payrollService;
    private final SessionService sessionService;

    public PayrollController(EmployeeService employeeService, SessionService sessionService,
                             PayrollService payrollService) {
        this.employeeService= employeeService;
        this.sessionService=sessionService;
        this.payrollService= payrollService;
    }

   @GetMapping
    public String getAllEmployees(Model model,
                                 @RequestParam(required = false) String department,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) String searchQuery,
                                 @RequestParam(required = false) String startDate,
                                 @RequestParam(required = false) String endDate) {
        if (!sessionService.isAuthenticated()) {
            return "redirect:/login";
        }
        
        List<Employee> employees = employeeService.getAllEmployees();
        model.addAttribute("employees", employees);
        return "autres";
    }

        @PostMapping("/generate")
    public String generatePayroll(@RequestParam String employeeId,
                                  @RequestParam String startDate,
                                  @RequestParam String endDate,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (!sessionService.isAuthenticated()) {
            return "redirect:/dashboard";
        }

        // Validation des paramètres
        if (employeeId == null || employeeId.isBlank() ||
            startDate == null || startDate.isBlank() ||
            endDate == null || endDate.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Employee ID, start date, and end date are required.");
            return "redirect:/autres";
        }

        try {
            // Vérifier si l'employé existe
            Employee employee = employeeService.getEmployeeById(employeeId);
            if (employee == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Employee with ID " + employeeId + " not found.");
                return "redirect:/autres";
            }

            // Appeler la méthode du service
            String result = payrollService.generateSalarySlips(employeeId, startDate, endDate);

            // Ajouter le résultat au modèle pour affichage
            redirectAttributes.addFlashAttribute("success", result);
            redirectAttributes.addFlashAttribute("employee", employee);
            return "redirect:/autres"; // Rediriger vers la page payroll avec le message
        } catch (DateTimeParseException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid date format. Please use YYYY-MM-DD.");
            return "redirect:/autres";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error generating salary slips: " + e.getMessage());
            return "redirect:/autres";
        }
    }
}
