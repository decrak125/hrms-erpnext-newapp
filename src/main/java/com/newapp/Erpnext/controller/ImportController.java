package com.newapp.Erpnext.controller;

import com.newapp.Erpnext.services.ImportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.newapp.Erpnext.services.SessionService;
import com.opencsv.exceptions.CsvValidationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/import")
public class ImportController {

    @Autowired
    private ImportService importService;

    @Autowired
    private SessionService sessionService;

    @GetMapping
    public String showImportPage(Model model, HttpSession session) {
        if (!sessionService.isAuthenticated()) {
            return "redirect:/login";
        }
        return "import";
    }

    @PostMapping("/api/complete")
    public String importCompleteData(
            @RequestParam("employee_file") MultipartFile employeeFile,
            @RequestParam("structure_file") MultipartFile structureFile,
            @RequestParam("salary_file") MultipartFile salaryFile,
            HttpSession session,
            Model model) {
        
        try {
            // Validation des fichiers
            if (employeeFile.isEmpty() || structureFile.isEmpty() || salaryFile.isEmpty()) {
                model.addAttribute("errors", Map.of(
                    "message", List.of("Tous les fichiers sont requis")
                ));
                return "import";
            }

            // Validation de la session
            if (session.getAttribute("sid") == null) {
                model.addAttribute("errors", Map.of(
                    "message", List.of("Session invalide - Veuillez vous reconnecter")
                ));
                return "import";
            }

            boolean result = importService.import_data(employeeFile, structureFile, salaryFile, session);
            
            if (result) {
                model.addAttribute("success", List.of(
                    "Import réussi",
                    "Les employés ont été importés avec succès",
                    "Les structures salariales ont été importées avec succès",
                    "Les attributions de salaires ont été importées avec succès"
                ));
            } else {
                model.addAttribute("errors", Map.of(
                    "message", List.of("Erreur lors de l'import - Veuillez vérifier vos fichiers et réessayer")
                ));
            }
            
        } catch (Exception e) {
            model.addAttribute("errors", Map.of(
                "message", List.of("Erreur: " + e.getMessage())
            ));
        }
        
        return "import";
    }

    /**
     * Import des employés seulement
     */
    @PostMapping("/employees")
    public ResponseEntity<Map<String, Object>> importEmployees(
            @RequestParam("employee_file") MultipartFile employeeFile,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (employeeFile.isEmpty()) {
                response.put("success", false);
                response.put("message", "Fichier employé requis");
                return ResponseEntity.badRequest().body(response);
            }

            if (session.getAttribute("sid") == null) {
                response.put("success", false);
                response.put("message", "Session invalide");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            boolean result = importService.import_employees(employeeFile, session);
            
            if (result) {
                response.put("success", true);
                response.put("message", "Import des employés réussi");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Erreur lors de l'import des employés");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Import des structures de salaire seulement
     */
    @PostMapping("/salary-structures")
    public ResponseEntity<Map<String, Object>> importSalaryStructures(
            @RequestParam("structure_file") MultipartFile structureFile,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (structureFile.isEmpty()) {
                response.put("success", false);
                response.put("message", "Fichier structure requis");
                return ResponseEntity.badRequest().body(response);
            }

            if (session.getAttribute("sid") == null) {
                response.put("success", false);
                response.put("message", "Session invalide");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            boolean result = importService.import_salary_structures(structureFile, session);
            
            if (result) {
                response.put("success", true);
                response.put("message", "Import des structures de salaire réussi");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Erreur lors de l'import des structures");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Import des affectations de salaire seulement
     */
    @PostMapping("/salary-assignments")
    public ResponseEntity<Map<String, Object>> importSalaryAssignments(
            @RequestParam("salary_file") MultipartFile salaryFile,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (salaryFile.isEmpty()) {
                response.put("success", false);
                response.put("message", "Fichier affectation requis");
                return ResponseEntity.badRequest().body(response);
            }

            if (session.getAttribute("sid") == null) {
                response.put("success", false);
                response.put("message", "Session invalide");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            boolean result = importService.import_salary_assignments(salaryFile, session);
            
            if (result) {
                response.put("success", true);
                response.put("message", "Import des affectations de salaire réussi");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Erreur lors de l'import des affectations");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint pour vérifier le statut du service
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "active");
        response.put("service", "Import Service");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint pour obtenir les formats de fichiers attendus
     */
    @GetMapping("/formats")
    public ResponseEntity<Map<String, Object>> getFileFormats() {
        Map<String, Object> response = new HashMap<>();
        
        Map<String, String> employeeFormat = new HashMap<>();
        employeeFormat.put("description", "Format CSV pour les employés");
        employeeFormat.put("headers", "ref,firstName,lastName,middleName,gender,hireDate,company,department,position,email,phone,address,status,contractType");
        employeeFormat.put("dateFormat", "dd/MM/yyyy");
        
        Map<String, String> structureFormat = new HashMap<>();
        structureFormat.put("description", "Format CSV pour les structures de salaire");
        structureFormat.put("headers", "structureName,salary_component,salary_component_abbr,type,formula,company");
        
        Map<String, String> assignmentFormat = new HashMap<>();
        assignmentFormat.put("description", "Format CSV pour les affectations de salaire");
        assignmentFormat.put("headers", "from_date,employee_ref,base,salary_structure");
        assignmentFormat.put("dateFormat", "dd/MM/yyyy");
        
        response.put("employee_format", employeeFormat);
        response.put("structure_format", structureFormat);
        response.put("assignment_format", assignmentFormat);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint pour import avec validation préalable
     */
    @PostMapping("/validate-and-import")
    public ResponseEntity<Map<String, Object>> validateAndImport(
            @RequestParam("employee_file") MultipartFile employeeFile,
            @RequestParam("structure_file") MultipartFile structureFile,
            @RequestParam("salary_file") MultipartFile salaryFile,
            @RequestParam(value = "validate_only", defaultValue = "false") boolean validateOnly,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validation de base
            if (employeeFile.isEmpty() || structureFile.isEmpty() || salaryFile.isEmpty()) {
                response.put("success", false);
                response.put("message", "Tous les fichiers sont requis");
                return ResponseEntity.badRequest().body(response);
            }

            // Validation des types de fichiers
            if (!isValidCSVFile(employeeFile) || !isValidCSVFile(structureFile) || !isValidCSVFile(salaryFile)) {
                response.put("success", false);
                response.put("message", "Seuls les fichiers CSV sont acceptés");
                return ResponseEntity.badRequest().body(response);
            }

            if (session.getAttribute("sid") == null) {
                response.put("success", false);
                response.put("message", "Session invalide");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Si seulement validation
            if (validateOnly) {
                response.put("success", true);
                response.put("message", "Validation réussie - fichiers prêts pour l'import");
                response.put("validation_only", true);
                return ResponseEntity.ok(response);
            }

            // Procéder à l'import
            boolean result = importService.import_data(employeeFile, structureFile, salaryFile, session);
            
            if (result) {
                response.put("success", true);
                response.put("message", "Validation et import réussis");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Validation réussie mais erreur lors de l'import");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Méthode utilitaire pour valider le type de fichier CSV
     */
    private boolean isValidCSVFile(MultipartFile file) {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        
        return (contentType != null && (contentType.equals("text/csv") || contentType.equals("application/csv"))) ||
               (fileName != null && fileName.toLowerCase().endsWith(".csv"));
    }
}