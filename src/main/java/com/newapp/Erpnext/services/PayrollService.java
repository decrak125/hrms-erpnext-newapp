package com.newapp.Erpnext.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newapp.Erpnext.models.Salary;
import com.newapp.Erpnext.models.Employee;
import com.newapp.Erpnext.models.SalaryComponent;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.colors.DeviceRgb;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PayrollService {
    private static final Logger logger = LoggerFactory.getLogger(PayrollService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Value("${erpnext.base.url:http://erpnext.localhost:8000/api}")
    private String baseUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SessionService sessionService;

    public Map<String, Object> getSalarySlipDetails(String salarySlipId, HttpSession session) throws Exception {
        if (!sessionService.isAuthenticated()) {
            throw new IllegalStateException("Invalid session");
        }

        String url = baseUrl + "/resource/Salary Slip/" + salarySlipId;
        HttpHeaders headers = createHeaders(session);
        HttpEntity<?> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                logger.error("Failed to fetch salary slip {}: {}", salarySlipId, response.getStatusCode());
                throw new Exception("Unable to fetch salary slip details");
            }

            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            Map<String, Object> result = new HashMap<>();
            result.put("salary", mapToSalary(data));
            result.put("components", parseSalaryComponents(data));
            return result;
        } catch (Exception e) {
            logger.error("Error fetching salary slip {}: {}", salarySlipId, e.getMessage());
            throw new Exception("Error fetching salary slip: " + e.getMessage(), e);
        }
    }

    public List<Salary> getSalarySlipsForMonth(String month, String employeeId, HttpSession session) throws Exception {
        if (!sessionService.isAuthenticated()) {
            throw new IllegalStateException("Session invalide");
        }

        StringBuilder filters = new StringBuilder("[");
        if (month != null && !month.isEmpty()) {
            filters.append("[\"month\", \"=\", \"").append(month).append("\"]");
        }
        if (employeeId != null && !employeeId.isEmpty()) {
            if (filters.length() > 1) {
                filters.append(",");
            }
            filters.append("[\"employee\", \"=\", \"").append(employeeId).append("\"]");
        }
        filters.append("]");

        String url = baseUrl + "/resource/Salary Slip?filters=" + filters + 
                    "&fields=[\"name\", \"employee\", \"employee_name\", \"posting_date\", \"month\", " +
                    "\"gross_pay\", \"net_pay\", \"tax_amount\", \"status\"]";

        HttpHeaders headers = createHeaders(session);
        HttpEntity<?> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                logger.error("Échec de la récupération des fiches de paie pour le mois {}: {}", month, response.getStatusCode());
                throw new Exception("Impossible de récupérer les fiches de paie");
            }

            List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.getBody().get("data");
            List<Salary> salaries = new ArrayList<>();
            
            if (dataList != null) {
                for (Map<String, Object> data : dataList) {
                    salaries.add(mapToSalary(data));
                }
            }

            logger.info("Récupération de {} fiches de paie pour le mois {}", salaries.size(), month);
            return salaries;

        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des fiches de paie pour le mois {}: {}", month, e.getMessage());
            throw new Exception("Erreur lors de la récupération des fiches de paie: " + e.getMessage(), e);
        }
    }

    public byte[] generateSalarySlipPdf(Salary salary, Employee employee, Map<String, Object> salaryDetails) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // En-tête du document
            Table header = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
            header.setWidth(UnitValue.createPercentValue(100));
            
            // Logo et informations de l'entreprise (colonne gauche)
            Table companyInfo = new Table(1);
            companyInfo.addCell(new Paragraph(employee.getCompany())
                .setFontSize(16)
                .setBold());
            companyInfo.addCell(new Paragraph("SIRET: " + "12345678900000")
                .setFontSize(10));
            companyInfo.addCell(new Paragraph("Adresse: 123 Rue Example")
                .setFontSize(10));
            header.addCell(companyInfo);

            // Titre et période (colonne droite)
            Table titleInfo = new Table(1);
            titleInfo.addCell(new Paragraph("BULLETIN DE PAIE")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.RIGHT));
            titleInfo.addCell(new Paragraph("Période: " + salary.getMonth())
                .setFontSize(12)
                .setTextAlignment(TextAlignment.RIGHT));
            header.addCell(titleInfo);
            
            document.add(header);
            document.add(new Paragraph("\n"));

            // Informations employé
            Table employeeInfo = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}));
            employeeInfo.setWidth(UnitValue.createPercentValue(100));
            employeeInfo.addCell(createInfoCell("Nom", employee.getName()));
            employeeInfo.addCell(createInfoCell("Matricule", employee.getId()));
            employeeInfo.addCell(createInfoCell("Département", employee.getDepartment()));
            employeeInfo.addCell(createInfoCell("Date de paiement", 
                salary.getPaymentDate() != null ? salary.getPaymentDate().format(DATE_FORMATTER) : "N/A"));
            document.add(employeeInfo);
            document.add(new Paragraph("\n"));

            // Tableau des composants de salaire
            Table salaryTable = new Table(UnitValue.createPercentArray(new float[]{4, 2, 2}));
            salaryTable.setWidth(UnitValue.createPercentValue(100));

            // En-têtes du tableau
            salaryTable.addHeaderCell(createHeaderCell("Rubrique"));
            salaryTable.addHeaderCell(createHeaderCell("Base"));
            salaryTable.addHeaderCell(createHeaderCell("Montant"));

            // Gains
            BigDecimal totalEarnings = BigDecimal.ZERO;
            @SuppressWarnings("unchecked")
            List<SalaryComponent> components = (List<SalaryComponent>) salaryDetails.get("components");
            if (components != null) {
                salaryTable.addCell(createSectionCell("GAINS"));
                salaryTable.addCell(createSectionCell(""));
                salaryTable.addCell(createSectionCell(""));

                for (SalaryComponent component : components) {
                    if ("Earning".equalsIgnoreCase(component.getType())) {
                        BigDecimal amount = calculateComponentAmount(component);
                        salaryTable.addCell(new Paragraph(component.getSalary_component()));
                        salaryTable.addCell(new Paragraph(salary.getGrossAmount().toString()));
                        salaryTable.addCell(new Paragraph(amount.toString() + " €"));
                        totalEarnings = totalEarnings.add(amount);
                    }
                }
            }

            // Retenues
            BigDecimal totalDeductions = BigDecimal.ZERO;
            if (components != null) {
                salaryTable.addCell(createSectionCell("RETENUES"));
                salaryTable.addCell(createSectionCell(""));
                salaryTable.addCell(createSectionCell(""));

                for (SalaryComponent component : components) {
                    if ("Deduction".equalsIgnoreCase(component.getType())) {
                        BigDecimal amount = calculateComponentAmount(component);
                        salaryTable.addCell(new Paragraph(component.getSalary_component()));
                        salaryTable.addCell(new Paragraph(salary.getGrossAmount().toString()));
                        salaryTable.addCell(new Paragraph(amount.toString() + " €"));
                        totalDeductions = totalDeductions.add(amount);
                    }
                }
            }

            // Totaux
            salaryTable.addCell(createTotalCell("TOTAL BRUT"));
            salaryTable.addCell(createTotalCell(""));
            salaryTable.addCell(createTotalCell(totalEarnings.toString() + " €"));

            salaryTable.addCell(createTotalCell("TOTAL RETENUES"));
            salaryTable.addCell(createTotalCell(""));
            salaryTable.addCell(createTotalCell(totalDeductions.toString() + " €"));

            salaryTable.addCell(createTotalCell("NET A PAYER"));
            salaryTable.addCell(createTotalCell(""));
            salaryTable.addCell(createTotalCell(salary.getNetAmount().toString() + " €"));

            document.add(salaryTable);

            // Pied de page
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Net imposable: " + salary.getNetAmount().subtract(salary.getTaxAmount()).toString() + " €")
                .setFontSize(10));
            document.add(new Paragraph("Généré le " + LocalDate.now().format(DATE_FORMATTER))
                .setFontSize(8)
                .setTextAlignment(TextAlignment.RIGHT));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            logger.error("Erreur lors de la génération du PDF pour le bulletin {}: {}", salary.getId(), e.getMessage());
            throw new Exception("Erreur lors de la génération du PDF: " + e.getMessage(), e);
        }
    }

    private Salary mapToSalary(Map<String, Object> data) {
        Salary salary = new Salary();
        salary.setId((String) data.get("name"));
        salary.setEmployeeId((String) data.get("employee"));
        salary.setMonth((String) data.get("month"));
        salary.setPaymentDate(data.get("posting_date") != null ? 
            LocalDate.parse((String) data.get("posting_date")) : null);
        salary.setGrossAmount(new BigDecimal(data.get("gross_pay").toString()));
        salary.setNetAmount(new BigDecimal(data.get("net_pay").toString()));
        salary.setTaxAmount(data.get("tax_amount") != null ? 
            new BigDecimal(data.get("tax_amount").toString()) : BigDecimal.ZERO);
        salary.setStatus((String) data.get("status"));
        return salary;
    }

    private List<SalaryComponent> parseSalaryComponents(Map<String, Object> data) {
        List<SalaryComponent> components = new ArrayList<>();

        // Parse earnings
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> earnings = (List<Map<String, Object>>) data.get("earnings");
        if (earnings != null) {
            for (Map<String, Object> earning : earnings) {
                SalaryComponent comp = new SalaryComponent();
                comp.setSalary_component((String) earning.get("salary_component"));
                comp.setSalary_component_abbr((String) earning.get("salary_component_abbr"));
                comp.setType("Earning");
                comp.setFormula((String) earning.get("formula"));
                components.add(comp);
            }
        }

        // Parse deductions
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> deductions = (List<Map<String, Object>>) data.get("deductions");
        if (deductions != null) {
            for (Map<String, Object> ded : deductions) {
                SalaryComponent comp = new SalaryComponent();
                comp.setSalary_component((String) ded.get("salary_component"));
                comp.setSalary_component_abbr((String) ded.get("salary_component_abbr"));
                comp.setType("Deduction");
                comp.setFormula((String) ded.get("formula"));
                components.add(comp);
            }
        }

        return components;
    }

    private HttpHeaders createHeaders(HttpSession session) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String sid = (String) session.getAttribute("importSid");
        if (sid == null || sid.trim().isEmpty()) {
            throw new IllegalStateException("Invalid session: sid missing");
        }
        headers.add("Cookie", "sid=" + sid);
        return headers;
    }

    private BigDecimal calculateComponentAmount(SalaryComponent component) {
        // Placeholder: In practice, fetch actual amounts from ERPNext earnings/deductions
        return new BigDecimal("1000.00"); // Example amount
    }

    private Paragraph createHeaderCell(String text) {
        return new Paragraph(text)
            .setBold()
            .setFontSize(11);
    }

    private Paragraph createSectionCell(String text) {
        return new Paragraph(text)
            .setBold()
            .setFontSize(10)
            .setBackgroundColor(new DeviceRgb(240, 240, 240));
    }

    private Paragraph createTotalCell(String text) {
        return new Paragraph(text)
            .setBold()
            .setFontSize(11)
            .setBackgroundColor(new DeviceRgb(220, 220, 220));
    }

    private Table createInfoCell(String label, String value) {
        Table table = new Table(1);
        table.addCell(new Paragraph(label).setFontSize(8).setFontColor(new DeviceRgb(100, 100, 100)));
        table.addCell(new Paragraph(value).setFontSize(10).setBold());
        return table;
    }
}