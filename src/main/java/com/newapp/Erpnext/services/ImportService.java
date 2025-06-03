package com.newapp.Erpnext.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.opencsv.CSVReader;
import java.io.FileReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class ImportService {

    @Value("${java.io.tmpdir}")
    private String tempDir;

    /**
     * Stocke un fichier téléchargé dans un répertoire temporaire
     * 
     * @param file Le fichier à stocker
     * @param prefix Un préfixe pour le nom du fichier
     * @return Le chemin du fichier stocké
     * @throws IOException Si une erreur survient lors du stockage
     */
    public String storeFile(MultipartFile file, String prefix) throws IOException {
        // Créer un nom de fichier unique
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = prefix + "_" + UUID.randomUUID().toString() + fileExtension;
        
        // Créer le répertoire temporaire s'il n'existe pas
        Path uploadDir = Paths.get(tempDir, "erpnext-imports");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        
        // Stocker le fichier
        Path targetPath = uploadDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        return targetPath.toString();
    }
    
    /**
     * Obtient une ressource de fichier à partir d'un chemin
     * 
     * @param filePath Le chemin du fichier
     * @return La ressource du fichier
     */
    public Resource getFileResource(String filePath) {
        return new FileSystemResource(filePath);
    }
    
    /**
     * Supprime un fichier temporaire
     * 
     * @param filePath Le chemin du fichier à supprimer
     * @return true si le fichier a été supprimé, false sinon
     */
    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }
    
    // Ajouter les méthodes de validation
    public Map<String, List<String>> validateEmployeeFile(String filePath) {
        Map<String, List<String>> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            // Lire les en-têtes
            String[] headers = reader.readNext();
            
            // Vérifier les colonnes obligatoires
            List<String> requiredColumns = Arrays.asList("Ref", "Nom", "Prenom", "company");
            for (String required : requiredColumns) {
                if (!Arrays.asList(headers).contains(required)) {
                    errors.add("Colonne obligatoire manquante: " + required);
                }
            }
            
            // Lire et valider les données
            String[] line;
            int rowNum = 1;
            while ((line = reader.readNext()) != null) {
                rowNum++;
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    row.put(headers[i], line[i]);
                }
                
                // Validation Ref (numérique)
                if (!row.get("Ref").matches("\\d+")) {
                    errors.add(String.format("Ligne %d: Ref doit être numérique", rowNum));
                }
                
                // Validation Nom/Prénom
                if (row.get("Nom").length() > 140 || row.get("Prenom").length() > 140) {
                    errors.add(String.format("Ligne %d: Nom/Prénom ne doit pas dépasser 140 caractères", rowNum));
                }
                
                // Validation dates
                validateDates(row, rowNum, errors);
            }
        } catch (Exception e) {
            errors.add("Erreur lors de la lecture du fichier: " + e.getMessage());
        }
        
        validation.put("errors", errors);
        validation.put("warnings", warnings);
        return validation;
    }
    
    // Méthodes similaires pour validateSalaryStructureFile et validateSalaryAssignmentFile
    private void validateDates(Map<String, String> row, int rowNum, List<String> errors) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        
        // Validate hire date if present
        String hireDate = row.get("hire_date");
        if (hireDate != null && !hireDate.isEmpty()) {
            try {
                Date date = dateFormat.parse(hireDate);
                if (date.after(new Date())) {
                    errors.add(String.format("Line %d: Hire date cannot be in the future", rowNum));
                }
            } catch (ParseException e) {
                errors.add(String.format("Line %d: Invalid hire date format. Use YYYY-MM-DD", rowNum));
            }
        }
    }

    public Map<String, List<String>> validateSalaryStructureFile(String filePath) {
        Map<String, List<String>> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headers = reader.readNext();
            
            List<String> requiredColumns = Arrays.asList("salary_structure_id", "base_salary", "currency");
            for (String required : requiredColumns) {
                if (!Arrays.asList(headers).contains(required)) {
                    errors.add("Missing required column: " + required);
                }
            }
            
            String[] line;
            int rowNum = 1;
            while ((line = reader.readNext()) != null) {
                rowNum++;
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    row.put(headers[i], line[i]);
                }
                
                // Validate salary_structure_id format
                if (!row.get("salary_structure_id").matches("[A-Za-z0-9_-]+")) {
                    errors.add(String.format("Line %d: Invalid salary_structure_id format", rowNum));
                }
                
                // Validate base_salary (numeric and positive)
                try {
                    double baseSalary = Double.parseDouble(row.get("base_salary"));
                    if (baseSalary <= 0) {
                        errors.add(String.format("Line %d: Base salary must be positive", rowNum));
                    }
                } catch (NumberFormatException e) {
                    errors.add(String.format("Line %d: Invalid base salary format", rowNum));
                }
            }
        } catch (Exception e) {
            errors.add("Error reading file: " + e.getMessage());
        }
        
        validation.put("errors", errors);
        validation.put("warnings", warnings);
        return validation;
    }

    public Map<String, List<String>> validateSalaryAssignmentFile(String filePath) {
        Map<String, List<String>> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headers = reader.readNext();
            
            List<String> requiredColumns = Arrays.asList("employee_id", "salary_structure_id", "from_date");
            for (String required : requiredColumns) {
                if (!Arrays.asList(headers).contains(required)) {
                    errors.add("Missing required column: " + required);
                }
            }
            
            String[] line;
            int rowNum = 1;
            while ((line = reader.readNext()) != null) {
                rowNum++;
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    row.put(headers[i], line[i]);
                }
                
                // Validate employee_id format
                if (!row.get("employee_id").matches("\\d+")) {
                    errors.add(String.format("Line %d: Employee ID must be numeric", rowNum));
                }
                
                // Validate dates
                validateDates(row, rowNum, errors);
            }
        } catch (Exception e) {
            errors.add("Error reading file: " + e.getMessage());
        }
        
        validation.put("errors", errors);
        validation.put("warnings", warnings);
        return validation;
    }

    public Map<String, Object> validateReferenceData(String employeeFilePath, 
            String salaryStructureFilePath, String salaryAssignmentFilePath) {
        Map<String, Object> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();
        
        try {
            // Collect all employee IDs
            Set<String> employeeIds = new HashSet<>();
            try (CSVReader reader = new CSVReader(new FileReader(employeeFilePath))) {
                String[] headers = reader.readNext();
                int refIndex = Arrays.asList(headers).indexOf("Ref");
                
                String[] line;
                while ((line = reader.readNext()) != null) {
                    employeeIds.add(line[refIndex]);
                }
            }
            
            // Collect all salary structure IDs
            Set<String> structureIds = new HashSet<>();
            try (CSVReader reader = new CSVReader(new FileReader(salaryStructureFilePath))) {
                String[] headers = reader.readNext();
                int idIndex = Arrays.asList(headers).indexOf("salary_structure_id");
                
                String[] line;
                while ((line = reader.readNext()) != null) {
                    structureIds.add(line[idIndex]);
                }
            }
            
            // Validate references in salary assignments
            try (CSVReader reader = new CSVReader(new FileReader(salaryAssignmentFilePath))) {
                String[] headers = reader.readNext();
                int empIndex = Arrays.asList(headers).indexOf("employee_id");
                int structIndex = Arrays.asList(headers).indexOf("salary_structure_id");
                
                String[] line;
                int rowNum = 1;
                while ((line = reader.readNext()) != null) {
                    rowNum++;
                    String empId = line[empIndex];
                    String structId = line[structIndex];
                    
                    if (!employeeIds.contains(empId)) {
                        errors.add(String.format("Line %d: Employee ID %s not found in employee file", rowNum, empId));
                    }
                    
                    if (!structureIds.contains(structId)) {
                        errors.add(String.format("Line %d: Salary structure ID %s not found in salary structure file", rowNum, structId));
                    }
                }
            }
            
        } catch (Exception e) {
            errors.add("Error validating reference data: " + e.getMessage());
        }
        
        validation.put("errors", errors);
        return validation;
    }
}