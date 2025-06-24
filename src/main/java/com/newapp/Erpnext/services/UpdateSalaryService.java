package com.newapp.Erpnext.services;



import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.newapp.Erpnext.config.*;
import com.newapp.Erpnext.controller.HrmsExportController;
import com.newapp.Erpnext.models.Employee;
import com.newapp.Erpnext.models.Salary;
import com.newapp.Erpnext.models.SalaryComponent;
import com.newapp.Erpnext.models.SalaryStructureAssignment;

import jakarta.servlet.http.HttpSession;

@Service
public class UpdateSalaryService {

    

    
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    ObjectMapper objectMapper;

    @Value("${erpnext.base.url:http://erpnext.localhost:8000/api/resource/}")
    private String erpnextBaseUrl;

    @Value("${erpnext.base.url:http://erpnext.localhost:8000/api/}")
    private String Baseurl;


    private static final Logger logger = LoggerFactory.getLogger(UpdateSalaryService.class);
    
    private HttpHeaders createHeaders(String sid) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", "sid=" + sid);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    public List<SalaryComponent> getAllSalaryComponent(){
        HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
        String sid = sessionService.ensureImportSessionValid(session);


        
            String url = erpnextBaseUrl + "Salary Component";
            HttpHeaders headers = createHeaders(sid);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );

            List<SalaryComponent> salaryComponents = new ArrayList<>();

            if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");

            for (Map<String, Object> item : data) {
                SalaryComponent component= new SalaryComponent();
                component.setSalary_component((String) item.get("name"));
                salaryComponents.add(component);
            }
        }
    return salaryComponents;
    }

    public List<Salary> filterSalaries(String componentName, BigDecimal montant, int signe) {
    logger.info("Démarrage du filtrage des salaires : componentName={}, montant={}, signe={}", 
        componentName, montant, signe);

    // Validation des entrées
    if (componentName == null || componentName.trim().isEmpty()) {
        logger.error("Erreur : componentName est null ou vide");
        throw new IllegalArgumentException("Le nom du composant ne peut pas être null ou vide");
    }
    if (montant == null) {
        logger.error("Erreur : montant est null");
        throw new IllegalArgumentException("Le montant ne peut pas être null");
    }
    if (signe != 1 && signe != 2) {
        logger.error("Erreur : signe invalide ({})", signe);
        throw new IllegalArgumentException("Le signe doit être 1 (<) ou 2 (>)");
    }

    List<Employee> salaryEmployee = employeeService.getAllEmployees();
    List<Salary> salaryFilter = new ArrayList<>();

    if (salaryEmployee == null || salaryEmployee.isEmpty()) {
        logger.warn("Aucun employé trouvé par employeeService.getAllEmployees()");
        return salaryFilter;
    }

    for (Employee employee : salaryEmployee) {
        if (employee == null || employee.getId() == null) {
            logger.warn("Employé null ou ID d'employé null");
            continue;
        }

        logger.debug("Traitement de l'employé : {} (ID: {})", 
            employee.getName() != null ? employee.getName() : "inconnu", employee.getId());

        // Récupérer les salaires via employeeService.getEmployeeSalaries
        List<Salary> salaries = null;
        try {
            salaries = employeeService.getEmployeeSalaries(employee.getId());
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des salaires pour l'employé {} : {}", 
                employee.getId(), e.getMessage());
            continue;
        }

        if (salaries == null || salaries.isEmpty()) {
            logger.debug("Aucun salaire trouvé pour l'employé {}", employee.getId());
            continue;
        }

        for (Salary salary : salaries) {
            if (salary == null) {
                logger.warn("Salaire null pour l'employé {}", employee.getId());
                continue;
            }

            // Vérifier les earnings
            Map<String, BigDecimal> earnings = salary.getEarnings();
            if (earnings != null && earnings.containsKey(componentName)) {
                BigDecimal value = earnings.get(componentName);
                logger.debug("Composant {} trouvé dans earnings avec valeur {} pour l'employé {}", 
                    componentName, value, employee.getId());

                if ((signe == 1 && value.compareTo(montant) < 0) || 
                    (signe == 2 && value.compareTo(montant) > 0)) {
                    salaryFilter.add(salary);
                    logger.info("Salaire correspondant ajouté pour l'employé {} : component={}, valeur={}", 
                        employee.getId(), componentName, value);
                    continue;
                }
            }

            // Vérifier les deductions
            Map<String, BigDecimal> deductions = salary.getDeductions();
            if (deductions != null && deductions.containsKey(componentName)) {
                BigDecimal value = deductions.get(componentName);
                logger.debug("Composant {} trouvé dans deductions avec valeur {} pour l'employé {}", 
                    componentName, value, employee.getId());

                if ((signe == 1 && value.compareTo(montant) < 0) || 
                    (signe == 2 && value.compareTo(montant) > 0)) {
                    salaryFilter.add(salary);
                    logger.info("Salaire correspondant ajouté pour l'employé {} : component={}, valeur={}", 
                        employee.getId(), componentName, value);
                }
            }
        }
    }

    logger.info("Filtrage terminé : {} salaires trouvés", salaryFilter.size());
    return salaryFilter;
}

    // public List<Salary> filterSalaries (String componentName, BigDecimal montant, int signe){
        
    //     List<Employee> salaryEmployee = employeeService.getAllEmployees();
    //     List<Salary> salaryFilter = new ArrayList<>();
        
    //     for (Employee filterdata : salaryEmployee){
    //         boolean matched = false;           
    //         for (Salary data : filterdata.getSalaries()){
    //             Map<String,BigDecimal> earnings = data.getEarnings();
    //             Collection<BigDecimal> earningsvalue = earnings.values();
    //             if (earnings.containsKey(componentName)){
                    
    //                 if (signe == 1 && ((BigDecimal) earningsvalue).compareTo(montant) > 0 || signe == 2 && ((BigDecimal)earningsvalue).compareTo(montant) < 0){
    //                     salaryFilter.add(data);
    //                     matched = true;
    //                     break;
                        
    //                 }
    //                 System.out.println("Salary with component " + componentName + " and amount " + montant + " matched for employee: " + filterdata.getName()); 
    //             }
    //         }
    //         if (matched) continue;

    //         for (Salary datas : filterdata.getSalaries()){
    //             Map<String,BigDecimal> deductions = datas.getDeductions();
    //             Collection<BigDecimal> deductionvalue = deductions.values();
    //             if (deductions.containsKey(componentName)){
                    
    //                 if (signe == 1 && ((BigDecimal) deductionvalue).compareTo(montant) > 0 || signe == 2 && ((BigDecimal)deductionvalue).compareTo(montant) < 0){
    //                     salaryFilter.add(datas);
    //                     matched = true;
    //                     break;
    //                 } 
    //             }
    //         }
        
    //     }
    //     return salaryFilter;
    // }


    // public boolean cancel (String name) throws Exception{
    //     HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
    //     String sid = sessionService.ensureImportSessionValid(session);

    //     String url = Baseurl + "method/frappe/client.cancel";
    //         HttpHeaders headers = createHeaders(sid);

    //         ObjectNode param = objectMapper.createObjectNode();
    //         param.put("doctype","Salary Structure Assignment");
    //         param.put("name",name);

    //         HttpEntity<String> entity = new HttpEntity<>(headers);

    //         ResponseEntity<Map> response = restTemplate.exchange(
    //             url,
    //             HttpMethod.POST,
    //             entity,
    //             Map.class
    //         );
    //     if(response.getStatusCode().is2xxSuccessful()){
    //     return true;
    //     }
    //     else {
    //         throw new Exception("Tsy afaka annulena le sALARY STRUCTURE ASSIGNMENT : code" + response.getStatusCode());
    //     }
    // }


    public boolean cancel(String name) throws Exception {
    HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
            .getRequest().getSession();
    String sid = sessionService.ensureImportSessionValid(session);

    // URL corrigée
    String url = Baseurl + "method/frappe.client.cancel";
    
    HttpHeaders headers = createHeaders(sid);
    headers.setContentType(MediaType.APPLICATION_JSON);

    // Paramètres JSON
    ObjectNode params = objectMapper.createObjectNode();
    params.put("doctype", "Salary Structure Assignment");
    params.put("name", name);

    HttpEntity<String> entity = new HttpEntity<>(params.toString(), headers);

    try {
        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            return true;
        } else {
            throw new Exception("Échec de l'annulation. Code: " + response.getStatusCode());
        }
    } catch (HttpClientErrorException e) {
        throw new Exception("Erreur client: " + e.getResponseBodyAsString());
    } catch (HttpServerErrorException e) {
        throw new Exception("Erreur serveur: " + e.getResponseBodyAsString());
    }
}

        public boolean cancelSalary (String name) throws Exception{
        HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
        String sid = sessionService.ensureImportSessionValid(session);

        String url = Baseurl + "method/frappe.client.cancel";
            HttpHeaders headers = createHeaders(sid);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ObjectNode param = objectMapper.createObjectNode();
            param.put("doctype","Salary Slip");
            param.put("name",name);

            HttpEntity<String> entity = new HttpEntity<>(param.toString(),headers);

            try{
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
        if(response.getStatusCode().is2xxSuccessful()){
        return true;
        }
        else {
            throw new Exception("Tsy afaka annulena le sALARY slip : code" + response.getStatusCode());
        }
    }catch (HttpClientErrorException e) {
            throw new Exception("Erreur client: " + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            throw new Exception("Erreur serveur: " + e.getResponseBodyAsString());
        }
    }



    public boolean delete (String name) throws Exception{
        HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
        String sid = sessionService.ensureImportSessionValid(session);

        String url = erpnextBaseUrl + "Salary Structure Assignment/" + name;
            HttpHeaders headers = createHeaders(sid);

            

            HttpEntity<String> entity = new HttpEntity<>(headers);

        try{    
        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                entity,
                Map.class
            );
        if(response.getStatusCode().is2xxSuccessful()){
        return true;
        }
        else {
            throw new Exception("Tsy afaka fafany le sALARY STRUCTURE ASSIGNMENT : code" + response.getStatusCode());
        }
    }catch (HttpClientErrorException e) {
            throw new Exception("Erreur client: " + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            throw new Exception("Erreur serveur: " + e.getResponseBodyAsString());
        }
    }

        public boolean deleteSalary (String name) throws Exception{
        HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
        String sid = sessionService.ensureImportSessionValid(session);

        String url = erpnextBaseUrl + "Salary Slip/" + name;
            HttpHeaders headers = createHeaders(sid);

            

            HttpEntity<String> entity = new HttpEntity<>(headers);

        try{
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                entity,
                Map.class
            );
        if(response.getStatusCode().is2xxSuccessful()){
        return true;
        }
        else {
            throw new Exception("Tsy afaka fafany le sALARY Slip : code" + response.getStatusCode());
        }
    }catch (HttpClientErrorException e) {
            throw new Exception("Erreur client: " + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            throw new Exception("Erreur serveur: " + e.getResponseBodyAsString());
        }
    }

    public boolean remove(Salary salary, SalaryStructureAssignment salaryStructureAssignment) throws Exception {
    logger.info("Démarrage de la suppression pour Salary Slip ID: {} et Salary Structure Assignment: {}", 
        salary != null ? salary.getId() : "null", 
        salaryStructureAssignment != null ? salaryStructureAssignment.getName() : "null");

    // Validation des entrées
    if (salary == null || salary.getId() == null || salary.getId().trim().isEmpty()) {
        logger.error("Erreur : Salary ou son ID est null ou vide");
        throw new IllegalArgumentException("Salary et son ID sont requis");
    }
    if (salaryStructureAssignment == null || salaryStructureAssignment.getName() == null || salaryStructureAssignment.getName().trim().isEmpty()) {
        logger.error("Erreur : SalaryStructureAssignment ou son nom est null ou vide");
        throw new IllegalArgumentException("SalaryStructureAssignment et son nom sont requis");
    }

    HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
    String sid = sessionService.ensureImportSessionValid(session);
    logger.debug("Session validée avec sid : {}", sid);

    try {
        // Annulation du Salary Structure Assignment
        logger.debug("Tentative d'annulation du Salary Structure Assignment : {}", salaryStructureAssignment.getName());
        boolean cancelledSass = cancel(salaryStructureAssignment.getName());
        if (!cancelledSass) {
            logger.error("Échec de l'annulation du Salary Structure Assignment : {}", salaryStructureAssignment.getName());
            throw new Exception("Impossible d'annuler le Salary Structure Assignment : " + salaryStructureAssignment.getName());
        }
        logger.info("Salary Structure Assignment annulé avec succès : {}", salaryStructureAssignment.getName());

        // Suppression du Salary Structure Assignment
        logger.debug("Tentative de suppression du Salary Structure Assignment : {}", salaryStructureAssignment.getName());
        boolean deletedSass = delete(salaryStructureAssignment.getName());
        if (!deletedSass) {
            logger.error("Échec de la suppression du Salary Structure Assignment : {}", salaryStructureAssignment.getName());
            throw new Exception("Impossible de supprimer le Salary Structure Assignment : " + salaryStructureAssignment.getName());
        }
        logger.info("Salary Structure Assignment supprimé avec succès : {}", salaryStructureAssignment.getName());

        // Annulation du Salary Slip
        logger.debug("Tentative d'annulation du Salary Slip : {}", salary.getId());
        boolean cancelSalary = cancelSalary(salary.getId());
        if (!cancelSalary) {
            logger.error("Échec de l'annulation du Salary Slip : {}", salary.getId());
            throw new Exception("Impossible d'annuler le Salary Slip : " + salary.getId());
        }
        logger.info("Salary Slip annulé avec succès : {}", salary.getId());

        // Suppression du Salary Slip
        logger.debug("Tentative de suppression du Salary Slip : {}", salary.getId());
        boolean deletedSalary = deleteSalary(salary.getId());
        if (!deletedSalary) {
            logger.error("Échec de la suppression du Salary Slip : {}", salary.getId());
            throw new Exception("Impossible de supprimer le Salary Slip : " + salary.getId());
        }
        logger.info("Salary Slip supprimé avec succès : {}", salary.getId());

        logger.info("Salary Structure Assignment et Salary Slip supprimés avec succès pour l'employé : {}", 
            salaryStructureAssignment.getEmployee() != null ? salaryStructureAssignment.getEmployee() : "inconnu");
        return true;
    } catch (Exception e) {
        logger.error("Erreur critique lors de la suppression pour Salary Slip ID: {} et Salary Structure Assignment: {} : {}", 
            salary.getId(), salaryStructureAssignment.getName(), e.getMessage(), e);
        throw e;
    }
}

    // public boolean remove(Salary name, SalaryStructureAssignment salaryStructureAssignment) throws Exception{
    //     HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
    //     String sid = sessionService.ensureImportSessionValid(session);
    //     boolean cancelledSass= false;
    //     boolean deletedSass = false;
    //     boolean cancelSalary= false ;
    //     boolean deletedSalary = false;

        
    //     cancelledSass = cancel(salaryStructureAssignment.getName());
        
    //     if (cancelledSass!=true)
    //         throw new Exception("Tsy mbola annulé le Salary Structure Assignment : " + salaryStructureAssignment.getName());
        
    //     deletedSass = delete(salaryStructureAssignment.getName());
    //     if (deletedSass!=true)
    //         throw new Exception("Tsy mbola voafafa le Salary Structure Assignment : " + salaryStructureAssignment.getName());
        
    //     cancelSalary = cancelSalary(name.getId());
    //     if (cancelSalary!=true)
    //         throw new Exception("Tsy mbola annulé le Salary Slip : " + name.getId());
        
    //     deletedSalary = deleteSalary(name.getId());
    //     if (deletedSalary!=true)
    //         throw new Exception("Tsy mbola voafafa le Salary Slip : " + name.getId());

    //     System.out.println("Salary Structure Assignment and Salary Slip successfully removed for employee: " + salaryStructureAssignment.getEmployee());
        
    //     return true;
    // }



public SalaryStructureAssignment get(Salary salary) throws Exception {
    logger.info("Démarrage de la récupération de Salary Structure Assignment pour l'employé : {}", 
        salary != null ? salary.getEmployeeId() : "null");

    // Validation des entrées
    if (salary == null || salary.getEmployeeId() == null || salary.getEmployeeId().trim().isEmpty()) {
        logger.error("Erreur : Salary ou employeeId est null ou vide");
        throw new IllegalArgumentException("Salary et employeeId sont requis");
    }

    HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
    String sid = sessionService.ensureImportSessionValid(session);
    logger.debug("Session validée avec sid : {}", sid);

    String url = erpnextBaseUrl + "Salary Structure Assignment?filters=[[\"employee\",\"=\",\"" + salary.getEmployeeId() + "\"]]&fields=[\"name\",\"base\",\"employee\",\"from_date\"]";
    logger.debug("Envoi de la requête GET à : {}", url);

    HttpHeaders headers = createHeaders(sid);
    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            Map.class
        );
        logger.info("Réponse API reçue : code HTTP = {}", response.getStatusCode());

        if (response.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> responseBody = response.getBody();
            logger.debug("Corps de la réponse : {}", responseBody);

            if (responseBody == null || !responseBody.containsKey("data")) {
                logger.error("Réponse invalide : 'data' est absent ou null");
                throw new RuntimeException("Aucun Salary Structure Assignment trouvé");
            }

            List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
            if (data == null || data.isEmpty()) {
                logger.error("Aucune donnée de Salary Structure Assignment pour l'employé : {}", salary.getEmployeeId());
                throw new RuntimeException("Aucun Salary Structure Assignment trouvé");
            }

            Map<String, Object> assignmentData = data.get(0); // Prendre le premier assignment
            logger.debug("Données du premier assignment : {}", assignmentData);

            SalaryStructureAssignment sass = new SalaryStructureAssignment();
            sass.setName((String) assignmentData.get("name"));
            sass.setEmployee((String) assignmentData.get("employee"));
            sass.setBase(assignmentData.get("base") != null ? ((Number) assignmentData.get("base")).doubleValue() : 0.0);
            sass.setSalary_structure((String) assignmentData.get("salary_structure"));

            String fromDateStr = (String) assignmentData.get("from_date");
            if (fromDateStr != null) {
                try {
                    sass.setFrom_date(LocalDate.parse(fromDateStr, DateTimeFormatter.ISO_LOCAL_DATE));
                    logger.debug("Date from_date parsée : {}", sass.getFrom_date());
                } catch (DateTimeParseException e) {
                    logger.error("Erreur lors du parsing de from_date : {}", fromDateStr, e);
                    throw new RuntimeException("Format de date invalide pour from_date : " + fromDateStr);
                }
            } else {
                logger.warn("from_date est null pour l'employé : {}", salary.getEmployeeId());
            }

            logger.info("Salary Structure Assignment récupéré avec succès pour l'employé : {}", sass.getEmployee());
            return sass;
        } else {
            logger.error("Échec de la requête API : code HTTP = {}", response.getStatusCode());
            throw new Exception("Erreur lors de la récupération du Salary Structure Assignment : code " + response.getStatusCode());
        }
    } catch (Exception e) {
        logger.error("Erreur critique lors de la récupération du Salary Structure Assignment pour l'employé {} : {}", 
            salary.getEmployeeId(), e.getMessage(), e);
        throw e;
    }
}


    // public SalaryStructureAssignment get(Salary salary) throws Exception{

    //     HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
    //     String sid = sessionService.ensureImportSessionValid(session);

    //     String url = erpnextBaseUrl + "Salary Structure Assignment?filters=[[\"employee\",\"=\",\""+ salary.getEmployeeId()+"\"]]&fields=[\"name\",\"base\",\"employee\",\"from_date\"]";
    //     HttpHeaders headers = createHeaders(sid);

    //     HttpEntity<String> entity = new HttpEntity<>(headers);
        
    //     ResponseEntity<Map> response = restTemplate.exchange(
    //             url,
    //             HttpMethod.GET,
    //             entity,
    //             Map.class
    //         );
        
    //     if (response.getStatusCode().is2xxSuccessful()){
    //         Map<String, Object> responseBody = response.getBody();
    //         Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
    //         if (data == null || data.isEmpty() || data.size()==0){
    //             throw new RuntimeException("Aucun salary structure Assignment trouvé");
    //         }

    //         Map<String, Object> assignmentData = (Map<String, Object>) data.get(0); // Take first assignment
    //     SalaryStructureAssignment sass = new SalaryStructureAssignment();
    //     sass.setName((String) assignmentData.get("name"));
    //     sass.setBase(((Number) assignmentData.get("base")).doubleValue());
    //     sass.setEmployee((String) assignmentData.get("employee"));
    //     sass.setFrom_date((LocalDate) assignmentData.get("from_date"));

    //         return sass;
    //     }
    //     else {
    //         throw new Exception("Tsy poins le get SASS");
    //     }        
    // }


