package com.newapp.Erpnext.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.newapp.Erpnext.models.Employee;
import com.newapp.Erpnext.services.EmployeeService;
import com.newapp.Erpnext.services.SessionService;

@Controller
@RequestMapping("/autres")
public class SalarySlipController {
    EmployeeService employeeService;
    SessionService sessionService;

    public SalarySlipController(EmployeeService employeeService, SessionService sessionService) {
        this.employeeService = employeeService;
        this.sessionService = sessionService;
    }

    @GetMapping
    public String getAllEmployees(Model model,
                              @RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String endDate) {
    if (!sessionService.isAuthenticated()) {
        return "redirect:/login";
    }

    List<Employee> employees = employeeService.getAllEmployees();
    LocalDate starDateParsed = startDate != null && !startDate.isEmpty() ? LocalDate.parse(startDate) : LocalDate.now();
    LocalDate endDateParsed = endDate != null && !endDate.isEmpty() ? LocalDate.parse(endDate) : LocalDate.now();

    model.addAttribute("employees", employees);
    model.addAttribute("endDate", endDateParsed);
    model.addAttribute("startDate", starDateParsed);
    model.addAttribute("selectedEmployee", "");
    return "autres";
    }

    

}
