package com.newapp.Erpnext.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newapp.Erpnext.entity.EmployeeEntity;
import com.newapp.Erpnext.models.Employee;
import com.newapp.Erpnext.models.Salary;
import com.newapp.Erpnext.repository.EmployeeRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.awt.Color;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Value("${erpnext.url}")
    private String erpNextUrl;
    
    private final RestTemplate restTemplate;
    private final ErpNextAuthService authService;


    // public List<EmployeeEntity> getFirst5Employees() {
    //     return employeeRepository.findFirst5();
    // }
    
    // public Long getTotalEmployeeCount() {
    //     return employeeRepository.countEmployees();
    // }
    
    // public List<EmployeeEntity> getAllEmp() {
    //     return employeeRepository.findAll();
    // }
    
    public EmployeeService(RestTemplateBuilder restTemplateBuilder, ErpNextAuthService authService) {
        this.restTemplate = restTemplateBuilder.build();
        this.authService = authService;
    }
    

    /**
     * Crée les en-têtes HTTP avec le token d'authentification
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + authService.getApiAuthToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
    
    /**
     * Récupère tous les employés
     */
    public List<Employee> getAllEmployees() {
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        String url = erpNextUrl + "/api/resource/Employee?fields=[\"name\",\"employee_name\",\"department\",\"designation\",\"status\",\"date_of_joining\"]";
        
        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            Map.class
        );
        
        List<Employee> employees = new ArrayList<>();
        
        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
            
            for (Map<String, Object> item : data) {
                Employee employee = new Employee();
                employee.setId((String) item.get("name"));
                employee.setName((String) item.get("employee_name"));
                employee.setDepartment((String) item.get("department"));
                employee.setPosition((String) item.get("designation"));
                employee.setStatus((String) item.get("status"));
                
                // Conversion de la date d'embauche
                if (item.get("date_of_joining") != null) {
                    String dateStr = (String) item.get("date_of_joining");
                    employee.setHireDate(LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE));
                }
                
                employees.add(employee);
            }
        }
        
        return employees;
    }
    
    /**
     * Filtre les employés par département
     */
    public List<Employee> filterByDepartment(List<Employee> employees, String department) {
        if (department == null || department.isEmpty()) {
            return employees;
        }
        
        return employees.stream()
                .filter(e -> department.equals(e.getDepartment()))
                .collect(Collectors.toList());
    }
    
    /**
     * Filtre les employés par statut
     */
    public List<Employee> filterByStatus(List<Employee> employees, String status) {
        if (status == null || status.isEmpty()) {
            return employees;
        }
        
        return employees.stream()
                .filter(e -> status.equals(e.getStatus()))
                .collect(Collectors.toList());
    }
    
    /**
     * Filtre les employés par date d'embauche
     */
    public List<Employee> filterByHireDate(List<Employee> employees, LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return employees;
        }
        
        return employees.stream()
                .filter(e -> {
                    if (e.getHireDate() == null) return false;
                    
                    boolean afterStartDate = startDate == null || !e.getHireDate().isBefore(startDate);
                    boolean beforeEndDate = endDate == null || !e.getHireDate().isAfter(endDate);
                    
                    return afterStartDate && beforeEndDate;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Recherche des employés par nom ou ID
     */
    public List<Employee> searchEmployees(List<Employee> employees, String query) {
        if (query == null || query.isEmpty()) {
            return employees;
        }
        
        String lowerQuery = query.toLowerCase();
        
        return employees.stream()
                .filter(e -> 
                    (e.getName() != null && e.getName().toLowerCase().contains(lowerQuery)) ||
                    (e.getId() != null && e.getId().toLowerCase().contains(lowerQuery))
                )
                .collect(Collectors.toList());
    }
    
    /**
     * Récupère les détails d'un employé par son ID
     */
    public Employee getEmployeeById(String id) {
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        String url = erpNextUrl + "/api/resource/Employee/" + id;
        
        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            Map.class
        );
        
        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> responseBody = response.getBody();
            Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
            
            Employee employee = new Employee();
            employee.setId((String) data.get("name"));
            employee.setName((String) data.get("employee_name"));
            employee.setDepartment((String) data.get("department"));
            employee.setPosition((String) data.get("designation"));
            employee.setEmail((String) data.get("company_email"));
            employee.setPhone((String) data.get("cell_number"));
            employee.setAddress((String) data.get("current_address"));
            employee.setStatus((String) data.get("status"));
            employee.setContractType((String) data.get("employment_type"));
            
            // Conversion de la date d'embauche
            if (data.get("date_of_joining") != null) {
                String dateStr = (String) data.get("date_of_joining");
                employee.setHireDate(LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE));
            }
            
            // Récupérer les salaires de l'employé
            employee.setSalaries(getEmployeeSalaries(id));
            
            return employee;
        }
        
        return null;
    }