//     public SalaryStructureAssignment createSalaryStructureAssignment(SalaryStructureAssignment data) throws Exception {
//     HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
//     String sid = sessionService.ensureImportSessionValid(session);

//     String url = erpnextBaseUrl + "Salary Structure Assignment";
//     HttpHeaders headers = createHeaders(sid);
    
//     // Convertir les données en JSON
//     Map<String, Object> ssa = new HashMap<>();
//         ssa.put("docstatus", 1);
//         ssa.put("employee",data.getEmployee());
//         ssa.put("salary_structure",data.getSalary_structure());
//         ssa.put("from_date",data.getFrom_date().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE));
//         ssa.put("base",data.getBase());
//         ssa.put("company",data.getCompany());


//     HttpEntity<Map<String, Object>> entity = new HttpEntity<>(ssa, headers);

//     // 
//         try {
//         ResponseEntity<Map> response = restTemplate.exchange(
//             url,
//             HttpMethod.POST,
//             entity,
//             Map.class
//         );
        
//         if(response.getStatusCode().is2xxSuccessful()){
//             Map<String, Object> responseBody = response.getBody();
//             if (responseBody != null && responseBody.containsKey("data")) {
//                 Map<String, Object> dataResponse = (Map<String, Object>) responseBody.get("data");
//                 SalaryStructureAssignment createdAssignment = new SalaryStructureAssignment();
//                 createdAssignment.setName((String) dataResponse.get("name"));
//                 createdAssignment.setEmployee((String) dataResponse.get("employee"));
//                 createdAssignment.setSalary_structure((String) dataResponse.get("salary_structure"));
//                 createdAssignment.setFrom_date(LocalDate.parse((String) dataResponse.get("from_date")));
//                 createdAssignment.setBase(((Number) dataResponse.get("base")).doubleValue());
//                 createdAssignment.setCompany((String) dataResponse.get("company"));
//                 return createdAssignment;
//             } else {
//                 throw new Exception("La réponse ne contient pas de données valides pour le Salary Structure Assignment créé.");
//             }
//         } else {
//             throw new Exception("Échec de la création du Salary Structure Assignment. Code HTTP: " + 
//                               response.getStatusCode() + ", Réponse: " + response.getBody());
//         }
//     } catch (RestClientException e) {
//         throw new Exception("Erreur lors de la communication avec l'API ERPNext: " + e.getMessage(), e);
//     } catch (Exception e) {
//         throw new Exception("Erreur inattendue lors de la création du Salary Structure Assignment: " + e.getMessage(), e);
//     }
// }

