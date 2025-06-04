package com.newapp.Erpnext.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newapp.Erpnext.models.Employee;
import com.newapp.Erpnext.models.SalaryComponent;
import com.newapp.Erpnext.models.SalaryStructure;
import com.newapp.Erpnext.models.SalaryStructureAssignment;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ImportService {
    
    private final String base_url = "http://erpnext.localhost:8000/api";
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;

    public boolean import_data(MultipartFile employee_file, MultipartFile structure_file, MultipartFile salary_file, HttpSession session) throws Exception {
        List<Employee> employees = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(employee_file.getInputStream()));
        String line = reader.readLine(); // sauter l'en-tête
        
        while ((line = reader.readLine()) != null) {
            Employee employee = new Employee();
            String[] columns = line.split(",");
            employee.setRef(Integer.parseInt(columns[0]));
            employee.setFirstName(columns[1]);
            employee.setLastName(columns[2]);
            employee.setMiddleName(columns[3]);
            employee.setGender(columns[4]);
            
            // Parse dates avec gestion d'erreur
            if (columns.length > 5 && !columns[5].isEmpty()) {
                employee.setHireDate(LocalDate.parse(columns[5], DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
            
            employee.setCompany(columns[6]);
            employee.setDepartment(columns[7]);
            employee.setPosition(columns[8]);
            employee.setEmail(columns[9]);
            employee.setPhone(columns[10]);
            employee.setAddress(columns[11]);
            employee.setStatus(columns[12]);
            employee.setContractType(columns[13]);
            
            // Générer le nom complet
            String fullName = employee.getFirstName();
            if (employee.getMiddleName() != null && !employee.getMiddleName().isEmpty()) {
                fullName += " " + employee.getMiddleName();
            }
            fullName += " " + employee.getLastName();
            employee.setName(fullName);

            employees.add(employee);
        }
        reader.close();

        Map<String, SalaryStructure> structureMap = new HashMap<>();

        reader = new BufferedReader(new InputStreamReader(structure_file.getInputStream()));
        line = reader.readLine(); // sauter l'en-tête

        while ((line = reader.readLine()) != null) {
            String[] columns = line.split(",");

            String structureName = columns[0];
            String company = columns[5];

            SalaryStructure salaryStructure = structureMap.getOrDefault(structureName, new SalaryStructure());
            if (!structureMap.containsKey(structureName)) {
                salaryStructure.setName(structureName);
                salaryStructure.setCompany(company);
                salaryStructure.setSalaryComponents(new ArrayList<>());
                structureMap.put(structureName, salaryStructure);
            }

            SalaryComponent sc = new SalaryComponent();
            sc.setSalary_component(columns[1]);
            sc.setSalary_component_abbr(columns[2]);
            sc.setType(columns[3]);
            sc.setFormula(columns[4]);

            salaryStructure.getSalaryComponents().add(sc);
        }
        reader.close();
        
        List<SalaryStructure> salaryStructures = new ArrayList<>(structureMap.values());

        List<SalaryStructureAssignment> salaryStructureAssignments = new ArrayList<>();

        reader = new BufferedReader(new InputStreamReader(salary_file.getInputStream()));
        line = reader.readLine(); // sauter l'en-tête

        while ((line = reader.readLine()) != null) {
            String[] columns = line.split(",");
            SalaryStructureAssignment salaryStructureAssignment = new SalaryStructureAssignment();
            salaryStructureAssignment.setFrom_date(LocalDate.parse(columns[0], DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            salaryStructureAssignment.setEmployee_ref(Integer.parseInt(columns[1]));
            salaryStructureAssignment.setBase(Double.parseDouble(columns[2]));
            salaryStructureAssignment.setSalary_structure(columns[3]);

            salaryStructureAssignments.add(salaryStructureAssignment);
        }
        reader.close();

        String url = base_url + "/method/erpnext.importation.rh_import.import_data";

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("employees_json", objectMapper.writeValueAsString(employees));
        map.add("structure_json", objectMapper.writeValueAsString(salaryStructures));
        map.add("salary_json", objectMapper.writeValueAsString(salaryStructureAssignments));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", "sid=" + session.getAttribute("sid"));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
        );
        
        return response.getStatusCode().is2xxSuccessful();
    }

    public boolean import_employees(MultipartFile employee_file, HttpSession session) throws Exception {
        List<Employee> employees = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(employee_file.getInputStream()));
        String line = reader.readLine(); // sauter l'en-tête
        
        while ((line = reader.readLine()) != null) {
            Employee employee = new Employee();
            String[] columns = line.split(",");
            employee.setRef(Integer.parseInt(columns[0]));
            employee.setFirstName(columns[1]);
            employee.setLastName(columns[2]);
            employee.setMiddleName(columns[3]);
            employee.setGender(columns[4]);
            
            if (columns.length > 5 && !columns[5].isEmpty()) {
                employee.setHireDate(LocalDate.parse(columns[5], DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
            
            employee.setCompany(columns[6]);
            employee.setDepartment(columns[7]);
            employee.setPosition(columns[8]);
            employee.setEmail(columns[9]);
            employee.setPhone(columns[10]);
            employee.setAddress(columns[11]);
            employee.setStatus(columns[12]);
            employee.setContractType(columns[13]);
            
            // Générer le nom complet
            String fullName = employee.getFirstName();
            if (employee.getMiddleName() != null && !employee.getMiddleName().isEmpty()) {
                fullName += " " + employee.getMiddleName();
            }
            fullName += " " + employee.getLastName();
            employee.setName(fullName);

            employees.add(employee);
        }
        reader.close();

        String url = base_url + "/method/erpnext.database_manipulation.controllers.rh_import.import_employees";

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("employees_json", objectMapper.writeValueAsString(employees));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", "sid=" + session.getAttribute("sid"));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
        );
        
        return response.getStatusCode().is2xxSuccessful();
    }

    public boolean import_salary_structures(MultipartFile structure_file, HttpSession session) throws Exception {
        Map<String, SalaryStructure> structureMap = new HashMap<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(structure_file.getInputStream()));
        String line = reader.readLine(); // sauter l'en-tête

        while ((line = reader.readLine()) != null) {
            String[] columns = line.split(",");

            String structureName = columns[0];
            String company = columns[5];

            SalaryStructure salaryStructure = structureMap.getOrDefault(structureName, new SalaryStructure());
            if (!structureMap.containsKey(structureName)) {
                salaryStructure.setName(structureName);
                salaryStructure.setCompany(company);
                salaryStructure.setSalaryComponents(new ArrayList<>());
                structureMap.put(structureName, salaryStructure);
            }

            SalaryComponent sc = new SalaryComponent();
            sc.setSalary_component(columns[1]);
            sc.setSalary_component_abbr(columns[2]);
            sc.setType(columns[3]);
            sc.setFormula(columns[4]);

            salaryStructure.getSalaryComponents().add(sc);
        }
        reader.close();

        List<SalaryStructure> salaryStructures = new ArrayList<>(structureMap.values());

        String url = base_url + "/method/erpnext.database_manipulation.controllers.rh_import.import_salary_structures";

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("structure_json", objectMapper.writeValueAsString(salaryStructures));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", "sid=" + session.getAttribute("sid"));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
        );
        
        return response.getStatusCode().is2xxSuccessful();
    }

    public boolean import_salary_assignments(MultipartFile salary_file, HttpSession session) throws Exception {
        List<SalaryStructureAssignment> salaryStructureAssignments = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(salary_file.getInputStream()));
        String line = reader.readLine(); // sauter l'en-tête

        while ((line = reader.readLine()) != null) {
            String[] columns = line.split(",");
            SalaryStructureAssignment salaryStructureAssignment = new SalaryStructureAssignment();
            salaryStructureAssignment.setFrom_date(LocalDate.parse(columns[0], DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            salaryStructureAssignment.setEmployee_ref(Integer.parseInt(columns[1]));
            salaryStructureAssignment.setBase(Double.parseDouble(columns[2]));
            salaryStructureAssignment.setSalary_structure(columns[3]);

            salaryStructureAssignments.add(salaryStructureAssignment);
        }
        reader.close();

        String url = base_url + "/method/erpnext.database_manipulation.controllers.rh_import.import_salary_assignments";

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("salary_json", objectMapper.writeValueAsString(salaryStructureAssignments));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", "sid=" + session.getAttribute("sid"));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
        );
        
        return response.getStatusCode().is2xxSuccessful();
    }
}