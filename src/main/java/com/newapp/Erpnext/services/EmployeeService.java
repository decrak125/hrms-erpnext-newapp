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
        // Implémentation de la génération de PDF
        // Cette méthode pourrait utiliser une bibliothèque comme iText ou PDFBox
        // pour générer un PDF avec les informations de l'employé
        
        // Pour l'instant, retournons un tableau de bytes vide
        return new byte[0];
    }
}