public SalaryStructureAssignment createSalaryStructureAssignment(SalaryStructureAssignment data) throws Exception {
    logger.info("Début création Salary Structure Assignment pour employé: {}", data.getEmployee());
    
    if (data.getSalary_structure() == null || data.getSalary_structure().isEmpty()) {
        logger.error("La structure de salaire est obligatoire pour l'employé {}", data.getEmployee());
        throw new IllegalArgumentException("La structure de salaire ne peut pas être vide");
    }

    if (data.getCompany() == null || data.getCompany().isEmpty()) {
        logger.error("La société est obligatoire pour l'employé {}", data.getEmployee());
        throw new IllegalArgumentException("La société ne peut pas être vide");
    }

    HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
    String sid = sessionService.ensureImportSessionValid(session);

    String url = erpnextBaseUrl + "Salary Structure Assignment";
    logger.debug("URL API: {}", url);
    
    HttpHeaders headers = createHeaders(sid);
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("docstatus", 1);
    requestBody.put("employee", data.getEmployee());
    requestBody.put("salary_structure", data.getSalary_structure());
    requestBody.put("from_date", data.getFrom_date().format(DateTimeFormatter.ISO_LOCAL_DATE));
    requestBody.put("base", data.getBase());
    requestBody.put("company", data.getCompany());

    logger.debug("Request Body: {}", requestBody);
    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

    try {
        logger.info("Envoi requête à l'API ERPNext...");
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        
        if(response.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> responseData = (Map<String, Object>) response.getBody().get("data");
            logger.debug("Réponse API: {}", responseData);
            
            SalaryStructureAssignment result = new SalaryStructureAssignment();
            result.setName((String) responseData.get("name"));
            result.setEmployee((String) responseData.get("employee"));
            result.setSalary_structure((String) responseData.get("salary_structure"));
            result.setFrom_date(LocalDate.parse((String) responseData.get("from_date")));
            result.setBase(((Number) responseData.get("base")).doubleValue());
            result.setCompany((String) responseData.get("company"));
            
            logger.info("Création réussie - SSA ID: {} | Base: {} | Structure: {}", 
                      result.getName(), result.getBase(), result.getSalary_structure());
            return result;
        } else {
            logger.error("Échec création SSA - Code HTTP: {} | Réponse: {}", 
                       response.getStatusCode(), response.getBody());
            throw new Exception("Erreur API: " + response.getBody());
        }
    } catch (RestClientException e) {
        logger.error("Erreur technique lors de la création SSA - Employé: {} | Détails: {}", 
                   data.getEmployee(), e.getMessage(), e);
        throw new Exception("Erreur de communication API: " + e.getMessage(), e);
    } catch (Exception e) {
        logger.error("Erreur inattendue - Employé: {} | Cause: {}", 
                   data.getEmployee(), e.getMessage(), e);
        throw e;
    }
}

