package com.newapp.Erpnext.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.newapp.Erpnext.services.ImportService;
import com.newapp.Erpnext.services.SessionService;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/import")
public class ImportController {

    private static final Logger logger = LoggerFactory.getLogger(ImportController.class);
    private final ImportService importService;
    private final SessionService sessionService;
    private final RestTemplate restTemplate;
    
    @Value("${erpnext.url}")
    private String erpnextUrl;
    
    @Value("${erpnext.api.key}")
    private String apiKey;
    
    @Value("${erpnext.api.secret}")
    private String apiSecret;

    @Autowired
    public ImportController(ImportService importService, SessionService sessionService, RestTemplate restTemplate) {
        this.importService = importService;
        this.sessionService = sessionService;
        this.restTemplate = restTemplate;
    }

    @GetMapping
    public String importPage(Model model) {
        if (!sessionService.isAuthenticated()) {
            return "redirect:/login";
        }
        return "import";
    }

    @PostMapping("/upload")
    public String uploadFiles(
            @RequestParam("employeeFile") MultipartFile employeeFile,
            @RequestParam("salaryStructureFile") MultipartFile salaryStructureFile,
            @RequestParam("salaryAssignmentFile") MultipartFile salaryAssignmentFile,
            RedirectAttributes redirectAttributes) {

        if (!sessionService.isAuthenticated()) {
            return "redirect:/login";
        }

        Map<String, Object> response = new HashMap<>();
        String employeeFilePath = null;
        String salaryStructureFilePath = null;
        String salaryAssignmentFilePath = null;

        try {
            logger.info("Début de l'upload des fichiers");

            // Validation des fichiers
            if (!validateFiles(employeeFile, salaryStructureFile, salaryAssignmentFile, response)) {
                redirectAttributes.addFlashAttribute("errors", response);
                return "redirect:/import";
            }

            // Stockage des fichiers avec gestion des erreurs
            try {
                employeeFilePath = importService.storeFile(employeeFile, "employees");
                salaryStructureFilePath = importService.storeFile(salaryStructureFile, "salary_structure");
                salaryAssignmentFilePath = importService.storeFile(salaryAssignmentFile, "salary_assignment");
            } catch (IOException e) {
                logger.error("Erreur lors du stockage des fichiers", e);
                response.put("success", false);
                response.put("message", "Erreur lors du stockage des fichiers: " + e.getMessage());
                redirectAttributes.addFlashAttribute("errors", response);
                return "redirect:/import";
            }
            
            logger.info("Fichiers stockés avec succès");
            
            // Validation détaillée
            Map<String, Object> validationResults = performValidation(employeeFilePath, 
                salaryStructureFilePath, salaryAssignmentFilePath);
            
            if (!validationResults.isEmpty()) {
                logger.warn("Erreurs de validation détectées");
                response.put("success", false);
                response.put("validation_errors", validationResults);
                redirectAttributes.addFlashAttribute("errors", validationResults);
                cleanupFiles(employeeFilePath, salaryStructureFilePath, salaryAssignmentFilePath);
                return "redirect:/import";
            }

            logger.info("Validation réussie, début de l'import vers ERPNext");

            // Envoi à ERPNext
            Map<String, Object> importResults = importService.importToErpnext(
                employeeFilePath, salaryStructureFilePath, salaryAssignmentFilePath);
            
            if (!(Boolean) importResults.get("success")) {
                logger.error("Échec de l'import vers ERPNext");
                response.put("success", false);
                response.put("errors", importResults.get("errors"));
                redirectAttributes.addFlashAttribute("errors", response);
                cleanupFiles(employeeFilePath, salaryStructureFilePath, salaryAssignmentFilePath);
                return "redirect:/import";
            }

            logger.info("Import terminé avec succès");
            response.putAll(importResults);
            redirectAttributes.addFlashAttribute("success", importResults.get("messages"));
            
            // Nettoyage des fichiers temporaires
            cleanupFiles(employeeFilePath, salaryStructureFilePath, salaryAssignmentFilePath);
            
            return "redirect:/import";

        } catch (MaxUploadSizeExceededException e) {
            logger.error("Taille maximale de fichier dépassée", e);
            response.put("success", false);
            response.put("message", "La taille des fichiers dépasse la limite autorisée (10MB par fichier)");
            redirectAttributes.addFlashAttribute("errors", response);
            return "redirect:/import";
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de l'upload", e);
            response.put("success", false);
            response.put("message", "Erreur lors du téléchargement des fichiers: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errors", response);
            cleanupFiles(employeeFilePath, salaryStructureFilePath, salaryAssignmentFilePath);
            return "redirect:/import";
        }
    }