public List<Salary> getEmployeeSalaries(String employeeId) {
    HttpHeaders headers = createHeaders();
    List<Salary> salaries = new ArrayList<>();

    try {
        // 1. Récupérer la liste des Salary Slips (simplifiée)
        String listUrl = String.format("%s/api/resource/Salary Slip?fields=[\"name\"]&filters=[[\"employee\",\"=\",\"%s\"]]", 
                                     erpNextUrl, employeeId);
        
        ResponseEntity<Map> listResponse = restTemplate.exchange(
            listUrl, 
            HttpMethod.GET, 
            new HttpEntity<>(headers), 
            Map.class
        );

        if (listResponse.getStatusCode() != HttpStatus.OK || listResponse.getBody() == null) {
            // logger.error("Erreur lors de la récupération des fiches de paie pour l'employé {}", employeeId);
            return salaries;
        }

        List<Map<String, Object>> salarySlips = (List<Map<String, Object>>) listResponse.getBody().get("data");

        // 2. Pour chaque fiche, récupérer les détails complets
        for (Map<String, Object> slip : salarySlips) {
            String slipId = (String) slip.get("name");
            String detailUrl = String.format("%s/api/resource/Salary Slip/%s", erpNextUrl, slipId);

            ResponseEntity<Map> detailResponse = restTemplate.exchange(
                detailUrl, 
                HttpMethod.GET, 
                new HttpEntity<>(headers), 
                Map.class
            );

            if (detailResponse.getStatusCode() == HttpStatus.OK && detailResponse.getBody() != null) {
                Map<String, Object> slipData = (Map<String, Object>) detailResponse.getBody().get("data");
                
                // ⚡ Construction de l'objet Salary avec TOUS les champs
                Salary salary = new Salary();
                salary.setId(slipId);
                salary.setEmployeeId(employeeId);
                
                
                // 📅 Gestion des dates (avec vérification de null)
                if (slipData.get("posting_date") != null) {
                    salary.setPaymentDate(LocalDate.parse(
                        (String) slipData.get("posting_date"), 
                        DateTimeFormatter.ISO_DATE
                    ));
                }
                
                if (slipData.get("start_date") != null) {
                    LocalDate startDate = LocalDate.parse(
                        (String) slipData.get("start_date"), 
                        DateTimeFormatter.ISO_DATE
                    );
                    salary.setMonth(startDate.getYear() + "-" + String.format("%02d", startDate.getMonthValue()));
                }

                // 💰 Gestion des montants (avec sécurité)
                salary.setGrossAmount(getBigDecimal(slipData.get("gross_pay")));
                salary.setNetAmount(getBigDecimal(slipData.get("net_pay")));
                salary.setTaxAmount(getBigDecimal(slipData.get("total_deduction")));
                
                // 🏷️ Statut et autres métadonnées
                salary.setStatus((String) slipData.get("status"));
                 // Si disponible

                // ✨ Récupération des composants (earnings/deductions)
                salary.setEarnings(extractComponents((List<Map<String, Object>>) slipData.get("earnings")));
                salary.setDeductions(extractComponents((List<Map<String, Object>>) slipData.get("deductions")));

                salaries.add(salary);
                // logger.debug("Salaire traité : {}", salary.getId());
            } else {
                // logger.warn("Échec de récupération des détails pour la fiche {}", slipId);
            }
        }
    } catch (Exception e) {
        // logger.error("Erreur critique lors de la récupération des salaires pour l'employé {} : {}", employeeId, e.getMessage(), e);
    }

    return salaries;
}

// 🛡️ Méthode helper pour convertir safely en BigDecimal
private BigDecimal getBigDecimal(Object value) {
    if (value == null) return BigDecimal.ZERO;
    try {
        return new BigDecimal(value.toString());
    } catch (NumberFormatException e) {
        // logger.warn("Erreur de conversion en BigDecimal : {}", value);
        return BigDecimal.ZERO;
    }
}