public Salary createSalarySlip(SalaryStructureAssignment data) throws Exception {
    logger.info("Début création Salary Slip pour employé: {}", data.getEmployee());
    
    HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
    String sid = sessionService.ensureImportSessionValid(session);

    // Check for existing Salary Slip
    String checkUrl = erpnextBaseUrl + "Salary Slip?filters=[[\"employee\",\"=\",\"" + data.getEmployee() + 
                      "\"],[\"start_date\",\"=\",\"" + data.getFrom_date().format(DateTimeFormatter.ISO_LOCAL_DATE) + 
                      "\"],[\"docstatus\",\"!=\",2]]&fields=[\"name\"]";
    logger.debug("Vérification existence Salary Slip: {}", checkUrl);

    HttpHeaders headers = createHeaders(sid);
    HttpEntity<String> checkEntity = new HttpEntity<>(headers);

    try {
        ResponseEntity<Map> checkResponse = restTemplate.exchange(checkUrl, HttpMethod.GET, checkEntity, Map.class);
        if (checkResponse.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> responseBody = checkResponse.getBody();
            List<Map<String, Object>> existingSlips = (List<Map<String, Object>>) responseBody.get("data");
            if (existingSlips != null && !existingSlips.isEmpty()) {
                logger.warn("Salary Slip déjà existant pour employé {} pour la période {}", 
                           data.getEmployee(), data.getFrom_date());
                throw new Exception("Un Salary Slip existe déjà pour l'employé " + data.getEmployee() + 
                                    " pour la période " + data.getFrom_date());
            }
        }
    } catch (RestClientException e) {
        logger.error("Erreur lors de la vérification de l'existence du Salary Slip: {}", e.getMessage());
        throw new Exception("Erreur lors de la vérification du Salary Slip: " + e.getMessage(), e);
    }

    // Proceed with Salary Slip creation
    String url = erpnextBaseUrl + "Salary Slip";
    logger.debug("URL API: {}", url);
    
    headers.setContentType(MediaType.APPLICATION_JSON);

    YearMonth yearMonth = YearMonth.from(data.getFrom_date());
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();
    logger.debug("Période calculée: {} à {}", startDate, endDate);

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("docstatus", 1);
    requestBody.put("employee", data.getEmployee());
    requestBody.put("salary_structure", data.getSalary_structure());
    requestBody.put("start_date", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
    requestBody.put("end_date", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
    requestBody.put("company", data.getCompany());
    requestBody.put("payroll_frequency", data.getPayrollFrequency());
    requestBody.put("posting_date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

    logger.debug("Request Body: {}", requestBody);
    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

    try {
        logger.info("Envoi requête création bulletin...");
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        
        if (response.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> responseData = (Map<String, Object>) response.getBody().get("data");
            logger.debug("Réponse API: {}", responseData);
            
            Salary result = new Salary();
            result.setId((String) responseData.get("name"));
            result.setEmployeeId((String) responseData.get("employee"));
            result.setStartDate(LocalDate.parse((String) responseData.get("start_date")));
            result.setEndDate(LocalDate.parse((String) responseData.get("end_date")));
            result.setCompany((String) responseData.get("company"));
            result.setPayrollFrequency((String) responseData.get("payroll_frequency"));
            result.setPostingDate(LocalDate.parse((String) responseData.get("posting_date")));
            
            logger.info("Bulletin créé - ID: {} | Période: {}->{} | Fréquence: {}", 
                       result.getId(), result.getStartDate(), result.getEndDate(), 
                       result.getPayrollFrequency());
            return result;
        } else {
            logger.error("Échec création bulletin - Code: {} | Réponse: {}", 
                        response.getStatusCode(), response.getBody());
            throw new Exception("Erreur API: " + response.getBody());
        }
    } catch (RestClientException e) {
        logger.error("Erreur technique création bulletin - Employé: {} | Erreur: {}", 
                   data.getEmployee(), e.getMessage(), e);
        throw new Exception("Erreur communication API: " + e.getMessage(), e);
    } catch (Exception e) {
        logger.error("Erreur inattendue - Employé: {} | Cause: {}", 
                   data.getEmployee(), e.getMessage(), e);
        throw e;
    }
}

// public Salary createSalarySlip(SalaryStructureAssignment data) throws Exception {
//     HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
//     String sid = sessionService.ensureImportSessionValid(session);

//     String url = erpnextBaseUrl + "Salary Slip";
//     HttpHeaders headers = createHeaders(sid);
    
   
//     LocalDate start = LocalDate.of(data.getFrom_date().getYear(), data.getFrom_date().getMonthValue(),1);
//         LocalDate end = start.withDayOfMonth(data.getFrom_date().lengthOfMonth());
//         Map<String, Object> salarySlip = new HashMap<>();
//         salarySlip.put("docstatus", 1);
//         salarySlip.put("employee", data.getEmployee());
//         salarySlip.put("start_date",start.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE));
//         salarySlip.put("end_date",end.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE));
//         salarySlip.put("payroll_frequency", "Monthly");
//         salarySlip.put("company", data.getCompany());
//         salarySlip.put("posting_date",start.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE));



//     HttpEntity<Map<String, Object>> entity = new HttpEntity<>(salarySlip, headers);

//     try {
//     ResponseEntity<Map> response = restTemplate.exchange(
//         url,
//         HttpMethod.POST,
//         entity,
//         Map.class
//     );
    
//     if(response.getStatusCode().is2xxSuccessful()){
//         Map<String,Object> responseBody = response.getBody();
//         if (responseBody != null && responseBody.containsKey("data")){
//             Map<String, Object> dataResponse = (Map<String, Object>) responseBody.get("data");
//             Salary createdSalary = new Salary();
//             createdSalary.setId((String) dataResponse.get("name"));
//             createdSalary.setEmployeeId((String) dataResponse.get("employee"));
//             createdSalary.setStartDate(LocalDate.parse((String) dataResponse.get("start_date")));
//             createdSalary.setEndDate(LocalDate.parse((String) dataResponse.get("end_date")));
//             createdSalary.setCompany((String) dataResponse.get("company"));
//             return createdSalary;
//         } else {
//             throw new Exception("La réponse ne contient pas de données valides pour le Salary Slip créé.");
//         }
//     }
//     else {
//         throw new Exception("Impossible de créer le Salary Slip : code " + response.getStatusCode());
//     }
//     } catch (Exception e) {
//         throw new Exception("Erreur lors de la création du Salary Slip: " + e.getMessage(), e);
//     }
// }
    

}






    




