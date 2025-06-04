package com.newapp.Erpnext.services;

<<<<<<< Updated upstream
<<<<<<< Updated upstream
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
=======
=======
>>>>>>> Stashed changes
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newapp.Erpnext.models.Employee;
import com.newapp.Erpnext.models.SalaryComponent;
import com.newapp.Erpnext.models.SalaryStructure;
import com.newapp.Erpnext.models.SalaryStructureAssignment;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
<<<<<<< Updated upstream
>>>>>>> Stashed changes
=======
>>>>>>> Stashed changes
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

<<<<<<< Updated upstream
<<<<<<< Updated upstream
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
=======
=======
>>>>>>> Stashed changes
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
<<<<<<< Updated upstream
>>>>>>> Stashed changes
=======
>>>>>>> Stashed changes
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ImportService {
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    ObjectMapper objectMapper;

<<<<<<< Updated upstream
<<<<<<< Updated upstream
    @Value("${java.io.tmpdir}")
    private String tempDir;

    @Value("${erpnext.url}")
    private String erpnextUrl;

    @Value("${erpnext.api.key}")
    private String apiKey;

    @Value("${erpnext.api.secret}")
    private String apiSecret;

    private final RestTemplate restTemplate;

    public ImportService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private final Map<String, List<String>> columnMappings = new HashMap<String, List<String>>() {{
        put("Ref", Arrays.asList("ref", "id", "employee_id", "emp_id", "numero", "reference", "Ref"));
        put("Nom", Arrays.asList("nom", "lastname", "family_name", "surname", "Nom"));
        put("Prenom", Arrays.asList("prenom", "firstname", "given_name", "Prenom"));
        put("Genre", Arrays.asList("genre", "gender", "sexe", "Genre"));
        put("Date_Naissance", Arrays.asList("date_naissance", "birth_date", "date_of_birth", "Date_Naissance"));
        put("Date_Embauche", Arrays.asList("date_embauche", "hire_date", "date_of_joining", "Date_Embauche"));
        put("company", Arrays.asList("company", "entreprise", "societe", "Company"));
        put("Employee_Ref", Arrays.asList("employee_ref", "emp_ref", "ref_employe", "Employee_Ref"));
        put("Structure_Ref", Arrays.asList("structure_ref", "salary_structure", "Structure_Ref"));
        put("Date_Debut", Arrays.asList("date_debut", "start_date", "from_date", "Date_Debut"));
        put("Date_Fin", Arrays.asList("date_fin", "end_date", "to_date", "Date_Fin"));
        put("Designation", Arrays.asList("designation", "name", "component_name", "Designation"));
        put("Base", Arrays.asList("base", "salary_component", "Base"));
        put("Type", Arrays.asList("type", "component_type", "Type"));
        put("Montant", Arrays.asList("montant", "amount", "formula", "Montant"));
        put("Mois", Arrays.asList("mois", "month", "posting_date", "Mois"));
        put("Salaire Base", Arrays.asList("salaire_base", "base_salary", "Salaire Base"));
        put("Salaire", Arrays.asList("salaire", "salary", "Salaire"));
    }};

    public String storeFile(MultipartFile file, String prefix) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
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

    public Resource getFileResource(String filePath) {
        return new FileSystemResource(filePath);
    }

    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }

    public Map<String, Object> parseAndPreviewCSV(String filePath) throws IOException {
        Map<String, Object> result = new HashMap<>();
        List<String[]> preview = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headers = reader.readNext();
            if (headers != null) {
                result.put("headers", headers);
                preview.add(headers);
                
                for (int i = 0; i < 5; i++) {
                    String[] line = reader.readNext();
                    if (line != null) {
                        preview.add(line);
                    } else {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException("Erreur lors du parsing du fichier CSV: " + e.getMessage(), e);
=======
    String base_url = "http://erpnext.localhost:8000/api";

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
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            return false;
>>>>>>> Stashed changes
        }
        return true;
    }

    // Méthode pour importer seulement les employés
    public boolean import_employees(MultipartFile employee_file, HttpSession session) throws Exception {
        List<Employee> employees = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(employee_file.getInputStream()));
        String line = reader.readLine(); // sauter l'en-tête
        
<<<<<<< Updated upstream
        result.put("preview", preview);
        return result;
    }

    public Map<String, Object> analyzeColumns(String[] headers, String fileType) {
        Map<String, Object> analysis = new HashMap<>();
        Map<String, String> mappingSuggestions = new HashMap<>();
        List<String> missingColumns = new ArrayList<>();
        
        List<String> requiredColumns = getRequiredColumns(fileType);
        
        for (String required : requiredColumns) {
            String foundColumn = findBestMatch(required, headers);
            if (foundColumn != null) {
                mappingSuggestions.put(required, foundColumn);
            } else {
                missingColumns.add(required);
            }
=======
    String base_url = "http://erpnext.localhost:8000/api";

    public boolean import_data(MultipartFile employee_file, MultipartFile structure_file, MultipartFile salary_file, HttpSession session) throws Exception {
        List<Employee> employees = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(employee_file.getInputStream()));
        String line = reader.readLine(); // sauter l'en-tête
        
=======
>>>>>>> Stashed changes
        while ((line = reader.readLine()) != null) {
            Employee employee = new Employee();
            String[] columns = line.split(",");
            employee.setRef(Integer.parseInt(columns[0]));
            employee.setFirstName(columns[1]);
            employee.setLastName(columns[2]);
            employee.setMiddleName(columns[3]);
            employee.setGender(columns[4]);
            
<<<<<<< Updated upstream
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
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            return false;
>>>>>>> Stashed changes
        }
        return true;
    }

    // Méthode pour importer seulement les employés
    public boolean import_employees(MultipartFile employee_file, HttpSession session) throws Exception {
        List<Employee> employees = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(employee_file.getInputStream()));
        String line = reader.readLine(); // sauter l'en-tête
        
<<<<<<< Updated upstream
        analysis.put("mappingSuggestions", mappingSuggestions);
        analysis.put("missingColumns", missingColumns);
        analysis.put("availableColumns", Arrays.asList(headers));
        
        return analysis;
    }

    private List<String> getRequiredColumns(String fileType) {
        switch (fileType) {
            case "employee":
                return Arrays.asList("Ref", "Nom", "Prenom", "Genre", "Date_Naissance", "Date_Embauche", "company");
            case "salary_structure":
                return Arrays.asList("salary structure", "name", "Abbr", "type", "valeur", "company");
            case "salary_assignment":
                return Arrays.asList("Mois", "Ref Employe", "Salaire Base", "Salaire", "company");
            default:
                return new ArrayList<>();
        }
    }

    private String findBestMatch(String requiredColumn, String[] headers) {
        List<String> possibleMatches = columnMappings.get(requiredColumn);
        if (possibleMatches == null) return null;
        
        for (String header : headers) {
            for (String possible : possibleMatches) {
                if (normalizeColumnName(header).equals(normalizeColumnName(possible))) {
                    return header;
                }
            }
        }
        return null;
    }

    private String normalizeColumnName(String columnName) {
        return columnName.toLowerCase()
                .replaceAll("\\s+", "_")
                .replaceAll("[àáâãäå]", "a")
                .replaceAll("[èéêë]", "e")
                .replaceAll("[ìíîï]", "i")
                .replaceAll("[òóôõö]", "o")
                .replaceAll("[ùúûü]", "u")
                .replaceAll("[ç]", "c");
    }

    public Map<String, Object> generateMissingColumnSolutions(List<String> missingColumns, String fileType) {
        Map<String, Object> solutions = new HashMap<>();
        
        for (String missing : missingColumns) {
            Map<String, Object> solution = new HashMap<>();
            
            switch (missing) {
                case "Ref":
                    solution.put("type", "auto_generate");
                    solution.put("pattern", "EMP{0000}");
                    solution.put("description", "Génération automatique: EMP0001, EMP0002, ...");
                    break;
                case "company":
                    solution.put("type", "default_value");
                    solution.put("defaultValue", "My Company");
                    solution.put("description", "Utiliser 'My Company' par défaut");
                    break;
                case "Ref Employe":
                    solution.put("type", "reference_mapping");
                    solution.put("description", "Utiliser la colonne Ref du fichier employé");
                    break;
                case "Mois":
                    solution.put("type", "current_date");
                    solution.put("description", "Utiliser la date actuelle au format YYYY-MM-DD");
                    break;
                default:
                    solution.put("type", "manual_input");
                    solution.put("description", "Saisie manuelle requise");
                    break;
            }
            
            solutions.put(missing, solution);
        }
        
        return solutions;
    }

    public Map<String, List<String>> validateEmployeeFile(String filePath) throws CsvValidationException {
        Map<String, List<String>> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> employeeRefs = new HashSet<>();
        
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headers = reader.readNext();
            if (headers == null) {
                errors.add("Fichier vide ou en-têtes manquants");
                validation.put("errors", errors);
                validation.put("warnings", warnings);
                return validation;
            }
            
            validateEmployeeHeaders(headers, errors);
            if (!errors.isEmpty()) {
                validation.put("errors", errors);
                validation.put("warnings", warnings);
                return validation;
            }
            
            validateEmployeeData(reader, headers, errors, warnings, employeeRefs);
            
            // Vérifier les doublons dans HRMS
            for (String ref : employeeRefs) {
                ResponseEntity<Map> response = restTemplate.getForEntity(
                    erpnextUrl + "/api/resource/Employee?filters=[['employee_number','=','" + ref + "']]",
                    Map.class
                );
                if (response.getStatusCode() == HttpStatus.OK && !((List) response.getBody().get("data")).isEmpty()) {
                    errors.add("Employé avec Ref " + ref + " existe déjà dans HRMS");
                }
            }
        } catch (IOException e) {
            errors.add("Erreur lors de la lecture du fichier: " + e.getMessage());
        }
        
        validation.put("errors", errors);
        validation.put("warnings", warnings);
        return validation;
    }

    private void validateEmployeeHeaders(String[] headers, List<String> errors) {
        List<String> requiredColumns = Arrays.asList("Ref", "Nom", "Prenom", "Genre", "Date_Naissance", "Date_Embauche", "company");
        List<String> headerList = Arrays.asList(headers);
        
        for (String required : requiredColumns) {
            if (!headerList.contains(required)) {
                String match = findBestMatch(required, headers);
                if (match == null) {
                    errors.add("Colonne obligatoire manquante: " + required);
                }
            }
        }
    }

    private void validateEmployeeData(CSVReader reader, String[] headers, List<String> errors, 
                                    List<String> warnings, Set<String> employeeRefs) throws IOException, CsvValidationException {
        String[] line;
        int rowNum = 1;
        Map<String, Integer> columnIndexes = createColumnIndexMap(headers);
        
        while ((line = reader.readNext()) != null) {
            rowNum++;
            if (line.length < headers.length) {
                errors.add(String.format("Ligne %d: Données incomplètes", rowNum));
                continue;
            }
            
            validateEmployeeRef(getValue(line, columnIndexes, "Ref"), rowNum, employeeRefs, errors);
            validateNameField(getValue(line, columnIndexes, "Nom"), "Nom", rowNum, errors);
            validateNameField(getValue(line, columnIndexes, "Prenom"), "Prenom", rowNum, errors);
            validateGender(getValue(line, columnIndexes, "Genre"), rowNum, errors);
            validateBirthDate(getValue(line, columnIndexes, "Date_Naissance"), rowNum, errors);
            validateHireDate(getValue(line, columnIndexes, "Date_Embauche"), rowNum, errors);
            validateCompany(getValue(line, columnIndexes, "company"), rowNum, errors);
        }
    }

    private Map<String, Integer> createColumnIndexMap(String[] headers) {
        Map<String, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            indexMap.put(headers[i], i);
        }
        return indexMap;
    }

    private String getValue(String[] line, Map<String, Integer> columnIndexes, String columnName) {
        Integer index = columnIndexes.get(columnName);
        if (index != null && index < line.length) {
            return line[index].trim();
        }
        return "";
    }

    public Map<String, List<String>> validateSalaryStructureFile(String filePath) throws CsvValidationException {
        Map<String, List<String>> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> structureRefs = new HashSet<>();
        Set<String> componentAbbrs = new HashSet<>();
        
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headers = reader.readNext();
            if (headers == null) {
                errors.add("Fichier vide ou en-têtes manquants");
                validation.put("errors", errors);
                validation.put("warnings", warnings);
                return validation;
            }
            
            validateSalaryStructureHeaders(headers, errors);
            if (!errors.isEmpty()) {
                validation.put("errors", errors);
                validation.put("warnings", warnings);
                return validation;
            }
            
            validateSalaryStructureData(reader, headers, errors, warnings, structureRefs, componentAbbrs);
            
            // Vérifier les doublons dans HRMS
            for (String ref : structureRefs) {
                ResponseEntity<Map> response = restTemplate.getForEntity(
                    erpnextUrl + "/api/resource/Salary Structure?filters=[['name','=','" + ref + "']]",
                    Map.class
                );
                if (response.getStatusCode() == HttpStatus.OK && !((List) response.getBody().get("data")).isEmpty()) {
                    errors.add("Structure de salaire avec Ref " + ref + " existe déjà dans HRMS");
                }
            }
            for (String abbr : componentAbbrs) {
                ResponseEntity<Map> response = restTemplate.getForEntity(
                    erpnextUrl + "/api/resource/Salary Component?filters=[['abbr','=','" + abbr + "']]",
                    Map.class
                );
                if (response.getStatusCode() == HttpStatus.OK && !((List) response.getBody().get("data")).isEmpty()) {
                    errors.add("Composant de salaire avec Abbr " + abbr + " existe déjà dans HRMS");
                }
            }
        } catch (IOException e) {
            errors.add("Erreur lors de la lecture du fichier: " + e.getMessage());
        }
        
        validation.put("errors", errors);
        validation.put("warnings", warnings);
        return validation;
    }

    private void validateSalaryStructureHeaders(String[] headers, List<String> errors) {
        List<String> requiredColumns = Arrays.asList("salary structure", "name", "Abbr", "type", "valeur", "company");
        List<String> headerList = Arrays.asList(headers);
        
        for (String required : requiredColumns) {
            if (!headerList.contains(required)) {
                String match = findBestMatch(required, headers);
                if (match == null) {
                    errors.add("Colonne obligatoire manquante: " + required);
                }
            }
        }
    }

    private void validateSalaryStructureData(CSVReader reader, String[] headers, List<String> errors, 
                                           List<String> warnings, Set<String> structureRefs, Set<String> componentAbbrs) throws IOException, CsvValidationException {
        String[] line;
        int rowNum = 1;
        Map<String, Integer> columnIndexes = createColumnIndexMap(headers);

        while ((line = reader.readNext()) != null) {
            rowNum++;
            if (line.length < headers.length) {
                errors.add(String.format("Ligne %d: Données incomplètes", rowNum));
                continue;
            }

            String ref = getValue(line, columnIndexes, "salary structure");
            String name = getValue(line, columnIndexes, "name");
            String abbr = getValue(line, columnIndexes, "Abbr");
            String type = getValue(line, columnIndexes, "type");
            String valeur = getValue(line, columnIndexes, "valeur");
            String company = getValue(line, columnIndexes, "company");

            if (ref == null || ref.trim().isEmpty()) {
                errors.add(String.format("Ligne %d: salary structure est obligatoire", rowNum));
            } else if (structureRefs.contains(ref)) {
                errors.add(String.format("Ligne %d: salary structure %s est en double", rowNum, ref));
            } else {
                structureRefs.add(ref);
            }

            if (name == null || name.trim().isEmpty()) {
                errors.add(String.format("Ligne %d: name est obligatoire", rowNum));
            }

            if (abbr == null || abbr.trim().isEmpty()) {
                errors.add(String.format("Ligne %d: Abbr est obligatoire", rowNum));
            } else if (componentAbbrs.contains(abbr)) {
                errors.add(String.format("Ligne %d: Abbr %s est en double", rowNum, abbr));
            } else {
                componentAbbrs.add(abbr);
            }

            if (type == null || (!type.equals("earning") && !type.equals("deduction"))) {
                errors.add(String.format("Ligne %d: type doit être 'earning' ou 'deduction'", rowNum));
            }

            if (valeur == null || valeur.trim().isEmpty()) {
                errors.add(String.format("Ligne %d: valeur est obligatoire", rowNum));
            } else if (!isValidFormula(valeur)) {
                try {
                    double amount = Double.parseDouble(valeur);
                    if (amount < 0) {
                        errors.add(String.format("Ligne %d: valeur doit être positif", rowNum));
                    }
                } catch (NumberFormatException e) {
                    errors.add(String.format("Ligne %d: valeur doit être un nombre valide ou une formule", rowNum));
                }
            }

            if (company == null || !company.equals("My Company")) {
                errors.add(String.format("Ligne %d: company doit être 'My Company'", rowNum));
            }
        }
    }

    public Map<String, List<String>> validateSalaryAssignmentFile(String filePath) throws CsvValidationException {
        Map<String, List<String>> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> assignmentKeys = new HashSet<>();
        
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headers = reader.readNext();
            if (headers == null) {
                errors.add("Fichier vide ou en-têtes manquants");
                validation.put("errors", errors);
                validation.put("warnings", warnings);
                return validation;
            }
            
            validateSalaryAssignmentHeaders(headers, errors);
            if (!errors.isEmpty()) {
                validation.put("errors", errors);
                validation.put("warnings", warnings);
                return validation;
            }
            
            validateSalaryAssignmentData(reader, headers, errors, warnings, assignmentKeys);
            
            // Vérifier les doublons dans HRMS
            for (String key : assignmentKeys) {
                String[] parts = key.split(":");
                String employeeRef = parts[0];
                String mois = parts[1];
                ResponseEntity<Map> response = restTemplate.getForEntity(
                    erpnextUrl + "/api/resource/Salary Slip?filters=[['employee','=','" + employeeRef + "'],['posting_date','=','" + mois + "']]",
                    Map.class
                );
                if (response.getStatusCode() == HttpStatus.OK && !((List) response.getBody().get("data")).isEmpty()) {
                    errors.add("Fiche de paie pour employé " + employeeRef + " et mois " + mois + " existe déjà dans HRMS");
                }
            }
        } catch (IOException e) {
            errors.add("Erreur lors de la lecture du fichier: " + e.getMessage());
        }
        
        validation.put("errors", errors);
        validation.put("warnings", warnings);
        return validation;
    }

    private void validateSalaryAssignmentHeaders(String[] headers, List<String> errors) {
        List<String> requiredColumns = Arrays.asList("Mois", "Ref Employe", "Salaire Base", "Salaire", "company");
        List<String> headerList = Arrays.asList(headers);
        
        for (String required : requiredColumns) {
            if (!headerList.contains(required)) {
                String match = findBestMatch(required, headers);
                if (match == null) {
                    errors.add("Colonne obligatoire manquante: " + required);
                }
            }
        }
    }

    private void validateSalaryAssignmentData(CSVReader reader, String[] headers, List<String> errors, 
                                            List<String> warnings, Set<String> assignmentKeys) throws IOException, CsvValidationException {
        String[] line;
        int rowNum = 1;
        Map<String, Integer> columnIndexes = createColumnIndexMap(headers);

        while ((line = reader.readNext()) != null) {
            rowNum++;
            if (line.length < headers.length) {
                errors.add(String.format("Ligne %d: Données incomplètes", rowNum));
                continue;
            }

            String mois = getValue(line, columnIndexes, "Mois");
            String employeeRef = getValue(line, columnIndexes, "Ref Employe");
            String salaireBase = getValue(line, columnIndexes, "Salaire Base");
            String salaire = getValue(line, columnIndexes, "Salaire");
            String company = getValue(line, columnIndexes, "company");

            if (mois == null || mois.trim().isEmpty()) {
                errors.add(String.format("Ligne %d: Mois est obligatoire", rowNum));
            } else {
                try {
                    transformDate(mois);
                } catch (ParseException e) {
                    errors.add(String.format("Ligne %d: Format de Mois invalide (attendu: DD/MM/YYYY ou YYYY-MM-DD)", rowNum));
                }
            }

            if (employeeRef == null || employeeRef.trim().isEmpty()) {
                errors.add(String.format("Ligne %d: Ref Employe est obligatoire", rowNum));
            }

            if (salaireBase == null || salaireBase.trim().isEmpty()) {
                errors.add(String.format("Ligne %d: Salaire Base est obligatoire", rowNum));
            } else {
                try {
                    double amount = Double.parseDouble(salaireBase);
                    if (amount < 0) {
                        errors.add(String.format("Ligne %d: Salaire Base doit être positif", rowNum));
                    }
                } catch (NumberFormatException e) {
                    errors.add(String.format("Ligne %d: Salaire Base doit être un nombre valide", rowNum));
                }
            }

            if (salaire == null || salaire.trim().isEmpty()) {
                errors.add(String.format("Ligne %d: Salaire est obligatoire", rowNum));
            }

            if (company == null || !company.equals("My Company")) {
                errors.add(String.format("Ligne %d: company doit être 'My Company'", rowNum));
            }

            String assignmentKey = employeeRef + ":" + mois;
            if (assignmentKeys.contains(assignmentKey)) {
                errors.add(String.format("Ligne %d: Fiche de paie pour employé %s et mois %s est en double", rowNum, employeeRef, mois));
            } else {
                assignmentKeys.add(assignmentKey);
            }
        }
    }

    public Map<String, Object> validateReferenceData(String employeeFilePath, 
                                                    String salaryStructureFilePath, 
                                                    String salaryAssignmentFilePath) throws CsvValidationException {
        Map<String, Object> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try {
            Set<String> employeeIds = collectEmployeeIds(employeeFilePath);
            Set<String> structureIds = collectSalaryStructureIds(salaryStructureFilePath);
            
            validateSalaryAssignmentReferences(salaryAssignmentFilePath, employeeIds, structureIds, errors);
            
        } catch (IOException e) {
            errors.add("Erreur lors de la validation des références: " + e.getMessage());
        }
        
        validation.put("errors", errors);
        validation.put("warnings", warnings);
        return validation;
    }

    private Set<String> collectEmployeeIds(String filePath) throws IOException, CsvValidationException {
        Set<String> employeeIds = new HashSet<>();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headers = reader.readNext();
            if (headers == null) return employeeIds;
            
            Map<String, Integer> columnIndexes = createColumnIndexMap(headers);
            String[] line;
            
            while ((line = reader.readNext()) != null) {
                String ref = getValue(line, columnIndexes, "Ref");
                if (ref != null && !ref.trim().isEmpty()) {
                    employeeIds.add(ref.trim());
                }
            }
        }
        return employeeIds;
    }

    private Set<String> collectSalaryStructureIds(String filePath) throws IOException, CsvValidationException {
        Set<String> structureIds = new HashSet<>();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headers = reader.readNext();
            if (headers == null) return structureIds;
            
            Map<String, Integer> columnIndexes = createColumnIndexMap(headers);
            String[] line;
            
            while ((line = reader.readNext()) != null) {
                String ref = getValue(line, columnIndexes, "salary structure");
                if (ref != null && !ref.trim().isEmpty()) {
                    structureIds.add(ref.trim());
                }
            }
        }
        return structureIds;
    }

    private void validateSalaryAssignmentReferences(String filePath, Set<String> employeeIds, 
                                                  Set<String> structureIds, List<String> errors) throws IOException, CsvValidationException {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headers = reader.readNext();
            if (headers == null) return;
            
            Map<String, Integer> columnIndexes = createColumnIndexMap(headers);
            String[] line;
            int rowNum = 1;
            
            while ((line = reader.readNext()) != null) {
                rowNum++;
                
                String employeeRef = getValue(line, columnIndexes, "Ref Employe");
                String structureRef = getValue(line, columnIndexes, "Salaire");

                if (employeeRef != null && !employeeRef.trim().isEmpty() && 
                    !employeeIds.contains(employeeRef.trim())) {
                    errors.add(String.format("Ligne %d: Ref Employe %s n'existe pas dans le fichier employé", 
                               rowNum, employeeRef));
                }
                
                if (structureRef != null && !structureRef.trim().isEmpty() && 
                    !structureIds.contains(structureRef.trim())) {
                    errors.add(String.format("Ligne %d: Salaire %s n'existe pas dans le fichier structure", 
                               rowNum, structureRef));
                }
            }
        }
    }

    private void validateEmployeeRef(String ref, int rowNum, Set<String> employeeRefs, List<String> errors) {
        if (ref == null || ref.trim().isEmpty()) {
            errors.add(String.format("Ligne %d: Ref est obligatoire", rowNum));
        } else if (employeeRefs.contains(ref)) {
            errors.add(String.format("Ligne %d: Ref %s existe déjà", rowNum, ref));
        } else {
            employeeRefs.add(ref);
        }
    }

    private void validateNameField(String name, String fieldName, int rowNum, List<String> errors) {
        if (name == null || name.trim().isEmpty()) {
            errors.add(String.format("Ligne %d: %s est obligatoire", rowNum, fieldName));
        } else if (name.length() > 140) {
            errors.add(String.format("Ligne %d: %s ne peut pas dépasser 140 caractères", rowNum, fieldName));
        }
    }

    private void validateGender(String gender, int rowNum, List<String> errors) {
        if (gender == null || gender.trim().isEmpty()) {
            errors.add(String.format("Ligne %d: Genre est obligatoire", rowNum));
        } else if (!gender.equals("Masculin") && !gender.equals("Feminin") && 
                   !gender.equals("Male") && !gender.equals("Female")) {
            errors.add(String.format("Ligne %d: Genre doit être 'Masculin', 'Feminin', 'Male' ou 'Female'", rowNum));
        }
    }

    private void validateBirthDate(String birthDate, int rowNum, List<String> errors) {
        if (birthDate == null || birthDate.trim().isEmpty()) {
            errors.add(String.format("Ligne %d: Date de naissance est obligatoire", rowNum));
            return;
        }
        
        try {
            Date date = parseDate(birthDate);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, -70);
            Date maxDate = cal.getTime();
            cal.add(Calendar.YEAR, 54); // -16 ans
            Date minDate = cal.getTime();
            
            if (date.before(maxDate)) {
                errors.add(String.format("Ligne %d: Âge trop élevé (plus de 70 ans)", rowNum));
            } else if (date.after(minDate)) {
                errors.add(String.format("Ligne %d: Âge trop faible (moins de 16 ans)", rowNum));
            }
        } catch (ParseException e) {
            errors.add(String.format("Ligne %d: Format de date de naissance invalide (attendu: DD/MM/YYYY ou YYYY-MM-DD)", rowNum));
        }
    }

    private void validateHireDate(String hireDate, int rowNum, List<String> errors) {
        if (hireDate == null || hireDate.trim().isEmpty()) {
            errors.add(String.format("Ligne %d: Date d'embauche est obligatoire", rowNum));
            return;
        }
        
        try {
            Date date = parseDate(hireDate);
            Date today = new Date();
            
            if (date.after(today)) {
                errors.add(String.format("Ligne %d: Date d'embauche ne peut pas être future", rowNum));
            }
        } catch (ParseException e) {
            errors.add(String.format("Ligne %d: Format de date d'embauche invalide (attendu: DD/MM/YYYY ou YYYY-MM-DD)", rowNum));
        }
    }

    private void validateCompany(String company, int rowNum, List<String> errors) {
        if (company == null || !company.equals("My Company")) {
            errors.add(String.format("Ligne %d: company doit être 'My Company'", rowNum));
        }
    }

    private Date parseDate(String dateStr) throws ParseException {
        String[] formats = {"dd/MM/yyyy", "yyyy-MM-dd", "MM/dd/yyyy", "dd-MM-yyyy"};
        
        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setLenient(false);
                return sdf.parse(dateStr);
            } catch (ParseException e) {
                // Continuer avec le format suivant
            }
        }
        
        throw new ParseException("Format de date non reconnu: " + dateStr, 0);
    }

    private boolean isValidFormula(String formula) {
        if (formula == null || formula.trim().isEmpty()) {
            return false;
        }
        Pattern formulaPattern = Pattern.compile("^[A-Za-z0-9_\\s\\+\\-\\*\\/\\.\\(\\)]+$");
        return formulaPattern.matcher(formula).matches();
    }

    private Map<String, Object> transformEmployeeData(String[] headers, String[] data) throws ParseException {
        Map<String, Object> employee = new HashMap<>();
        Map<String, Integer> columnMap = createColumnIndexMap(headers);
    
        employee.put("doctype", "Employee");
        employee.put("employee_number", getValue(data, columnMap, "Ref"));
        employee.put("first_name", getValue(data, columnMap, "Prenom"));
        employee.put("last_name", getValue(data, columnMap, "Nom"));
        employee.put("employee_name", getValue(data, columnMap, "Prenom") + " " + getValue(data, columnMap, "Nom"));
        employee.put("gender", transformGender(getValue(data, columnMap, "Genre")));
        employee.put("date_of_birth", transformDate(getValue(data, columnMap, "Date_Naissance")));
        employee.put("date_of_joining", transformDate(getValue(data, columnMap, "Date_Embauche")));
        employee.put("company", getValue(data, columnMap, "company"));
    
        return employee;
    }

    private String transformGender(String gender) {
        if ("Masculin".equalsIgnoreCase(gender)) return "Male";
        if ("Feminin".equalsIgnoreCase(gender)) return "Female";
        return gender;
    }

    private String transformDate(String date) throws ParseException {
        String[] formats = {"dd/MM/yyyy", "yyyy-MM-dd", "MM/dd/yyyy", "dd-MM-yyyy"};
        for (String format : formats) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat(format);
                inputFormat.setLenient(false);
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
                return outputFormat.format(inputFormat.parse(date));
            } catch (ParseException e) {
                // Continuer avec le format suivant
            }
        }
        throw new ParseException("Format de date non reconnu: " + date, 0);
    }

    private Map<String, Object> transformSalaryComponent(String[] headers, String[] data) {
        Map<String, Object> component = new HashMap<>();
        Map<String, Integer> columnMap = createColumnIndexMap(headers);
    
        component.put("doctype", "Salary Component");
        component.put("salary_component", getValue(data, columnMap, "name"));
        component.put("abbr", getValue(data, columnMap, "Abbr"));
        component.put("type", getValue(data, columnMap, "type").equalsIgnoreCase("earning") ? "Earning" : "Deduction");
    
        return component;
    }

    private Map<String, Object> transformSalaryStructure(String[] headers, List<String[]> data) {
        Map<String, Object> structure = new HashMap<>();
        List<Map<String, Object>> earnings = new ArrayList<>();
        List<Map<String, Object>> deductions = new ArrayList<>();
        Map<String, Integer> columnMap = createColumnIndexMap(headers);
        String structureName = getValue(data.get(0), columnMap, "salary structure");
        String company = getValue(data.get(0), columnMap, "company");

        Map<String, Double> variables = new HashMap<>();
        for (String[] line : data) {
            String type = getValue(line, columnMap, "type");
            String valeur = getValue(line, columnMap, "valeur");
            String abbr = getValue(line, columnMap, "Abbr");
            double amount = evaluateFormula(valeur, variables);
            variables.put(abbr, amount);

            Map<String, Object> component = new HashMap<>();
            component.put("salary_component", getValue(line, columnMap, "name"));
            component.put("amount", amount);

            if (type.equalsIgnoreCase("earning")) {
                earnings.add(component);
            } else {
                deductions.add(component);
            }
        }

        structure.put("doctype", "Salary Structure");
        structure.put("name", structureName);
        structure.put("company", company);
        structure.put("earnings", earnings);
        structure.put("deductions", deductions);
    
        return structure;
    }

    private double evaluateFormula(String formula, Map<String, Double> variables) {
        if (formula == null || formula.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(formula);
        } catch (NumberFormatException e) {
            // Évaluer les formules simples comme "SB * 0.3" ou "(SB + IND) * 0.2"
            String normalized = formula.replaceAll("\\s+", "");
            for (Map.Entry<String, Double> entry : variables.entrySet()) {
                normalized = normalized.replace(entry.getKey(), String.valueOf(entry.getValue()));
            }
            // Simplification pour cet exemple; utiliser un parseur d'expressions si nécessaire
            if (normalized.contains("*")) {
                String[] parts = normalized.split("\\*");
                if (parts.length == 2) {
                    return Double.parseDouble(parts[0]) * Double.parseDouble(parts[1]);
                }
            } else if (normalized.contains("+") && normalized.contains("*")) {
                String[] parts = normalized.replace("(", "").replace(")", "").split("\\*");
                String[] sumParts = parts[0].split("\\+");
                double sum = Double.parseDouble(sumParts[0]) + Double.parseDouble(sumParts[1]);
                return sum * Double.parseDouble(parts[1]);
            }
            return 0.0;
        }
    }

    private Map<String, Object> transformSalarySlip(String[] headers, String[] data, Map<String, String> employeeRefs, Map<String, String> structureRefs) throws ParseException {
        Map<String, Object> slip = new HashMap<>();
        Map<String, Integer> columnMap = createColumnIndexMap(headers);
    
        slip.put("doctype", "Salary Slip");
        slip.put("employee", employeeRefs.get(getValue(data, columnMap, "Ref Employe")));
        slip.put("posting_date", transformDate(getValue(data, columnMap, "Mois")));
        slip.put("salary_structure", structureRefs.get(getValue(data, columnMap, "Salaire")));
        slip.put("company", getValue(data, columnMap, "company"));
        
        List<Map<String, Object>> earnings = new ArrayList<>();
        Map<String, Object> baseEarning = new HashMap<>();
        baseEarning.put("salary_component", "Salaire Base");
        baseEarning.put("amount", parseAmount(getValue(data, columnMap, "Salaire Base")));
        earnings.add(baseEarning);
        
        slip.put("earnings", earnings);
    
        return slip;
    }

    private double parseAmount(String amount) {
        try {
            return Double.parseDouble(amount.replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public Map<String, Object> importToErpnext(String employeeFilePath, String structureFilePath, String assignmentFilePath) throws IOException, CsvValidationException {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> success = new ArrayList<>();
        List<String> createdRecords = new ArrayList<>();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "token " + apiKey + ":" + apiSecret);

        try {
            // 1. Import employees
            Map<String, String> employeeRefs = importEmployees(employeeFilePath, headers, errors, success, createdRecords);
            if (!errors.isEmpty()) {
                result.put("success", false);
                result.put("errors", errors);
                result.put("messages", success);
                rollback(createdRecords, headers, errors);
                return result;
            }

            // 2. Import salary components and structures
            Map<String, String> structureRefs = importSalaryStructures(structureFilePath, headers, errors, success, createdRecords);
            if (!errors.isEmpty()) {
                result.put("success", false);
                result.put("errors", errors);
                result.put("messages", success);
                rollback(createdRecords, headers, errors);
                return result;
            }

            // 3. Import salary assignments
            int assignments = importSalaryAssignments(assignmentFilePath, employeeRefs, structureRefs, headers, errors, success, createdRecords);
            if (!errors.isEmpty()) {
                result.put("success", false);
                result.put("errors", errors);
                result.put("messages", success);
                rollback(createdRecords, headers, errors);
                return result;
            }

            success.add(String.format("%d salary assignments imported", assignments));
        
        } catch (Exception e) {
            errors.add("Import failed: " + e.getMessage());
            rollback(createdRecords, headers, errors);
        }
    
        result.put("success", errors.isEmpty());
        result.put("errors", errors);
        result.put("messages", success);
        return result;
    }
    private Map<String, String> importEmployees(String filePath, HttpHeaders headers, List<String> errors, List<String> success, List<String> createdRecords) throws IOException, CsvValidationException, ParseException {
        Map<String, String> employeeRefs = new HashMap<>();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] csvHeaders = reader.readNext();
            if (csvHeaders == null) {
                errors.add("Fichier employé vide ou en-têtes manquants");
                return employeeRefs;
            }
            String[] line;
            int rowNum = 1;
            while ((line = reader.readNext()) != null) {
                rowNum++;
                Map<String, Object> employee = transformEmployeeData(csvHeaders, line);
                String employeeRef = (String) employee.get("employee_number");
                
                ResponseEntity<Map> checkResponse = restTemplate.getForEntity(
                    erpnextUrl + "/api/resource/Employee?filters=[['employee_number','=','" + employeeRef + "']]",
                    Map.class
                );
                
                if (checkResponse.getStatusCode() == HttpStatus.OK && !((List) checkResponse.getBody().get("data")).isEmpty()) {
                    errors.add(String.format("Ligne %d: Employé avec Ref %s existe déjà dans HRMS", rowNum, employeeRef));
                    continue;
                }
                
                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(employee, headers);
                ResponseEntity<Map> response = restTemplate.postForEntity(
                    erpnextUrl + "/api/resource/Employee",
                    requestEntity,
                    Map.class
                );
                
                if (response.getStatusCode() == HttpStatus.OK) {
                    String name = (String) ((Map) response.getBody().get("data")).get("name");
                    employeeRefs.put(employeeRef, name);
                    createdRecords.add("Employee:" + name);
                    success.add(String.format("Ligne %d: Employé %s créé avec succès", rowNum, employeeRef));
                } else {
                    String errorMsg = response.getBody() != null ? (String) response.getBody().get("message") : response.getStatusCode().toString();
                    errors.add(String.format("Ligne %d: Échec de la création de l'employé %s: %s", rowNum, employeeRef, errorMsg));
                }
            }
        }
        return employeeRefs;
    }
    
    private Map<String, String> importSalaryStructures(String filePath, HttpHeaders httpHeaders, List<String> errors, List<String> success, List<String> createdRecords) throws IOException, CsvValidationException {
        Map<String, String> structureRefs = new HashMap<>();
        Map<String, List<String[]>> structureData = new HashMap<>();
        String[] csvHeaders;
        
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            csvHeaders = reader.readNext();
            if (csvHeaders == null) {
                errors.add("Fichier structure salariale vide ou en-têtes manquants");
                return structureRefs;
            }
            String[] line;
            while ((line = reader.readNext()) != null) {
                String structureName = getValue(line, createColumnIndexMap(csvHeaders), "salary structure");
                structureData.computeIfAbsent(structureName, k -> new ArrayList<>()).add(line);
            }
        }
        
        for (Map.Entry<String, List<String[]>> entry : structureData.entrySet()) {
            String structureName = entry.getKey();
            List<String[]> data = entry.getValue();
            
            for (String[] line : data) {
                Map<String, Object> component = transformSalaryComponent(csvHeaders, line);
                String abbr = (String) component.get("abbr");
                
                ResponseEntity<Map> checkResponse = restTemplate.getForEntity(
                    erpnextUrl + "/api/resource/Salary Component?filters=[['abbr','=','" + abbr + "']]",
                    Map.class
                );
                
                if (checkResponse.getStatusCode() == HttpStatus.OK && !((List) checkResponse.getBody().get("data")).isEmpty()) {
                    success.add(String.format("Composant de salaire %s existe déjà, réutilisation", abbr));
                    continue;
                }
                
                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(component, httpHeaders);
                ResponseEntity<Map> response = restTemplate.postForEntity(
                    erpnextUrl + "/api/resource/Salary Component",
                    requestEntity,
                    Map.class
                );
                
                if (response.getStatusCode() == HttpStatus.OK) {
                    String name = (String) ((Map) response.getBody().get("data")).get("name");
                    createdRecords.add("Salary Component:" + name);
                    success.add(String.format("Composant de salaire %s créé avec succès", abbr));
                } else {
                    String errorMsg = response.getBody() != null ? (String) response.getBody().get("message") : response.getStatusCode().toString();
                    errors.add(String.format("Échec de la création du composant %s: %s", abbr, errorMsg));
                    continue;
                }
            }
            
            if (!errors.isEmpty()) {
                continue;
            }
            
            Map<String, Object> structure = transformSalaryStructure(csvHeaders, data);
            ResponseEntity<Map> checkResponse = restTemplate.getForEntity(
                erpnextUrl + "/api/resource/Salary Structure?filters=[['name','=','" + structureName + "']]",
                Map.class
            );
            
            if (checkResponse.getStatusCode() == HttpStatus.OK && !((List) checkResponse.getBody().get("data")).isEmpty()) {
                errors.add(String.format("Structure de salaire %s existe déjà dans HRMS", structureName));
                continue;
            }
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(structure, httpHeaders);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                erpnextUrl + "/api/resource/Salary Structure",
                requestEntity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                String name = (String) ((Map) response.getBody().get("data")).get("name");
                structureRefs.put(structureName, name);
                createdRecords.add("Salary Structure:" + name);
                success.add(String.format("Structure de salaire %s créée avec succès", structureName));
            } else {
                String errorMsg = response.getBody() != null ? (String) response.getBody().get("message") : response.getStatusCode().toString();
                errors.add(String.format("Échec de la création de la structure %s: %s", structureName, errorMsg));
            }
        }
        
        return structureRefs;
    }
    
    private int importSalaryAssignments(String filePath, Map<String, String> employeeRefs, 
            Map<String, String> structureRefs, HttpHeaders httpHeaders, List<String> errors, List<String> success, List<String> createdRecords) throws IOException, CsvValidationException, ParseException {
        int count = 0;
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headers = reader.readNext(); // Single declaration of headers
            if (headers == null) {
                errors.add("Fichier attribution salaires vide ou en-têtes manquants");
                return count;
            }
            String[] line;
            int rowNum = 1;
            while ((line = reader.readNext()) != null) {
                rowNum++;
                Map<String, Integer> columnMap = createColumnIndexMap(headers);
                String employeeRef = getValue(line, columnMap, "Ref Employe");
                String structureRef = getValue(line, columnMap, "Salaire");
    
                if (employeeRefs.containsKey(employeeRef) && structureRefs.containsKey(structureRef)) {
                    Map<String, Object> slip = transformSalarySlip(headers, line, employeeRefs, structureRefs); // Use String[] headers
                    
                    ResponseEntity<Map> checkResponse = restTemplate.getForEntity(
                        erpnextUrl + "/api/resource/Salary Slip?filters=[['employee','=','" + employeeRefs.get(employeeRef) + "'],['posting_date','=','" + slip.get("posting_date") + "']]",
                        Map.class
                    );
                    
                    if (checkResponse.getStatusCode() == HttpStatus.OK && !((List) checkResponse.getBody().get("data")).isEmpty()) {
                        errors.add(String.format("Ligne %d: Fiche de paie pour employé %s et mois %s existe déjà", rowNum, employeeRef, slip.get("posting_date")));
                        continue;
                    }
                    
                    HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(slip, httpHeaders);
                    ResponseEntity<Map> response = restTemplate.postForEntity(
                        erpnextUrl + "/api/resource/Salary Slip",
                        requestEntity,
                        Map.class
                    );
                    
                    if (response.getStatusCode() == HttpStatus.OK) {
                        String name = (String) ((Map) response.getBody().get("data")).get("name");
                        createdRecords.add("Salary Slip:" + name);
                        success.add(String.format("Ligne %d: Fiche de paie pour employé %s créée avec succès", rowNum, employeeRef));
                        count++;
                    } else {
                        String errorMsg = response.getBody() != null ? (String) response.getBody().get("message") : response.getStatusCode().toString();
                        errors.add(String.format("Ligne %d: Échec de la création de la fiche de paie pour employé %s: %s", rowNum, employeeRef, errorMsg));
                    }
                } else {
                    if (!employeeRefs.containsKey(employeeRef)) {
                        errors.add(String.format("Ligne %d: Ref Employe %s non trouvé", rowNum, employeeRef));
                    }
                    if (!structureRefs.containsKey(structureRef)) {
                        errors.add(String.format("Ligne %d: Salaire %s non trouvé", rowNum, structureRef));
                    }
                }
            }
        }
        return count;
    }
    
    private void rollback(List<String> createdRecords, HttpHeaders headers, List<String> errors) {
        for (String record : createdRecords) {
            String[] parts = record.split(":");
            String doctype = parts[0];
            String name = parts[1];
            try {
                restTemplate.delete(erpnextUrl + "/api/resource/" + doctype + "/" + name, headers);
            } catch (Exception e) {
                errors.add("Échec du rollback pour " + doctype + " " + name + ": " + e.getMessage());
            }
        }
=======
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

    // Méthode pour importer seulement les structures de salaire
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

=======
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

    // Méthode pour importer seulement les structures de salaire
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

>>>>>>> Stashed changes
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

    // Méthode pour importer seulement les affectations de salaire
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
<<<<<<< Updated upstream
>>>>>>> Stashed changes
=======
>>>>>>> Stashed changes
    }
}