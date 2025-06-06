package com.newapp.Erpnext.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
public class ImportService {

    private static final Logger logger = LoggerFactory.getLogger(ImportService.class);

    @Value("${java.io.tmpdir}")
    private String tempDir;

    @Value("${erpnext.url}")
    private String erpnextUrl;

    @Value("${erpnext.api.key}")
    private String apiKey;

    @Value("${erpnext.api.secret}")
    private String apiSecret;

    private final RestTemplate restTemplate;

<<<<<<< Updated upstream
    public ImportService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String storeFile(MultipartFile file, String prefix) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
=======
        try {
            // Parsing et validation des employés
            List<EmployeeImport> employees = parseEmployees(employee_file);
            logger.info("Parsed {} employees", employees.size());

            // Créer une map des employés par référence pour un accès rapide
            Map<Integer, EmployeeImport> employeeMap = new HashMap<>();
            for (EmployeeImport emp : employees) {
                employeeMap.put(emp.getRef(), emp);
            }
            importCache.put("employee_map", employeeMap);

            // Stocker les références d'employés pour la validation
            Set<Integer> employeeRefs = new HashSet<>(employeeMap.keySet());
            importCache.put("employee_refs", employeeRefs);

            // Parsing et validation des structures salariales
            Map<String, SalaryStructure> structureMap = parseStructures(structure_file);
            List<SalaryStructure> structures = new ArrayList<>(structureMap.values());
            logger.info("Parsed {} salary structures", structures.size());

            // Parsing et validation des assignations
            List<SalaryStructureAssignment> assignments = parseAssignments(salary_file, 
                employeeRefs,
                structureMap.keySet());
            logger.info("Parsed {} salary assignments", assignments.size());

            // Préparation de la requête
            String url = baseUrl + "/method/erpnext.importation.rh_import.import_data";
            MultiValueMap<String, String> requestData = new LinkedMultiValueMap<>();
            requestData.add("employees_json", objectMapper.writeValueAsString(employees));
            requestData.add("structure_json", objectMapper.writeValueAsString(structures));
            requestData.add("salary_json", objectMapper.writeValueAsString(assignments));

            // Configuration des headers
            HttpHeaders headers = createHeaders(session);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestData, headers);

            // Appel API
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
        );
        
        if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("Import failed with status: {}", response.getStatusCode());
                throw new ImportException("L'importation a échoué avec le code : " + response.getStatusCode());
            }

            String responseBody = response.getBody();
            if (responseBody == null) {
                logger.error("Empty response from ERPNext");
                throw new ImportException("Réponse vide de ERPNext");
            }

            // Parse la réponse JSON
            Map<String, Object> jsonResponse = objectMapper.readValue(responseBody, Map.class);
            
            // Vérifie si la réponse contient une erreur
            if (jsonResponse.containsKey("exc_type") || jsonResponse.containsKey("exception") || 
                jsonResponse.containsKey("_error_message") || jsonResponse.containsKey("error")) {
                String errorMessage = jsonResponse.containsKey("exception") ? 
                    (String) jsonResponse.get("exception") : 
                    jsonResponse.containsKey("_error_message") ? 
                        (String) jsonResponse.get("_error_message") : 
                        "Erreur inconnue lors de l'importation";
                logger.error("ERPNext error: {}", errorMessage);
                throw new ImportException("Erreur ERPNext : " + errorMessage);
            }

            // Vérifie si l'importation a réussi
            if (jsonResponse.containsKey("message") && "OK".equals(jsonResponse.get("message"))) {
                logger.info("Import successful");
                return true;
            } else {
                logger.error("Unexpected response from ERPNext: {}", responseBody);
                throw new ImportException("Réponse inattendue de ERPNext: " + responseBody);
            }

        } catch (Exception e) {
            logger.error("Import failed", e);
            throw new ImportException("Erreur lors de l'importation: " + e.getMessage(), e);
        } finally {
            clearImportCache();
        }
    }

    private List<EmployeeImport> parseEmployees(MultipartFile file) throws Exception {
        List<EmployeeImport> employees = new ArrayList<>();
        Set<Integer> refs = new HashSet<>();
        importCache.put("employee_refs", refs);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            String line = reader.readLine(); // Skip header
            int lineNumber = 1;

        while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (employees.size() >= MAX_RECORDS) {
                    throw new ImportException("Nombre maximum d'employés dépassé (" + MAX_RECORDS + ")");
                }

            String[] columns = line.split(",");
                validateEmployeeColumns(columns, lineNumber);

                EmployeeImport employee = new EmployeeImport();
                try {
                    int ref = Integer.parseInt(columns[0].trim());
                    if (!refs.add(ref)) {
                        throw new ImportException("Référence employé en doublon: " + ref);
                    }
                    employee.setRef(ref);
                    employee.setLastName(columns[1].trim());
                    employee.setFirstName(columns[2].trim());
                    employee.setGender(columns[3].trim());
                    employee.setHireDate(LocalDate.parse(columns[4].trim(), dateFormatter));
                    employee.setDateOfBirth(LocalDate.parse(columns[5].trim(), dateFormatter));
                    employee.setCompany(columns[6].trim());
                    employee.setName(employee.getFirstName() + " " + employee.getLastName());

                    employees.add(employee);
                } catch (Exception e) {
                    throw new ImportException("Erreur ligne " + lineNumber + ": " + e.getMessage());
                }
            }
