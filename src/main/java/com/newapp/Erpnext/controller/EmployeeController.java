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
import java.util.List;

@Controller
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final SessionService sessionService;

    public EmployeeController(EmployeeService employeeService, SessionService sessionService) {
        this.employeeService = employeeService;
        this.sessionService = sessionService;
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
        
        // Appliquer les filtres si présents
        if (department != null && !department.isEmpty()) {
            employees = employeeService.filterByDepartment(employees, department);
        }
        
        if (status != null && !status.isEmpty()) {
            employees = employeeService.filterByStatus(employees, status);
        }
        
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            employees = employeeService.filterByHireDate(employees, start, end);
        }
        
        if (searchQuery != null && !searchQuery.isEmpty()) {
            employees = employeeService.searchEmployees(employees, searchQuery);
        }
        
        model.addAttribute("employees", employees);
        return "employees";
    }

    @GetMapping("/{id}")
    public String getEmployeeDetails(@PathVariable String id, Model model) {
        if (!sessionService.isAuthenticated()) {
            return "redirect:/login";
        }
        
        Employee employee = employeeService.getEmployeeById(id);
        if (employee == null) {
            return "redirect:/employees?error=not_found";
        }
        
        model.addAttribute("employee", employee);
        return "employee-details";
    }
    
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getEmployeePdf(@PathVariable String id) {
        if (!sessionService.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        byte[] pdfBytes = employeeService.generateEmployeePdf(id);
        if (pdfBytes == null) {
            return ResponseEntity.notFound().build();
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "employee-" + id + ".pdf");
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
    
    // API REST pour les requêtes AJAX
    @GetMapping("/api/list")
    @ResponseBody
    public List<Employee> getEmployeesList() {
        return employeeService.getAllEmployees();
    }
    
    @GetMapping("/api/{id}")
    @ResponseBody
    public Employee getEmployeeData(@PathVariable String id) {
        return employeeService.getEmployeeById(id);
    }
    

    @GetMapping("/api/{id}/salaries")
    @ResponseBody
    public List<Salary> getEmployeeSalaries(@PathVariable String id) {
        return employeeService.getEmployeeSalaries(id);
    }

    
}