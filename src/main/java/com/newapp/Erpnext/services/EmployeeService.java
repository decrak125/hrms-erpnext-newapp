package com.newapp.Erpnext.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newapp.Erpnext.models.Employee;
import com.newapp.Erpnext.models.Salary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.util.Locale;
import java.awt.Color;

@Service
public class EmployeeService {

    @Value("${erpnext.url}")
    private String erpNextUrl;
    
    private final RestTemplate restTemplate;
    private final ErpNextAuthService authService;
    
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
    
    /**
     * Récupère les salaires d'un employé
     */
    public List<Salary> getEmployeeSalaries(String employeeId) {
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        String url = erpNextUrl + "/api/resource/Salary Slip?fields=[\"name\",\"posting_date\",\"start_date\",\"end_date\",\"gross_pay\",\"net_pay\",\"total_deduction\",\"status\"]&filters=[[\"employee\",\"=\",\"" + employeeId + "\"]]";        
        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            Map.class
        );
        
        List<Salary> salaries = new ArrayList<>();
        
        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
            
            for (Map<String, Object> item : data) {
                Salary salary = new Salary();
                salary.setId((String) item.get("name"));
                salary.setEmployeeId(employeeId);
                
                // Conversion de la date de paiement
                if (item.get("posting_date") != null) {
                    String dateStr = (String) item.get("posting_date");
                    salary.setPaymentDate(LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE));
                }
                
                // Déterminer le mois à partir des dates de début et de fin
                if (item.get("start_date") != null) {
                    String startDateStr = (String) item.get("start_date");
                    LocalDate startDate = LocalDate.parse(startDateStr, DateTimeFormatter.ISO_DATE);
                    salary.setMonth(startDate.getYear() + "-" + String.format("%02d", startDate.getMonthValue()));
                }
                
                // Conversion des montants
                if (item.get("gross_pay") != null) {
                    salary.setGrossAmount(new BigDecimal(item.get("gross_pay").toString()));
                }
                
                if (item.get("net_pay") != null) {
                    salary.setNetAmount(new BigDecimal(item.get("net_pay").toString()));
                }
                
                if (item.get("total_deduction") != null) {
                    salary.setTaxAmount(new BigDecimal(item.get("total_deduction").toString()));
                }
                
                salary.setStatus((String) item.get("status"));
                
