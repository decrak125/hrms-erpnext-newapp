package com.newapp.Erpnext.services;

import com.newapp.Erpnext.models.Employee;
import com.newapp.Erpnext.models.Salary;
import org.springframework.stereotype.Service;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.text.NumberFormat;
import java.awt.Color;
import java.math.BigDecimal;

@Service
public class SalaryService {

    private final EmployeeService employeeService;

    public SalaryService(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    public byte[] generatePayslip(String salaryId) throws Exception {
        // Récupérer le salaire et l'employé
        Salary salary = getSalaryById(salaryId); // Fixed: use local method instead of employeeService
        if (salary == null) {
            throw new RuntimeException("Salaire non trouvé");
        }

        Employee employee = employeeService.getEmployeeById(salary.getEmployeeId());
        if (employee == null) {
            throw new RuntimeException("Employé non trouvé");
        }

        // Créer le document PDF
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);

        document.open();

        // Ajouter le contenu du PDF
        addHeader(document);
        addTitle(document);
        addEmployeeInfo(document, employee, salary);
        addSalaryDetails(document, salary);
        addFooter(document);

        document.close();
        return baos.toByteArray();
    }

    // Method to get salary by ID - you'll need to implement the actual data retrieval logic
    private Salary getSalaryById(String salaryId) {
        // TODO: Implement actual salary retrieval logic
        // This could be from a repository, database service, etc.
        // For now, returning null - replace with your actual implementation
        return null;
    }

    private void addHeader(Document document) throws Exception {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setSpacingAfter(20);

        // Logo
        PdfPCell logoCell = new PdfPCell(new Paragraph("LOGO", headerFont));
        logoCell.setBorder(Rectangle.NO_BORDER);
        header.addCell(logoCell);

        // Infos entreprise
        PdfPCell companyCell = new PdfPCell();
        companyCell.setBorder(Rectangle.NO_BORDER);
        companyCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph companyInfo = new Paragraph();
        companyInfo.add(new Chunk("Votre Entreprise\n", titleFont));
        companyInfo.add(new Chunk("123 Rue de l'Entreprise\n", normalFont));
        companyInfo.add(new Chunk("75000 Paris\n", normalFont));
        companyInfo.add(new Chunk("SIRET: 123 456 789 00000", normalFont));
        companyCell.addElement(companyInfo);
        header.addCell(companyCell);

        document.add(header);
    }

