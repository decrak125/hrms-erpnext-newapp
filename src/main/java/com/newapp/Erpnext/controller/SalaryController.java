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
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.awt.Color;
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
                if (salary.getGrossAmount() != null) {
                totalGross = totalGross.add(salary.getGrossAmount());
                }
                if (salary.getNetAmount() != null) {
                totalNet = totalNet.add(salary.getNetAmount());
                }
                if (salary.getTaxAmount() != null) {
                    totalTax = totalTax.add(salary.getTaxAmount());
                }
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

    // Route corrigée pour la génération de PDF de fiche de paie individuelle
    @GetMapping("/pdf/payslip/{id}")
    public ResponseEntity<byte[]> getSalaryPayslipPdf(@PathVariable String id) {
        try {
            System.out.println("Requête pour /salaries/pdf/payslip/" + id);
            
            if (!sessionService.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Récupérer le salaire par ID
            Salary salary = employeeService.getSalaryById(id);
            if (salary == null) {
                System.out.println("Aucun salaire trouvé pour l'ID: " + id);
                return ResponseEntity.notFound().build();
            }
            
            // Récupérer l'employé associé
            Employee employee = employeeService.getEmployeeById(salary.getEmployeeId());
            if (employee == null) {
                System.out.println("Employé non trouvé pour l'ID: " + salary.getEmployeeId());
                return ResponseEntity.notFound().build();
            }
            
            // Créer le document PDF
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Ajouter le contenu au PDF
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("FICHE DE PAIE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Informations de l'employé
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            document.add(new Paragraph("ID Employé: " + employee.getId(), normalFont));
            document.add(new Paragraph("Nom: " + employee.getName(), normalFont));
            document.add(new Paragraph("Département: " + employee.getDepartment(), normalFont));
            document.add(new Paragraph("Mois: " + salary.getMonth(), normalFont));
            document.add(new Paragraph("Montant Brut: " + salary.getGrossAmount() + " €", normalFont));
            document.add(new Paragraph("Montant Net: " + salary.getNetAmount() + " €", normalFont));
            
            document.close();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "fiche-paie-" + id + ".pdf");
            headers.setContentLength(baos.size());
            
            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
            
        } catch (Exception e) {
            System.err.println("Erreur dans getSalaryPayslipPdf: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Route alternative pour compatibilité
    @GetMapping("/pdf/salary/view")
    public void getSalaryPayslipPdfAlternate(@RequestParam("id") String id, 
                                            HttpServletRequest request,
                                            HttpServletResponse response,
                                            RedirectAttributes redirectAttributes) {
        try {
            // Vérifier l'authentification
            if (!sessionService.isAuthenticated()) {
                response.sendRedirect("/login");
                return;
            }
            
            System.out.println("Génération du PDF pour l'ID: " + id);
            
            // Décoder l'ID si nécessaire
            String decodedId = java.net.URLDecoder.decode(id, "UTF-8");
            System.out.println("ID décodé: " + decodedId);
            
            // Récupérer le salaire
            Salary salary = employeeService.getSalaryById(decodedId);
            if (salary == null) {
                response.sendRedirect("/salaries?error=" + URLEncoder.encode("Salaire non trouvé", StandardCharsets.UTF_8));
                return;
            }
            
            // Récupérer l'employé
            Employee employee = employeeService.getEmployeeById(salary.getEmployeeId());
            if (employee == null) {
                response.sendRedirect("/salaries?error=" + URLEncoder.encode("Employé non trouvé", StandardCharsets.UTF_8));
                return;
            }
            
            // Préparer les données pour le PDF
            Map<String, Object> salaryData = new HashMap<>();
            salaryData.put("id", salary.getId());
            salaryData.put("employeeId", employee.getId());
            salaryData.put("employeeName", employee.getName());
            salaryData.put("department", employee.getDepartment());
            salaryData.put("month", salary.getMonth());
            salaryData.put("paymentDate", salary.getPaymentDate());
            salaryData.put("grossAmount", salary.getGrossAmount());
            salaryData.put("netAmount", salary.getNetAmount());
            salaryData.put("taxAmount", salary.getTaxAmount());
            salaryData.put("status", salary.getStatus());
            
            // Générer le PDF
            byte[] pdfBytes = employeeService.generateSalaryPayslipPdf(salaryData);
            if (pdfBytes == null || pdfBytes.length == 0) {
                response.sendRedirect("/salaries?error=" + URLEncoder.encode("Erreur lors de la génération du PDF", StandardCharsets.UTF_8));
                return;
            }
            
            // Configurer la réponse HTTP
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=fiche-paie-" + 
                decodedId.replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf");
            response.setContentLength(pdfBytes.length);
            
            // Écrire le PDF dans la réponse
            response.getOutputStream().write(pdfBytes);
            response.getOutputStream().flush();
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération du PDF: " + e.getMessage());
            e.printStackTrace();
            try {
                response.sendRedirect("/salaries?error=" + URLEncoder.encode("Erreur lors de la génération du PDF: " + e.getMessage(), StandardCharsets.UTF_8));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // Route pour PDF employé (conservée pour compatibilité)
    @GetMapping("/pdf/{id:.+}")
    public String getSalaryPdf(@PathVariable String id, HttpServletResponse response) {
        try {
            if (!sessionService.isAuthenticated()) {
                return "redirect:/login";
            }
            
            System.out.println("Génération du PDF employé pour l'ID: " + id);
            
            // Décoder l'ID si nécessaire
            String decodedId = java.net.URLDecoder.decode(id, "UTF-8");
            System.out.println("ID décodé: " + decodedId);
            
            byte[] pdfBytes = employeeService.generateEmployeePdf(decodedId);
            if (pdfBytes == null) {
                System.out.println("Aucun PDF généré pour l'employé ID: " + decodedId);
                return "redirect:/error?message=PDF non trouvé";
            }
            
            // Configuration de la réponse HTTP
            response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            response.setHeader("Content-Disposition", "attachment; filename=salary-slip-" + 
                decodedId.replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf");
            response.getOutputStream().write(pdfBytes);
            response.getOutputStream().flush();
            
            return null; // Retourne null car la réponse a déjà été écrite
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération du PDF: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/error?message=" + URLEncoder.encode("Erreur lors de la génération du PDF: " + e.getMessage(), StandardCharsets.UTF_8);
        }
    }

    // Endpoint pour débugger les routes disponibles
    @GetMapping("/debug/routes")
    @ResponseBody
    public Map<String, String> getAvailableRoutes() {
        Map<String, String> routes = new HashMap<>();
        routes.put("Liste des salaires", "GET /salaries");
        routes.put("PDF fiche de paie", "GET /salaries/pdf/payslip/{id}");
        routes.put("PDF fiche de paie (alternatif)", "GET /salaries/pdf/salary/view");
        routes.put("PDF employé", "GET /salaries/pdf/{id}");
        routes.put("PDF mensuel", "GET /salaries/pdf/month/{month}");
        routes.put("API liste", "GET /salaries/api/list");
        return routes;
    }
}