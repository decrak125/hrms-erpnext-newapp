package com.newapp.Erpnext.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.newapp.Erpnext.services.ImportService;
import com.newapp.Erpnext.services.SessionService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/import")
public class ImportController {

    private static final Logger logger = LoggerFactory.getLogger(ImportController.class);
    private final ImportService importService;
    private final SessionService sessionService;

    @Value("${erpnext.url}")
    private String erpnextUrl;
    
    @Value("${erpnext.api.key}")
    private String apiKey;
    
    @Value("${erpnext.api.secret}")
    private String apiSecret;

    @Autowired
    public ImportController(ImportService importService, SessionService sessionService) {
        this.importService = importService;
        this.sessionService = sessionService;
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
            Map<String, Object> validationResults = importService.validateFiles(
                employeeFilePath, salaryStructureFilePath, salaryAssignmentFilePath);
            
            if (!validationResults.isEmpty()) {
                logger.warn("Erreurs de validation détectées");
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
                redirectAttributes.addFlashAttribute("errors", importResults);
                cleanupFiles(employeeFilePath, salaryStructureFilePath, salaryAssignmentFilePath);
                return "redirect:/import";
            }

            logger.info("Import terminé avec succès");
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

    private boolean validateFiles(MultipartFile employeeFile, 
            MultipartFile salaryStructureFile, 
            MultipartFile salaryAssignmentFile, 
            Map<String, Object> response) {
        
        if (employeeFile.isEmpty() || salaryStructureFile.isEmpty() || salaryAssignmentFile.isEmpty()) {
            response.put("success", false);
            response.put("message", "Tous les fichiers sont requis");
            return false;
        }

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

    private boolean validateFileFormat(MultipartFile file) {
        String filename = file.getOriginalFilename();
        return filename != null && filename.toLowerCase().endsWith(".csv");
    }

    private boolean validateFileSize(MultipartFile file) {
        long maxSize = 10 * 1024 * 1024; // 10MB en octets
        return file.getSize() <= maxSize;
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
}