    private void addTitle(Document document) throws Exception {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        
        Paragraph title = new Paragraph("BULLETIN DE PAIE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        
        document.add(title);
    }

    private void addEmployeeInfo(Document document, Employee employee, Salary salary) throws Exception {
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        PdfPTable employeeTable = new PdfPTable(2);
        employeeTable.setWidthPercentage(100);
        employeeTable.setSpacingAfter(15);

        // Employee information
        PdfPCell employeeInfoCell = new PdfPCell();
        employeeInfoCell.setBorder(Rectangle.BOX);
        employeeInfoCell.setPadding(10);
        
        Paragraph employeeInfo = new Paragraph();
        employeeInfo.add(new Chunk("INFORMATIONS EMPLOYÉ\n", boldFont));
        employeeInfo.add(new Chunk("Nom: " + (employee.getName() != null ? employee.getName() : "") + "\n", normalFont));
        employeeInfo.add(new Chunk("Matricule: " + (employee.getId() != null ? employee.getId() : "") + "\n", normalFont));
        employeeInfo.add(new Chunk("Poste: " + (employee.getPosition() != null ? employee.getPosition() : "") + "\n", normalFont));
        employeeInfo.add(new Chunk("Département: " + (employee.getDepartment() != null ? employee.getDepartment() : "") + "\n", normalFont));
        
        employeeInfoCell.addElement(employeeInfo);
        employeeTable.addCell(employeeInfoCell);

        // Salary period information
        PdfPCell periodInfoCell = new PdfPCell();
        periodInfoCell.setBorder(Rectangle.BOX);
        periodInfoCell.setPadding(10);
        
        Paragraph periodInfo = new Paragraph();
        periodInfo.add(new Chunk("PÉRIODE DE PAIE\n", boldFont));
        
        // Format the salary date if available
        String salaryPeriod = "N/A";
        if (salary.getMonth() != null) {
            // Convert "YYYY-MM" format to readable format
            String[] dateParts = salary.getMonth().split("-");
            if (dateParts.length == 2) {
                String[] months = {"", "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                                 "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
                int monthNum = Integer.parseInt(dateParts[1]);
                if (monthNum >= 1 && monthNum <= 12) {
                    salaryPeriod = months[monthNum] + " " + dateParts[0];
                }
            }
        }
        
        periodInfo.add(new Chunk("Période: " + salaryPeriod + "\n", normalFont));
        periodInfo.add(new Chunk("Date d'émission: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n", normalFont));
        
        periodInfoCell.addElement(periodInfo);
        employeeTable.addCell(periodInfoCell);

        document.add(employeeTable);
    }

    private void addSalaryDetails(Document document, Salary salary) throws Exception {
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE);

        // Salary details table
        PdfPTable salaryTable = new PdfPTable(3);
        salaryTable.setWidthPercentage(100);
        salaryTable.setWidths(new float[]{3f, 1f, 1f});
        salaryTable.setSpacingAfter(15);

        // Table headers
        PdfPCell[] headers = {
            new PdfPCell(new Paragraph("DESCRIPTION", boldFont)),
            new PdfPCell(new Paragraph("MONTANT", boldFont)),
            new PdfPCell(new Paragraph("TYPE", boldFont))
        };

        for (PdfPCell header : headers) {
            header.setBackgroundColor(Color.LIGHT_GRAY);
            header.setPadding(8);
            header.setHorizontalAlignment(Element.ALIGN_CENTER);
            salaryTable.addCell(header);
        }

        // Salary base (using grossAmount as base salary)
        addSalaryRow(salaryTable, "Salaire brut", salary.getGrossAmount(), "Gain", normalFont, currencyFormat);
        
        // Calculate deductions (gross - net)
        BigDecimal totalDeductions = salary.getGrossAmount().subtract(salary.getNetAmount());
        if (totalDeductions.compareTo(BigDecimal.ZERO) > 0) {
            addSalaryRow(salaryTable, "Cotisations sociales", totalDeductions.subtract(salary.getTaxAmount() != null ? salary.getTaxAmount() : BigDecimal.ZERO), "Retenue", normalFont, currencyFormat);
        }

        // Taxes
        if (salary.getTaxAmount() != null && salary.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            addSalaryRow(salaryTable, "Impôts sur le revenu", salary.getTaxAmount(), "Retenue", normalFont, currencyFormat);
        }

        document.add(salaryTable);

        // Net salary summary
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingAfter(20);

        PdfPCell netSalaryLabel = new PdfPCell(new Paragraph("SALAIRE NET À PAYER", boldFont));
        netSalaryLabel.setBorder(Rectangle.BOX);
        netSalaryLabel.setPadding(10);
        netSalaryLabel.setBackgroundColor(Color.LIGHT_GRAY);
        
        PdfPCell netSalaryAmount = new PdfPCell(new Paragraph(currencyFormat.format(salary.getNetAmount()), boldFont));
        netSalaryAmount.setBorder(Rectangle.BOX);
        netSalaryAmount.setPadding(10);
        netSalaryAmount.setHorizontalAlignment(Element.ALIGN_RIGHT);
        netSalaryAmount.setBackgroundColor(Color.LIGHT_GRAY);

        summaryTable.addCell(netSalaryLabel);
        summaryTable.addCell(netSalaryAmount);

        document.add(summaryTable);
    }

    private void addSalaryRow(PdfPTable table, String description, BigDecimal amount, String type, 
                             Font font, NumberFormat currencyFormat) {
        table.addCell(new PdfPCell(new Paragraph(description, font)));
        
        PdfPCell amountCell = new PdfPCell(new Paragraph(currencyFormat.format(amount), font));
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(amountCell);
        
        table.addCell(new PdfPCell(new Paragraph(type, font)));
    }

    private void addFooter(Document document) throws Exception {
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

        // Add some spacing
        Paragraph spacing = new Paragraph(" ");
        spacing.setSpacingBefore(20);
        document.add(spacing);

        // Legal notice
        Paragraph legalNotice = new Paragraph();
        legalNotice.add(new Chunk("MENTIONS LÉGALES\n", boldFont));
        legalNotice.add(new Chunk("Ce bulletin de paie est conforme à la législation française en vigueur. " +
                                "Conservez ce document, il pourra vous être demandé pour justifier de vos revenus.\n\n", footerFont));
        
        legalNotice.add(new Chunk("Document généré le: " + 
                               LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) + "\n", footerFont));
        
        legalNotice.setAlignment(Element.ALIGN_LEFT);
        legalNotice.setSpacingBefore(10);
        
        document.add(legalNotice);

        // Signature section
        PdfPTable signatureTable = new PdfPTable(2);
        signatureTable.setWidthPercentage(100);
        signatureTable.setSpacingBefore(30);

        PdfPCell employerSignature = new PdfPCell();
        employerSignature.setBorder(Rectangle.NO_BORDER);
        Paragraph employerSig = new Paragraph();
        employerSig.add(new Chunk("Signature de l'employeur\n\n\n", boldFont));
        employerSig.add(new Chunk("_____________________", footerFont));
        employerSignature.addElement(employerSig);

        PdfPCell employeeSignature = new PdfPCell();
        employeeSignature.setBorder(Rectangle.NO_BORDER);
        employeeSignature.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph employeeSig = new Paragraph();
        employeeSig.add(new Chunk("Signature de l'employé\n\n\n", boldFont));
        employeeSig.add(new Chunk("_____________________", footerFont));
        employeeSignature.addElement(employeeSig);

        signatureTable.addCell(employerSignature);
        signatureTable.addCell(employeeSignature);

        document.add(signatureTable);
    }
}