                salaries.add(salary);
            }
        }
        
        return salaries;
    }
    
    /**
     * Génère un PDF avec les informations de l'employé
     */
    public byte[] generateEmployeePdf(String employeeId) {
        try {
            // Récupérer les données de l'employé
            Employee employee = getEmployeeById(employeeId);
            if (employee == null) {
                return null;
            }
            
            // Créer un document PDF
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Ajouter le titre
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Paragraph title = new Paragraph("FICHE EMPLOYÉ", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Informations principales de l'employé
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            
            // Photo de l'employé (si disponible)
            // Note: Cette partie est commentée car nous n'avons pas accès aux photos
            // Si vous avez des photos, vous pouvez décommenter et adapter ce code
            /*
            if (employee.getPhotoUrl() != null) {
                try {
                    Image photo = Image.getInstance(new URL(employee.getPhotoUrl()));
                    photo.scaleToFit(100, 100);
                    PdfPCell photoCell = new PdfPCell(photo);
                    photoCell.setBorder(Rectangle.NO_BORDER);
                    photoCell.setRowspan(4);
                    infoTable.addCell(photoCell);
                } catch (Exception e) {
                    // En cas d'erreur, ajouter une cellule vide
                    PdfPCell emptyCell = new PdfPCell();
                    emptyCell.setBorder(Rectangle.NO_BORDER);
                    emptyCell.setRowspan(4);
                    infoTable.addCell(emptyCell);
                }
            } else {
                PdfPCell emptyCell = new PdfPCell();
                emptyCell.setBorder(Rectangle.NO_BORDER);
                emptyCell.setRowspan(4);
                infoTable.addCell(emptyCell);
            }
            */
            
            // ID et Nom
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);
            
            PdfPCell idCell = new PdfPCell();
            idCell.setBorder(Rectangle.NO_BORDER);
            Paragraph idPara = new Paragraph();
            idPara.add(new Chunk("ID: ", boldFont));
            idPara.add(new Chunk(employee.getId(), normalFont));
            idCell.addElement(idPara);
            infoTable.addCell(idCell);
            
            PdfPCell nameCell = new PdfPCell();
            nameCell.setBorder(Rectangle.NO_BORDER);
            Paragraph namePara = new Paragraph();
            namePara.add(new Chunk("Nom: ", boldFont));
            namePara.add(new Chunk(employee.getName(), normalFont));
            nameCell.addElement(namePara);
            infoTable.addCell(nameCell);
            
            // Département et Poste
            PdfPCell deptCell = new PdfPCell();
            deptCell.setBorder(Rectangle.NO_BORDER);
            Paragraph deptPara = new Paragraph();
            deptPara.add(new Chunk("Département: ", boldFont));
            deptPara.add(new Chunk(employee.getDepartment() != null ? employee.getDepartment() : "-", normalFont));
            deptCell.addElement(deptPara);
            infoTable.addCell(deptCell);
            
            PdfPCell posCell = new PdfPCell();
            posCell.setBorder(Rectangle.NO_BORDER);
            Paragraph posPara = new Paragraph();
            posPara.add(new Chunk("Poste: ", boldFont));
            posPara.add(new Chunk(employee.getPosition() != null ? employee.getPosition() : "-", normalFont));
            posCell.addElement(posPara);
            infoTable.addCell(posCell);
            
            document.add(infoTable);
            document.add(new Paragraph(" "));
            
            // Informations de contact
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
            Paragraph contactTitle = new Paragraph("Informations de contact", sectionFont);
            contactTitle.setSpacingAfter(10);
            document.add(contactTitle);
            
            PdfPTable contactTable = new PdfPTable(2);
            contactTable.setWidthPercentage(100);
            
            // Email
            PdfPCell emailCell = new PdfPCell();
            emailCell.setBorder(Rectangle.NO_BORDER);
            Paragraph emailPara = new Paragraph();
            emailPara.add(new Chunk("Email: ", boldFont));
            emailPara.add(new Chunk(employee.getEmail() != null ? employee.getEmail() : "-", normalFont));
            emailCell.addElement(emailPara);
            contactTable.addCell(emailCell);
            
            // Téléphone
            PdfPCell phoneCell = new PdfPCell();
            phoneCell.setBorder(Rectangle.NO_BORDER);
            Paragraph phonePara = new Paragraph();
            phonePara.add(new Chunk("Téléphone: ", boldFont));
            phonePara.add(new Chunk(employee.getPhone() != null ? employee.getPhone() : "-", normalFont));
            phoneCell.addElement(phonePara);
            contactTable.addCell(phoneCell);
            
            // Adresse
            PdfPCell addressCell = new PdfPCell();
            addressCell.setBorder(Rectangle.NO_BORDER);
            addressCell.setColspan(2);
            Paragraph addressPara = new Paragraph();
            addressPara.add(new Chunk("Adresse: ", boldFont));
            addressPara.add(new Chunk(employee.getAddress() != null ? employee.getAddress() : "-", normalFont));
            addressCell.addElement(addressPara);
            contactTable.addCell(addressCell);
            
            document.add(contactTable);
            document.add(new Paragraph(" "));
            
            // Informations professionnelles
            Paragraph proTitle = new Paragraph("Informations professionnelles", sectionFont);
            proTitle.setSpacingAfter(10);
            document.add(proTitle);
            
            PdfPTable proTable = new PdfPTable(2);
            proTable.setWidthPercentage(100);
            
            // Statut
            PdfPCell statusCell = new PdfPCell();
            statusCell.setBorder(Rectangle.NO_BORDER);
            Paragraph statusPara = new Paragraph();
            statusPara.add(new Chunk("Statut: ", boldFont));
            statusPara.add(new Chunk(employee.getStatus() != null ? employee.getStatus() : "-", normalFont));
            statusCell.addElement(statusPara);
            proTable.addCell(statusCell);
            
            // Type de contrat
            PdfPCell contractCell = new PdfPCell();
            contractCell.setBorder(Rectangle.NO_BORDER);
            Paragraph contractPara = new Paragraph();
            contractPara.add(new Chunk("Type de contrat: ", boldFont));
            contractPara.add(new Chunk(employee.getContractType() != null ? employee.getContractType() : "-", normalFont));
            contractCell.addElement(contractPara);
            proTable.addCell(contractCell);
            
            // Date d'embauche
            PdfPCell hireDateCell = new PdfPCell();
            hireDateCell.setBorder(Rectangle.NO_BORDER);
            hireDateCell.setColspan(2);
            Paragraph hireDatePara = new Paragraph();
            hireDatePara.add(new Chunk("Date d'embauche: ", boldFont));
            if (employee.getHireDate() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                hireDatePara.add(new Chunk(employee.getHireDate().format(formatter), normalFont));
            } else {
                hireDatePara.add(new Chunk("-", normalFont));
            }
            hireDateCell.addElement(hireDatePara);
            proTable.addCell(hireDateCell);
            
            document.add(proTable);
            document.add(new Paragraph(" "));
            
            // Historique des salaires
            if (employee.getSalaries() != null && !employee.getSalaries().isEmpty()) {
                Paragraph salaryTitle = new Paragraph("Historique des salaires", sectionFont);
                salaryTitle.setSpacingAfter(10);
                document.add(salaryTitle);
                
                PdfPTable salaryTable = new PdfPTable(5);
                salaryTable.setWidthPercentage(100);
                
                // En-têtes du tableau
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
                
                PdfPCell headerCell1 = new PdfPCell(new Phrase("Mois", headerFont));
                headerCell1.setBackgroundColor(Color.DARK_GRAY);
                headerCell1.setPadding(5);
                salaryTable.addCell(headerCell1);
                
                PdfPCell headerCell2 = new PdfPCell(new Phrase("Date de paiement", headerFont));
                headerCell2.setBackgroundColor(Color.DARK_GRAY);
                headerCell2.setPadding(5);
                salaryTable.addCell(headerCell2);
                
                PdfPCell headerCell3 = new PdfPCell(new Phrase("Montant brut", headerFont));
                headerCell3.setBackgroundColor(Color.DARK_GRAY);
                headerCell3.setPadding(5);
                salaryTable.addCell(headerCell3);
                
                PdfPCell headerCell4 = new PdfPCell(new Phrase("Montant net", headerFont));
                headerCell4.setBackgroundColor(Color.DARK_GRAY);
                headerCell4.setPadding(5);
                salaryTable.addCell(headerCell4);
                
                PdfPCell headerCell5 = new PdfPCell(new Phrase("Taxes", headerFont));
                headerCell5.setBackgroundColor(Color.DARK_GRAY);
                headerCell5.setPadding(5);
                salaryTable.addCell(headerCell5);
                
                // Formatter pour les montants
                NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.FRANCE);
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                
                // Données du tableau
                for (Salary salary : employee.getSalaries()) {
                    salaryTable.addCell(new Phrase(salary.getMonth() != null ? salary.getMonth() : "-"));
                    salaryTable.addCell(new Phrase(salary.getPaymentDate() != null ? salary.getPaymentDate().format(dateFormatter) : "-"));
                    salaryTable.addCell(new Phrase(salary.getGrossAmount() != null ? currencyFormatter.format(salary.getGrossAmount()) : "-"));
                    salaryTable.addCell(new Phrase(salary.getNetAmount() != null ? currencyFormatter.format(salary.getNetAmount()) : "-"));
                    salaryTable.addCell(new Phrase(salary.getTaxAmount() != null ? currencyFormatter.format(salary.getTaxAmount()) : "-"));
                }
                
                document.add(salaryTable);
            }
            
            // Pied de page
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("Document généré le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
            
            document.close();
            
            return baos.toByteArray();
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération du PDF: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}