    private void cleanupFiles(String... filePaths) {
        for (String filePath : filePaths) {
            if (filePath != null) {
                try {
                    importService.deleteFile(filePath);
                } catch (Exception e) {
                    logger.warn("Erreur lors du nettoyage du fichier: " + filePath, e);
                }
            }
        }
    }

    private boolean validateFiles(MultipartFile employeeFile, 
            MultipartFile salaryStructureFile, 
            MultipartFile salaryAssignmentFile, 
            Map<String, Object> response) {
        
        if (!validateFileFormat(employeeFile) || 
            !validateFileFormat(salaryStructureFile) || 
            !validateFileFormat(salaryAssignmentFile)) {
            response.put("success", false);
            response.put("message", "Format de fichier invalide. Seuls les fichiers CSV sont acceptés.");
            return false;
        }

        if (!validateFileSize(employeeFile) || 
            !validateFileSize(salaryStructureFile) || 
            !validateFileSize(salaryAssignmentFile)) {
            response.put("success", false);
            response.put("message", "Taille de fichier dépassée. La taille maximale est de 10MB.");
            return false;
        }

        return true;
    }

    private Map<String, Object> performValidation(String employeeFilePath, 
            String salaryStructureFilePath, 
            String salaryAssignmentFilePath) throws CsvValidationException {
        
        Map<String, Object> validationResults = new HashMap<>();
        
        // Validation du fichier employé
        Map<String, List<String>> employeeValidation = importService.validateEmployeeFile(employeeFilePath);
        if (!employeeValidation.get("errors").isEmpty()) {
            validationResults.put("employee_errors", employeeValidation.get("errors"));
        }
        if (!employeeValidation.get("warnings").isEmpty()) {
            validationResults.put("employee_warnings", employeeValidation.get("warnings"));
        }
        
        // Validation de la structure salariale
        Map<String, List<String>> structureValidation = importService.validateSalaryStructureFile(salaryStructureFilePath);
        if (!structureValidation.get("errors").isEmpty()) {
            validationResults.put("structure_errors", structureValidation.get("errors"));
        }
        if (!structureValidation.get("warnings").isEmpty()) {
            validationResults.put("structure_warnings", structureValidation.get("warnings"));
        }
        
        // Validation des attributions de salaire
        Map<String, List<String>> assignmentValidation = importService.validateSalaryAssignmentFile(salaryAssignmentFilePath);
        if (!assignmentValidation.get("errors").isEmpty()) {
            validationResults.put("assignment_errors", assignmentValidation.get("errors"));
        }
        if (!assignmentValidation.get("warnings").isEmpty()) {
            validationResults.put("assignment_warnings", assignmentValidation.get("warnings"));
        }
        
        // Vérification des données de référence
        Map<String, Object> referenceValidation = importService.validateReferenceData(employeeFilePath, 
            salaryStructureFilePath, salaryAssignmentFilePath);
        if (referenceValidation.containsKey("errors")) {
            validationResults.put("reference_errors", referenceValidation.get("errors"));
        }
        if (referenceValidation.containsKey("warnings")) {
            validationResults.put("reference_warnings", referenceValidation.get("warnings"));
        }
        
        return validationResults;
    }

    private boolean validateFileFormat(MultipartFile file) {
        if (file.isEmpty()) {
            return false;
        }
        String filename = file.getOriginalFilename();
        return filename != null && filename.toLowerCase().endsWith(".csv");
    }

    private boolean validateFileSize(MultipartFile file) {
        long maxSize = 10 * 1024 * 1024; // 10MB en octets
        return file.getSize() <= maxSize;
    }
}