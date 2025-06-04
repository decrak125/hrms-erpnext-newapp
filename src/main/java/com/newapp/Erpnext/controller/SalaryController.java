package com.newapp.Erpnext.controller;

import com.newapp.Erpnext.models.Employee;
import com.newapp.Erpnext.models.Salary;
import com.newapp.Erpnext.services.EmployeeService;
import com.newapp.Erpnext.services.SessionService;
import com.newapp.Erpnext.services.SalaryService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.text.NumberFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/salaries")
public class SalaryController {

    private final EmployeeService employeeService;
    private final SessionService sessionService;
    private final SalaryService salaryService;
    private static final Logger logger = LoggerFactory.getLogger(SalaryController.class);

    public SalaryController(EmployeeService employeeService, SessionService sessionService, SalaryService salaryService) {
        this.employeeService = employeeService;
        this.sessionService = sessionService;
        this.salaryService = salaryService;
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
                logger.debug("Ajout du salaire avec ID: {} pour l'employé {}", salary.getId(), employee.getName());
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
        
        // Générer la liste des mois disponibles
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
    @ResponseBody
    public ResponseEntity<byte[]> getSalaryPdf(@PathVariable String id) {
        if (!sessionService.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            byte[] pdfContent = salaryService.generatePayslip(id);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.inline()
                .filename("bulletin-paie.pdf")
                .build());

            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Erreur lors de la génération du PDF pour le salaire {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, headerFont));
        cell.setBackgroundColor(Color.DARK_GRAY);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addEmployeeInfoCell(PdfPTable table, String label, String value) {
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        
        PdfPCell labelCell = new PdfPCell(new Phrase(label, boldFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "-", normalFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(valueCell);
    }

    private void addSalaryRow(PdfPTable table, String description, BigDecimal base, String rate, BigDecimal amount) {
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        
        table.addCell(new Phrase(description, normalFont));
        table.addCell(new Phrase(formatCurrency(base), normalFont));
        table.addCell(new Phrase(rate, normalFont));
        
        PdfPCell amountCell = new PdfPCell(new Phrase(formatCurrency(amount), normalFont));
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(amountCell);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "-";
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        return formatter.format(amount);
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "-";
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String formatMonth(String monthStr) {
        if (monthStr == null) return "-";
        try {
            YearMonth ym = YearMonth.parse(monthStr);
            return ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRANCE));
        } catch (Exception e) {
            return monthStr;
        }
    }
}