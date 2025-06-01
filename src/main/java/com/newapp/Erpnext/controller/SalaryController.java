package com.newapp.Erpnext.controller;

import com.newapp.Erpnext.models.Employee;
import com.newapp.Erpnext.models.Salary;
import com.newapp.Erpnext.services.EmployeeService;
import com.newapp.Erpnext.services.SessionService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@Controller
@RequestMapping("/salaries")
public class SalaryController {

    private final EmployeeService employeeService;
    private final SessionService sessionService;

    public SalaryController(EmployeeService employeeService, SessionService sessionService) {
        this.employeeService = employeeService;
        this.sessionService = sessionService;
    }

    @GetMapping
    public String getAllSalaries(Model model,
                               @RequestParam(required = false) String month,
                               @RequestParam(required = false) String employeeId,
                               @RequestParam(required = false) String department,
                               @RequestParam(required = false) String status) {
        if (!sessionService.isAuthenticated()) {
            return "redirect:/login";
        }
        
        // Récupérer tous les employés
        List<Employee> employees = employeeService.getAllEmployees();
        
        // Appliquer les filtres si présents
        if (department != null && !department.isEmpty()) {
            employees = employeeService.filterByDepartment(employees, department);
        }
        
        if (status != null && !status.isEmpty()) {
            employees = employeeService.filterByStatus(employees, status);
        }
        
        if (employeeId != null && !employeeId.isEmpty()) {
            employees = employees.stream()
                    .filter(e -> e.getId().equals(employeeId))
                    .collect(Collectors.toList());
        }
        
        // Récupérer les salaires pour chaque employé
        List<Map<String, Object>> salaryData = new ArrayList<>();
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        
        for (Employee employee : employees) {
            List<Salary> employeeSalaries = employeeService.getEmployeeSalaries(employee.getId());
            
            // Filtrer par mois si spécifié
            if (month != null && !month.isEmpty()) {
                employeeSalaries = employeeSalaries.stream()
                        .filter(s -> s.getMonth().equals(month))
                        .collect(Collectors.toList());
            }
            
            for (Salary salary : employeeSalaries) {
                Map<String, Object> salaryInfo = new HashMap<>();
                salaryInfo.put("id", salary.getId());
                salaryInfo.put("employeeId", employee.getId());
                salaryInfo.put("employeeName", employee.getName());
                salaryInfo.put("department", employee.getDepartment());
                salaryInfo.put("month", salary.getMonth());
                salaryInfo.put("paymentDate", salary.getPaymentDate());
                salaryInfo.put("grossAmount", salary.getGrossAmount());
                salaryInfo.put("netAmount", salary.getNetAmount());
                salaryInfo.put("taxAmount", salary.getTaxAmount());
                salaryInfo.put("status", salary.getStatus());
                
                salaryData.add(salaryInfo);
                
                // Calculer les totaux
                totalGross = totalGross.add(salary.getGrossAmount());
                totalNet = totalNet.add(salary.getNetAmount());
                totalTax = totalTax.add(salary.getTaxAmount());
            }
        }
        
        // Générer la liste des mois disponibles (12 derniers mois)
        List<Map<String, String>> availableMonths = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        
        for (int i = 0; i < 12; i++) {
            Map<String, String> monthInfo = new HashMap<>();
            monthInfo.put("value", currentMonth.format(monthFormatter));
            monthInfo.put("display", currentMonth.format(displayFormatter));
            availableMonths.add(monthInfo);
            currentMonth = currentMonth.minusMonths(1);
        }
        
        model.addAttribute("salaryData", salaryData);
        model.addAttribute("availableMonths", availableMonths);
        model.addAttribute("employees", employees);
        model.addAttribute("totalGross", totalGross);
        model.addAttribute("totalNet", totalNet);
        model.addAttribute("totalTax", totalTax);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedEmployee", employeeId);
        model.addAttribute("selectedDepartment", department);
        model.addAttribute("selectedStatus", status);
        
        return "salaries";
    }
    
    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> getSalaryPdf(@PathVariable String id) {
        if (!sessionService.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Ici, vous pourriez implémenter la génération de PDF pour une fiche de paie
        // Pour l'instant, nous utilisons la même méthode que pour les employés
        byte[] pdfBytes = employeeService.generateEmployeePdf(id);
        if (pdfBytes == null) {
            return ResponseEntity.notFound().build();
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "salary-slip-" + id + ".pdf");
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
    
    // API REST pour les requêtes AJAX
    @GetMapping("/api/list")
    @ResponseBody
    public List<Map<String, Object>> getSalariesList(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String employeeId) {
        
        List<Employee> employees = employeeService.getAllEmployees();
        List<Map<String, Object>> salaryData = new ArrayList<>();
        
        if (employeeId != null && !employeeId.isEmpty()) {
            employees = employees.stream()
                    .filter(e -> e.getId().equals(employeeId))
                    .collect(Collectors.toList());
        }
        
        for (Employee employee : employees) {
            List<Salary> employeeSalaries = employeeService.getEmployeeSalaries(employee.getId());
            
            if (month != null && !month.isEmpty()) {
                employeeSalaries = employeeSalaries.stream()
                        .filter(s -> s.getMonth().equals(month))
                        .collect(Collectors.toList());
            }
            
            for (Salary salary : employeeSalaries) {
                Map<String, Object> salaryInfo = new HashMap<>();
                salaryInfo.put("id", salary.getId());
                salaryInfo.put("employeeId", employee.getId());
                salaryInfo.put("employeeName", employee.getName());
                salaryInfo.put("department", employee.getDepartment());
                salaryInfo.put("month", salary.getMonth());
                salaryInfo.put("paymentDate", salary.getPaymentDate());
                salaryInfo.put("grossAmount", salary.getGrossAmount());
                salaryInfo.put("netAmount", salary.getNetAmount());
                salaryInfo.put("taxAmount", salary.getTaxAmount());
                salaryInfo.put("status", salary.getStatus());
                
                salaryData.add(salaryInfo);
            }
        }
        
        return salaryData;
    }
}