// 🔄 Extraction des composants (earnings/deductions)
private Map<String, BigDecimal> extractComponents(List<Map<String, Object>> components) {
    Map<String, BigDecimal> result = new HashMap<>();
    if (components != null) {
        for (Map<String, Object> comp : components) {
            try {
                String key = (String) comp.get("salary_component");
                BigDecimal value = getBigDecimal(comp.get("amount"));
                if (key != null && value != null) {
                    result.put(key, value);
                }
            } catch (Exception e) {
                // logger.warn("Erreur lors de l'extraction d'un composant : {}", e.getMessage());
            }
        }
    }
    return result;
}
    
    /**
     * Récupère les salaires d'un employé
     */
//     public List<Salary> getEmployeeSalaries(String employeeId) {
//         HttpHeaders headers = createHeaders();
//         HttpEntity<String> entity = new HttpEntity<>(headers);
        
//         String url = erpNextUrl + "/api/resource/Salary Slip?fields=[\"name\",\"posting_date\",\"start_date\",\"end_date\",\"gross_pay\",\"net_pay\",\"total_deduction\",\"status\",\"earnings\",\"deductions\"]&filters=[[\"employee\",\"=\",\"" + employeeId + "\"]]";        
//         ResponseEntity<Map> response = restTemplate.exchange(
//             url,
//             HttpMethod.GET,
//             entity,
//             Map.class
//         );
        
//         List<Salary> salaries = new ArrayList<>();
        
//         if (response.getStatusCode() == HttpStatus.OK) {
//             Map<String, Object> responseBody = response.getBody();
//             List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
            
//             for (Map<String, Object> item : data) {
//                 Salary salary = new Salary();
//                 salary.setId((String) item.get("name"));
//                 salary.setEmployeeId(employeeId);
                
//                 // Conversion de la date de paiement
//                 if (item.get("posting_date") != null) {
//                     String dateStr = (String) item.get("posting_date");
//                     salary.setPaymentDate(LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE));
//                 }
                
//                 // Déterminer le mois à partir des dates de début et de fin
//                 if (item.get("start_date") != null) {
//                     String startDateStr = (String) item.get("start_date");
//                     LocalDate startDate = LocalDate.parse(startDateStr, DateTimeFormatter.ISO_DATE);
//                     salary.setMonth(startDate.getYear() + "-" + String.format("%02d", startDate.getMonthValue()));
//                 }
                
//                 // Conversion des montants
//                 if (item.get("gross_pay") != null) {
//                     salary.setGrossAmount(new BigDecimal(item.get("gross_pay").toString()));
//                 }
                
//                 if (item.get("net_pay") != null) {
//                     salary.setNetAmount(new BigDecimal(item.get("net_pay").toString()));
//                 }
                
//                 if (item.get("earnings") != null) {
//     List<Map<String, Object>> earningsList = (List<Map<String, Object>>) item.get("earnings");
//     Map<String, BigDecimal> earningsMap = new HashMap<>();
//     for (Map<String, Object> earning : earningsList) {
//         earningsMap.put(
//             (String) earning.get("salary_component"),
//             new BigDecimal(earning.get("amount").toString())
//         );
//     }
//     salary.setEarnings(earningsMap);
// }

// // Récupération des déductions
// if (item.get("deductions") != null) {
//     List<Map<String, Object>> deductionsList = (List<Map<String, Object>>) item.get("deductions");
//     Map<String, BigDecimal> deductionsMap = new HashMap<>();
//     for (Map<String, Object> deduction : deductionsList) {
//         deductionsMap.put(
//             (String) deduction.get("salary_component"),
//             new BigDecimal(deduction.get("amount").toString())
//         );
//     }
//     salary.setDeductions(deductionsMap);
// }
                
//                 salary.setStatus((String) item.get("status"));
                
//                 salaries.add(salary);
//             }
//         }
        
