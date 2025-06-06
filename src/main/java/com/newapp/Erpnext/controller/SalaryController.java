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
<<<<<<< Updated upstream
import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.text.NumberFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
=======
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.Locale;
import java.awt.Color;

// Imports pour la génération PDF
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
>>>>>>> Stashed changes

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

<<<<<<< Updated upstream
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
=======
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

    @GetMapping("/pdf/month/{month}")
    public ResponseEntity<byte[]> getMonthlyPayslipsPdf(@PathVariable String month) {
        if (!sessionService.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        byte[] pdfBytes = generateMonthlyPayslipsPdf(month);
        if (pdfBytes == null) {
            return ResponseEntity.notFound().build();
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "fiches-paie-" + month + ".pdf");
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
    
    /**
     * Génère un PDF consolidé pour toutes les fiches de paie d'un mois
     */
    private byte[] generateMonthlyPayslipsPdf(String month) {
        try {
            // Récupérer tous les employés
            List<Employee> employees = employeeService.getAllEmployees();
            
            // Récupérer les salaires pour le mois spécifié
            List<Map<String, Object>> monthlyPayslips = new ArrayList<>();
            BigDecimal totalGrossMonth = BigDecimal.ZERO;
            BigDecimal totalNetMonth = BigDecimal.ZERO;
            BigDecimal totalTaxMonth = BigDecimal.ZERO;
            
            for (Employee employee : employees) {
                List<Salary> employeeSalaries = employeeService.getEmployeeSalaries(employee.getId());
                
                // Filtrer par mois
                List<Salary> monthlySalaries = employeeSalaries.stream()
                        .filter(s -> s.getMonth() != null && s.getMonth().equals(month))
                        .collect(Collectors.toList());
                
                for (Salary salary : monthlySalaries) {
                    Map<String, Object> payslip = new HashMap<>();
                    payslip.put("employee", employee);
                    payslip.put("salary", salary);
                    monthlyPayslips.add(payslip);
                    
                    // Calculer les totaux
                    if (salary.getGrossAmount() != null) {
                        totalGrossMonth = totalGrossMonth.add(salary.getGrossAmount());
                    }
                    if (salary.getNetAmount() != null) {
                        totalNetMonth = totalNetMonth.add(salary.getNetAmount());
                    }
                    if (salary.getTaxAmount() != null) {
                        totalTaxMonth = totalTaxMonth.add(salary.getTaxAmount());
                    }
                }
            }
            
            if (monthlyPayslips.isEmpty()) {
                return null; // Aucune fiche de paie pour ce mois
            }
            
            // Créer le document PDF
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Titre principal
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.BLACK);
            Paragraph title = new Paragraph("FICHES DE PAIE - " + formatMonthForDisplay(month), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Résumé des totaux
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
            Paragraph summaryTitle = new Paragraph("Résumé mensuel", sectionFont);
            summaryTitle.setSpacingAfter(10);
            document.add(summaryTitle);
            
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.FRANCE);
            
            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(20);
            
            // En-têtes du résumé
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            
            PdfPCell headerCell1 = new PdfPCell(new Phrase("Nombre d'employés", headerFont));
            headerCell1.setBackgroundColor(Color.DARK_GRAY);
            headerCell1.setPadding(8);
            summaryTable.addCell(headerCell1);
            
            PdfPCell headerCell2 = new PdfPCell(new Phrase("Total Brut", headerFont));
            headerCell2.setBackgroundColor(Color.DARK_GRAY);
            headerCell2.setPadding(8);
            summaryTable.addCell(headerCell2);
            
            PdfPCell headerCell3 = new PdfPCell(new Phrase("Total Net", headerFont));
            headerCell3.setBackgroundColor(Color.DARK_GRAY);
            headerCell3.setPadding(8);
            summaryTable.addCell(headerCell3);
            
            PdfPCell headerCell4 = new PdfPCell(new Phrase("Total Impôts", headerFont));
            headerCell4.setBackgroundColor(Color.DARK_GRAY);
            headerCell4.setPadding(8);
            summaryTable.addCell(headerCell4);
            
            // Données du résumé
            summaryTable.addCell(new Phrase(String.valueOf(monthlyPayslips.size())));
            summaryTable.addCell(new Phrase(currencyFormatter.format(totalGrossMonth)));
            summaryTable.addCell(new Phrase(currencyFormatter.format(totalNetMonth)));
            summaryTable.addCell(new Phrase(currencyFormatter.format(totalTaxMonth)));
            
            document.add(summaryTable);
            
            // Détail des fiches de paie
            Paragraph detailTitle = new Paragraph("Détail des fiches de paie", sectionFont);
            detailTitle.setSpacingAfter(10);
            document.add(detailTitle);
            
            PdfPTable detailTable = new PdfPTable(7);
            detailTable.setWidthPercentage(100);
            detailTable.setWidths(new float[]{3f, 2f, 2f, 2f, 2f, 2f, 2f});
            
            // En-têtes du tableau détaillé
            String[] headers = {"Employé", "Département", "Poste", "Montant Brut", "Montant Net", "Impôts", "Statut"};
            for (String header : headers) {
                PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
                headerCell.setBackgroundColor(Color.DARK_GRAY);
                headerCell.setPadding(5);
                detailTable.addCell(headerCell);
            }
            
            // Données du tableau
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            for (Map<String, Object> payslip : monthlyPayslips) {
                Employee employee = (Employee) payslip.get("employee");
                Salary salary = (Salary) payslip.get("salary");
                
                detailTable.addCell(new Phrase(employee.getName() != null ? employee.getName() : "-", normalFont));
                detailTable.addCell(new Phrase(employee.getDepartment() != null ? employee.getDepartment() : "-", normalFont));
                detailTable.addCell(new Phrase(employee.getPosition() != null ? employee.getPosition() : "-", normalFont));
                detailTable.addCell(new Phrase(salary.getGrossAmount() != null ? currencyFormatter.format(salary.getGrossAmount()) : "-", normalFont));
                detailTable.addCell(new Phrase(salary.getNetAmount() != null ? currencyFormatter.format(salary.getNetAmount()) : "-", normalFont));
                detailTable.addCell(new Phrase(salary.getTaxAmount() != null ? currencyFormatter.format(salary.getTaxAmount()) : "-", normalFont));
                detailTable.addCell(new Phrase(salary.getStatus() != null ? salary.getStatus() : "-", normalFont));
            }
            
            document.add(detailTable);
            
            // Nouvelle page pour les fiches de paie individuelles
            document.newPage();
            
            // Générer les fiches de paie individuelles
            Paragraph individualTitle = new Paragraph("FICHES DE PAIE INDIVIDUELLES", titleFont);
            individualTitle.setAlignment(Element.ALIGN_CENTER);
            individualTitle.setSpacingAfter(30);
            document.add(individualTitle);
            
            for (int i = 0; i < monthlyPayslips.size(); i++) {
                Map<String, Object> payslip = monthlyPayslips.get(i);
                Employee employee = (Employee) payslip.get("employee");
                Salary salary = (Salary) payslip.get("salary");
                
                // Ajouter une nouvelle page pour chaque employé (sauf le premier)
                if (i > 0) {
                    document.newPage();
                }
                
                generateIndividualPayslip(document, employee, salary, month);
            }
            
            // Pied de page
            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("Document généré le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
            
            document.close();
            
            return baos.toByteArray();
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération du PDF mensuel: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Génère une fiche de paie individuelle dans le document
     */
    private void generateIndividualPayslip(Document document, Employee employee, Salary salary, String month) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        // Titre de la fiche de paie
        Paragraph payslipTitle = new Paragraph("FICHE DE PAIE", titleFont);
        payslipTitle.setAlignment(Element.ALIGN_CENTER);
        payslipTitle.setSpacingAfter(10);
        document.add(payslipTitle);
        
        Paragraph monthTitle = new Paragraph(formatMonthForDisplay(month), boldFont);
        monthTitle.setAlignment(Element.ALIGN_CENTER);
        monthTitle.setSpacingAfter(20);
        document.add(monthTitle);
        
        // Informations de l'employé
        PdfPTable employeeTable = new PdfPTable(2);
        employeeTable.setWidthPercentage(100);
        employeeTable.setSpacingAfter(15);
        
        // Colonne gauche - Informations employé
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.BOX);
        leftCell.setPadding(10);
        
        Paragraph empInfo = new Paragraph();
        empInfo.add(new Chunk("INFORMATIONS EMPLOYÉ\n", boldFont));
        empInfo.add(new Chunk("Nom: " + (employee.getName() != null ? employee.getName() : "-") + "\n", normalFont));
        empInfo.add(new Chunk("ID: " + (employee.getId() != null ? employee.getId() : "-") + "\n", normalFont));
        empInfo.add(new Chunk("Département: " + (employee.getDepartment() != null ? employee.getDepartment() : "-") + "\n", normalFont));
        empInfo.add(new Chunk("Poste: " + (employee.getPosition() != null ? employee.getPosition() : "-") + "\n", normalFont));
        leftCell.addElement(empInfo);
        
        // Colonne droite - Informations paie
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.BOX);
        rightCell.setPadding(10);
        
        Paragraph payInfo = new Paragraph();
        payInfo.add(new Chunk("INFORMATIONS PAIE\n", boldFont));
        payInfo.add(new Chunk("Période: " + formatMonthForDisplay(month) + "\n", normalFont));
        payInfo.add(new Chunk("Date de paiement: " + (salary.getPaymentDate() != null ? salary.getPaymentDate().format(dateFormatter) : "-") + "\n", normalFont));
        payInfo.add(new Chunk("Statut: " + (salary.getStatus() != null ? salary.getStatus() : "-") + "\n", normalFont));
        rightCell.addElement(payInfo);
        
        employeeTable.addCell(leftCell);
        employeeTable.addCell(rightCell);
        document.add(employeeTable);
        
        // Détail des montants
        PdfPTable amountTable = new PdfPTable(2);
        amountTable.setWidthPercentage(100);
        amountTable.setSpacingAfter(20);
        
        // En-tête
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
        PdfPCell headerCell1 = new PdfPCell(new Phrase("Description", headerFont));
        headerCell1.setBackgroundColor(Color.DARK_GRAY);
        headerCell1.setPadding(8);
        amountTable.addCell(headerCell1);
        
        PdfPCell headerCell2 = new PdfPCell(new Phrase("Montant", headerFont));
        headerCell2.setBackgroundColor(Color.DARK_GRAY);
        headerCell2.setPadding(8);
        amountTable.addCell(headerCell2);
        
        // Salaire brut
        amountTable.addCell(new Phrase("Salaire Brut", boldFont));
        amountTable.addCell(new Phrase(salary.getGrossAmount() != null ? currencyFormatter.format(salary.getGrossAmount()) : "-", normalFont));
        
        // Déductions/Impôts
        amountTable.addCell(new Phrase("Déductions/Impôts", normalFont));
        amountTable.addCell(new Phrase(salary.getTaxAmount() != null ? "- " + currencyFormatter.format(salary.getTaxAmount()) : "-", normalFont));
        
        // Ligne de séparation
        PdfPCell separatorCell1 = new PdfPCell(new Phrase("", normalFont));
        separatorCell1.setBorderWidth(2);
        separatorCell1.setBorderColor(Color.BLACK);
        separatorCell1.setPadding(2);
        amountTable.addCell(separatorCell1);
        
        PdfPCell separatorCell2 = new PdfPCell(new Phrase("", normalFont));
        separatorCell2.setBorderWidth(2);
        separatorCell2.setBorderColor(Color.BLACK);
        separatorCell2.setPadding(2);
        amountTable.addCell(separatorCell2);
        
        // Salaire net
        PdfPCell netLabelCell = new PdfPCell(new Phrase("SALAIRE NET", boldFont));
        netLabelCell.setBackgroundColor(Color.LIGHT_GRAY);
        netLabelCell.setPadding(8);
        amountTable.addCell(netLabelCell);
        
        PdfPCell netAmountCell = new PdfPCell(new Phrase(salary.getNetAmount() != null ? currencyFormatter.format(salary.getNetAmount()) : "-", boldFont));
        netAmountCell.setBackgroundColor(Color.LIGHT_GRAY);
        netAmountCell.setPadding(8);
        amountTable.addCell(netAmountCell);
        
        document.add(amountTable);
        
        // Signature et date
        Paragraph signature = new Paragraph("Signature de l'employeur: ________________________     Date: " + LocalDate.now().format(dateFormatter), normalFont);
        signature.setSpacingBefore(30);
        document.add(signature);
    }
    
    /**
     * Formate le mois pour l'affichage (YYYY-MM vers Mois YYYY)
     */
    private String formatMonthForDisplay(String month) {
        try {
            YearMonth yearMonth = YearMonth.parse(month);
            return yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH));
        } catch (Exception e) {
            return month;
        }
    }
    
    // API REST pour les requêtes AJAX
    @GetMapping("/api/list")
    @ResponseBody
    public List<Map<String, Object>> getSalariesList(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String employeeId) {
>>>>>>> Stashed changes
        
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
<<<<<<< Updated upstream
=======
        
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
>>>>>>> Stashed changes
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