>>>>>>> Stashed changes
        }
        String uniqueFilename = prefix + "_" + UUID.randomUUID().toString() + fileExtension;
        
        Path uploadDir = Paths.get(tempDir, "erpnext-imports");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        
        Path targetPath = uploadDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        return targetPath.toString();
    }

    public void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            logger.warn("Erreur lors de la suppression du fichier: " + filePath, e);
        }
    }

    public Map<String, Object> validateFiles(String employeeFilePath, 
            String salaryStructureFilePath, 
            String salaryAssignmentFilePath) {
        
        Map<String, Object> validationResults = new HashMap<>();
        
        try {
            // Validation basique des fichiers CSV
            validateCsvFile(employeeFilePath, "employee", validationResults);
            validateCsvFile(salaryStructureFilePath, "structure", validationResults);
            validateCsvFile(salaryAssignmentFilePath, "assignment", validationResults);
            
<<<<<<< Updated upstream
            // Si des erreurs sont trouvées, on arrête là
            if (!validationResults.isEmpty()) {
                return validationResults;
=======
            // Configuration des headers
            HttpHeaders headers = createHeaders(session);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(new LinkedMultiValueMap<>(), headers);

            // Appel API
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
        );
        
            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("Reset RH failed with status: {}", response.getStatusCode());
                throw new ImportException("La réinitialisation a échoué avec le code : " + response.getStatusCode());
>>>>>>> Stashed changes
            }
            
            // Validation des références entre les fichiers
            validateReferences(employeeFilePath, salaryStructureFilePath, 
                            salaryAssignmentFilePath, validationResults);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la validation des fichiers", e);
            validationResults.put("message", "Erreur lors de la validation: " + e.getMessage());
        }
        
        return validationResults;
    }

    private void validateCsvFile(String filePath, String fileType, Map<String, Object> results) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headers = reader.readNext();
            if (headers == null) {
                results.put(fileType + "_errors", Arrays.asList("Le fichier est vide"));
                return;
            }
            
            // Validation des en-têtes selon le type de fichier
            List<String> missingHeaders = validateHeaders(headers, fileType);
            if (!missingHeaders.isEmpty()) {
                results.put(fileType + "_errors", 
                    Arrays.asList("Colonnes manquantes: " + String.join(", ", missingHeaders)));
            }
            
        } catch (Exception e) {
            results.put(fileType + "_errors", 
                Arrays.asList("Erreur lors de la lecture du fichier: " + e.getMessage()));
        }
    }

    private List<String> validateHeaders(String[] headers, String fileType) {
        List<String> requiredHeaders = getRequiredHeaders(fileType);
        List<String> missingHeaders = new ArrayList<>();
        
        for (String required : requiredHeaders) {
            boolean found = false;
            for (String header : headers) {
                if (header.trim().equalsIgnoreCase(required)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                missingHeaders.add(required);
            }
        }
        
        return missingHeaders;
    }

    private List<String> getRequiredHeaders(String fileType) {
        switch (fileType) {
            case "employee":
                return Arrays.asList("Ref", "Nom", "Prenom", "Genre", "Date_Naissance", "Date_Embauche", "company");
            case "structure":
                return Arrays.asList("salary structure", "name", "Abbr", "type", "valeur", "company");
            case "assignment":
                return Arrays.asList("Mois", "Ref Employe", "Salaire Base", "Salaire", "company");
            default:
                return new ArrayList<>();
        }
    }

    private void validateReferences(String employeeFilePath, String structureFilePath, 
            String assignmentFilePath, Map<String, Object> results) {
        try {
            // Collecter les références des employés
            Set<String> employeeRefs = new HashSet<>();
            try (CSVReader reader = new CSVReader(new FileReader(employeeFilePath))) {
                String[] headers = reader.readNext();
                int refIndex = findColumnIndex(headers, "Ref");
                String[] line;
                while ((line = reader.readNext()) != null) {
                    if (line.length > refIndex) {
                        employeeRefs.add(line[refIndex].trim());
                    }
                }
            }
            
            // Vérifier les références dans le fichier d'attribution
            List<String> referenceErrors = new ArrayList<>();
            try (CSVReader reader = new CSVReader(new FileReader(assignmentFilePath))) {
                String[] headers = reader.readNext();
                int refIndex = findColumnIndex(headers, "Ref Employe");
                String[] line;
                while ((line = reader.readNext()) != null) {
                    if (line.length > refIndex) {
                        String ref = line[refIndex].trim();
                        if (!employeeRefs.contains(ref)) {
                            referenceErrors.add("Référence employé non trouvée: " + ref);
                        }
                    }
                }
            }
            
            if (!referenceErrors.isEmpty()) {
                results.put("reference_errors", referenceErrors);
            }
            
        } catch (Exception e) {
            logger.error("Erreur lors de la validation des références", e);
            results.put("reference_errors", Arrays.asList("Erreur lors de la validation des références: " + e.getMessage()));
        }
    }

    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1;
    }

    public Map<String, Object> importToErpnext(String employeeFilePath, 
            String structureFilePath, 
            String assignmentFilePath) {
        Map<String, Object> result = new HashMap<>();
        List<String> messages = new ArrayList<>();
        
        try {
            // Configuration des en-têtes pour l'API ERPNext
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "token " + apiKey + ":" + apiSecret);
            
            // Import des employés
            importEmployees(employeeFilePath, headers, messages);
            
            // Import des structures salariales
            importSalaryStructures(structureFilePath, headers, messages);
            
            // Import des attributions de salaire
            importSalaryAssignments(assignmentFilePath, headers, messages);
            
            result.put("success", true);
            result.put("messages", messages);
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'import", e);
            result.put("success", false);
            result.put("message", "Erreur lors de l'import: " + e.getMessage());
        }
        
        return result;
    }

    private void importEmployees(String filePath, HttpHeaders headers, List<String> messages) 
            throws IOException, CsvValidationException {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] csvHeaders = reader.readNext();
            String[] line;
            int count = 0;
            
            while ((line = reader.readNext()) != null) {
                Map<String, Object> employee = transformEmployeeData(csvHeaders, line);
                
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(employee, headers);
                ResponseEntity<Map> response = restTemplate.postForEntity(
                    erpnextUrl + "/api/resource/Employee",
                    request,
                    Map.class
                );
                
                if (response.getStatusCode() == HttpStatus.OK) {
                    count++;
                }
            }
            
            messages.add(count + " employés importés avec succès");
        }
    }

    private Map<String, Object> transformEmployeeData(String[] headers, String[] data) {
        Map<String, Object> employee = new HashMap<>();
        Map<String, Integer> columnMap = createColumnMap(headers);
        
        employee.put("doctype", "Employee");
        employee.put("employee_number", getValue(data, columnMap, "Ref"));
        employee.put("first_name", getValue(data, columnMap, "Prenom"));
        employee.put("last_name", getValue(data, columnMap, "Nom"));
        employee.put("gender", transformGender(getValue(data, columnMap, "Genre")));
        employee.put("date_of_birth", getValue(data, columnMap, "Date_Naissance"));
        employee.put("date_of_joining", getValue(data, columnMap, "Date_Embauche"));
        employee.put("company", getValue(data, columnMap, "company"));
        
        return employee;
    }

    private Map<String, Integer> createColumnMap(String[] headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            map.put(headers[i].trim(), i);
        }
        return map;
    }

    private String getValue(String[] data, Map<String, Integer> columnMap, String columnName) {
        Integer index = columnMap.get(columnName);
        if (index != null && index < data.length) {
            return data[index].trim();
        }
        return "";
    }

    private String transformGender(String gender) {
        if ("Masculin".equalsIgnoreCase(gender) || "Male".equalsIgnoreCase(gender)) {
            return "Male";
        }
        if ("Feminin".equalsIgnoreCase(gender) || "Female".equalsIgnoreCase(gender)) {
            return "Female";
        }
        return gender;
    }

    private void importSalaryStructures(String filePath, HttpHeaders headers, List<String> messages) 
            throws IOException, CsvValidationException {
        // TODO: Implémenter l'import des structures salariales
        messages.add("Import des structures salariales non implémenté");
    }

    private void importSalaryAssignments(String filePath, HttpHeaders headers, List<String> messages) 
            throws IOException, CsvValidationException {
        // TODO: Implémenter l'import des attributions de salaire
        messages.add("Import des attributions de salaire non implémenté");
    }
} 