//         return salaries;
//     }
    
    /**
     * Génère un PDF avec les informations de l'employé
     */
    public byte[] generateEmployeePdf(String employeeId) {
        ByteArrayOutputStream baos = null;
        Document document = null;
        PdfWriter writer = null;
        
        try {
            // Récupérer les données de l'employé
            Employee employee = getEmployeeById(employeeId);
            if (employee == null) {
                throw new IllegalArgumentException("Employé non trouvé avec l'ID: " + employeeId);
            }
            
            // Initialiser le document PDF
            baos = new ByteArrayOutputStream();
            document = new Document(PageSize.A4, 36, 36, 54, 36);
            writer = PdfWriter.getInstance(document, baos);
            document.open();
            
            // Polices personnalisées
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(0, 51, 102));
            Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(0, 51, 102));
            Font boldFont = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 11);
            
            // En-tête avec titre
            Paragraph title = new Paragraph("FICHE EMPLOYÉ", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Informations principales
            PdfPTable mainTable = new PdfPTable(2);
            mainTable.setWidthPercentage(100);
            mainTable.setSpacingAfter(20);
            
            // Informations personnelles
            addSection(document, "Informations Personnelles", sectionFont);
            addInfoTable(mainTable, new String[][] {
                {"ID", employee.getId()},
                {"Nom", employee.getName()},
                {"Email", employee.getEmail()},
                {"Téléphone", employee.getPhone()},
                {"Adresse", employee.getAddress()}
            }, boldFont, normalFont);
            document.add(mainTable);
            
            // Informations professionnelles
            addSection(document, "Informations Professionnelles", sectionFont);
            PdfPTable proTable = new PdfPTable(2);
            proTable.setWidthPercentage(100);
            proTable.setSpacingAfter(20);
            
            addInfoTable(proTable, new String[][] {
                {"Département", employee.getDepartment()},
                {"Poste", employee.getPosition()},
                {"Statut", employee.getStatus()},
                {"Type de contrat", employee.getContractType()},
                {"Date d'embauche", employee.getHireDate() != null ? 
                    employee.getHireDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-"}
            }, boldFont, normalFont);
            document.add(proTable);
            
            // Historique des salaires
            if (employee.getSalaries() != null && !employee.getSalaries().isEmpty()) {
                addSection(document, "Historique des Salaires", sectionFont);
                PdfPTable salaryTable = createSalaryTable(employee.getSalaries(), boldFont, normalFont);
                document.add(salaryTable);
            }
            
            // Pied de page
            addFooter(document, normalFont);
            
            document.close();
            writer.close();
            
            return baos.toByteArray();
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération du PDF: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (document != null && document.isOpen()) {
                document.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void addSection(Document document, String title, Font font) throws DocumentException {
        Paragraph section = new Paragraph(title, font);
        section.setSpacingBefore(15);
        section.setSpacingAfter(10);
        document.add(section);
    }
    
    private void addInfoTable(PdfPTable table, String[][] data, Font boldFont, Font normalFont) {
        for (String[] row : data) {
            PdfPCell labelCell = new PdfPCell(new Phrase(row[0] + ": ", boldFont));
            labelCell.setBorder(Rectangle.NO_BORDER);
            labelCell.setPadding(5);
            table.addCell(labelCell);
            
            PdfPCell valueCell = new PdfPCell(new Phrase(row[1] != null ? row[1] : "-", normalFont));
            valueCell.setBorder(Rectangle.NO_BORDER);
            valueCell.setPadding(5);
            table.addCell(valueCell);
        }
    }
    
    private PdfPTable createSalaryTable(List<Salary> salaries, Font boldFont, Font normalFont) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        
        // En-têtes
        String[] headers = {"Mois", "Date de paiement", "Montant brut", "Montant net", "Impôts"};
        Color headerColor = new Color(0, 51, 102);
        Font headerFont = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);
        
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(headerColor);
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
        
        // Données
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        for (Salary salary : salaries) {
            table.addCell(new Phrase(salary.getMonth(), normalFont));
            table.addCell(new Phrase(salary.getPaymentDate() != null ? 
                salary.getPaymentDate().format(dateFormat) : "-", normalFont));
            table.addCell(new Phrase(salary.getGrossAmount() != null ? 
                currencyFormat.format(salary.getGrossAmount()) : "-", normalFont));
            table.addCell(new Phrase(salary.getNetAmount() != null ? 
                currencyFormat.format(salary.getNetAmount()) : "-", normalFont));
            table.addCell(new Phrase(salary.getTaxAmount() != null ? 
                currencyFormat.format(salary.getTaxAmount()) : "-", normalFont));
        }
        
        return table;
    }
    
    private void addFooter(Document document, Font font) throws DocumentException {
        Paragraph footer = new Paragraph();
        footer.add(new Chunk("\n\nDocument généré le " + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), font));
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    public byte[] generateSalaryPayslipPdf(Map<String, Object> salaryData) {
        try {
            // Créer le document PDF avec une gestion explicite des ressources
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 54, 36); // Marges plus élégantes
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // Polices
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, new Color(0, 51, 102));
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(0, 51, 102));
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(0, 51, 102));
            Font headerWhiteFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11, new Color(51, 51, 51));
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(102, 102, 102));
            Font smallWhiteFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.WHITE);

            // En-tête avec bande colorée
            PdfPTable headerBand = new PdfPTable(1);
            headerBand.setWidthPercentage(100);
            PdfPCell bandCell = new PdfPCell(new Phrase(" "));
            bandCell.setBackgroundColor(new Color(0, 51, 102));
            bandCell.setFixedHeight(8f);
            bandCell.setBorder(Rectangle.NO_BORDER);
            headerBand.addCell(bandCell);
            document.add(headerBand);

            // Titre principal
            Paragraph title = new Paragraph();
            title.add(new Chunk("BULLETIN DE PAIE\n", titleFont));
            title.add(new Chunk(getString(salaryData, "month"), subTitleFont));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(20);
            title.setSpacingAfter(30);
            document.add(title);

            // Section informations employé
            PdfPTable mainTable = new PdfPTable(2);
            mainTable.setWidthPercentage(100);
            mainTable.setSpacingAfter(20);

            // Colonne gauche - Informations employé
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.BOX);
            leftCell.setBackgroundColor(new Color(240, 240, 240));
            leftCell.setPadding(10);

            Paragraph empInfo = new Paragraph();
            empInfo.add(new Chunk("INFORMATIONS EMPLOYÉ\n", headerFont));
            empInfo.add(new Chunk("\n"));
            empInfo.add(new Chunk("ID: " + getString(salaryData, "employeeId") + "\n", normalFont));
            empInfo.add(new Chunk("Nom: " + getString(salaryData, "employeeName") + "\n", normalFont));
            empInfo.add(new Chunk("Département: " + getString(salaryData, "department") + "\n", normalFont));
            leftCell.addElement(empInfo);
            mainTable.addCell(leftCell);

            // Colonne droite - Informations paie
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.BOX);
            rightCell.setBackgroundColor(new Color(240, 240, 240));
            rightCell.setPadding(10);

            Paragraph payInfo = new Paragraph();
            payInfo.add(new Chunk("INFORMATIONS PAIE\n", headerFont));
            payInfo.add(new Chunk("\n"));
            payInfo.add(new Chunk("Période: " + getString(salaryData, "month") + "\n", normalFont));
            if (salaryData.get("paymentDate") != null) {
                payInfo.add(new Chunk("Date de paiement: " + ((LocalDate)salaryData.get("paymentDate")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n", normalFont));
            }
            payInfo.add(new Chunk("Statut: " + getString(salaryData, "status") + "\n", normalFont));
            rightCell.addElement(payInfo);
            mainTable.addCell(rightCell);

            document.add(mainTable);

            // Tableau détaillé des montants
            PdfPTable detailTable = new PdfPTable(4);
            detailTable.setWidthPercentage(100);
            detailTable.setSpacingBefore(20);
            float[] columnWidths = {3f, 2f, 2f, 2f};
            detailTable.setWidths(columnWidths);

            // En-têtes du tableau détaillé
            String[] headers = {"Description", "Base", "Taux", "Montant"};
            for (String header : headers) {
                PdfPCell headerCell = new PdfPCell(new Phrase(header, headerWhiteFont));
                headerCell.setBackgroundColor(new Color(0, 51, 102));
                headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                headerCell.setPadding(8);
                detailTable.addCell(headerCell);
            }

            // Formatter pour les montants
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.FRANCE);

            // Salaire de base
            addDetailRow(detailTable, "Salaire de base", "100%", "-", 
                formatAmount(salaryData.get("grossAmount"), currencyFormatter), normalFont);

            // Déductions
            addDetailRow(detailTable, "Cotisations sociales", "-", "20%", 
                formatAmount(salaryData.get("taxAmount"), currencyFormatter), normalFont);

            // Ligne de total
            PdfPCell totalLabelCell = new PdfPCell(new Phrase("SALAIRE NET", headerFont));
            totalLabelCell.setColspan(3);
            totalLabelCell.setBackgroundColor(new Color(220, 220, 220));
            totalLabelCell.setPadding(8);
            totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            detailTable.addCell(totalLabelCell);

            PdfPCell totalAmountCell = new PdfPCell(new Phrase(
                formatAmount(salaryData.get("netAmount"), currencyFormatter), headerFont));
            totalAmountCell.setBackgroundColor(new Color(220, 220, 220));
            totalAmountCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            totalAmountCell.setPadding(8);
            detailTable.addCell(totalAmountCell);

            document.add(detailTable);

            // Note de bas de page
            document.add(new Paragraph("\n"));
            Paragraph note = new Paragraph("Note: Ce bulletin de paie est un document officiel à conserver sans limitation de durée.", smallFont);
            note.setSpacingBefore(20);
            document.add(note);

            // Pied de page avec bande colorée
            document.add(new Paragraph("\n"));
            PdfPTable footerBand = new PdfPTable(1);
            footerBand.setWidthPercentage(100);
            PdfPCell footerCell = new PdfPCell(new Phrase("Document généré le " + 
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), smallWhiteFont));
            footerCell.setBackgroundColor(new Color(0, 51, 102));
            footerCell.setPadding(8);
            footerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            footerBand.addCell(footerCell);
            document.add(footerBand);

            // Fermer le document
            document.close();
            writer.close();

            return baos.toByteArray();
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération du PDF: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void addDetailRow(PdfPTable table, String description, String base, String rate, String amount, Font font) {
        PdfPCell descCell = new PdfPCell(new Phrase(description, font));
        descCell.setPadding(5);
        descCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(descCell);

        PdfPCell baseCell = new PdfPCell(new Phrase(base, font));
        baseCell.setPadding(5);
        baseCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(baseCell);

        PdfPCell rateCell = new PdfPCell(new Phrase(rate, font));
        rateCell.setPadding(5);
        rateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(rateCell);

        PdfPCell amountCell = new PdfPCell(new Phrase(amount, font));
        amountCell.setPadding(5);
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(amountCell);
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "-";
    }

    private String formatAmount(Object amount, NumberFormat formatter) {
        if (amount == null) {
            return "-";
        }
        try {
            if (amount instanceof BigDecimal) {
                return formatter.format(amount);
            } else if (amount instanceof Number) {
                return formatter.format(((Number) amount).doubleValue());
            } else {
                return formatter.format(Double.parseDouble(amount.toString()));
            }
        } catch (Exception e) {
            return amount.toString();
        }
    }

    public Salary getSalaryById(String salaryId) {
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        String url = erpNextUrl + "/api/resource/Salary Slip/" + salaryId + 
            "?fields=[\"name\",\"employee\",\"posting_date\",\"start_date\",\"end_date\",\"gross_pay\",\"net_pay\",\"total_deduction\",\"status\"]";
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                
                Salary salary = new Salary();
                salary.setId((String) data.get("name"));
                salary.setEmployeeId((String) data.get("employee"));
                
                if (data.get("posting_date") != null) {
                    String dateStr = (String) data.get("posting_date");
                    salary.setPaymentDate(LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE));
                }
                
                if (data.get("start_date") != null) {
                    String startDateStr = (String) data.get("start_date");
                    LocalDate startDate = LocalDate.parse(startDateStr, DateTimeFormatter.ISO_DATE);
                    salary.setMonth(startDate.getYear() + "-" + String.format("%02d", startDate.getMonthValue()));
                }
                
                if (data.get("gross_pay") != null) {
                    salary.setGrossAmount(new BigDecimal(data.get("gross_pay").toString()));
                }
                
                if (data.get("net_pay") != null) {
                    salary.setNetAmount(new BigDecimal(data.get("net_pay").toString()));
                }
                
                if (data.get("total_deduction") != null) {
                    salary.setTaxAmount(new BigDecimal(data.get("total_deduction").toString()));
                }
                
                salary.setStatus((String) data.get("status"));
                return salary;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération du salaire ID: " + salaryId + ": " + e.getMessage());
            return null;
        }
        return null;
    }
}