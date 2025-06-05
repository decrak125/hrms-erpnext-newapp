package com.newapp.Erpnext.controller;
import com.newapp.Erpnext.services.ImportService;
import com.newapp.Erpnext.services.ImportService.ImportException;
import com.newapp.Erpnext.services.SessionService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Controller
@RequestMapping("/import")
public class ImportController {
    private static final Logger logger = LoggerFactory.getLogger(ImportController.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Autowired
    private ImportService importService;

    @Autowired
    private SessionService sessionService;

    @GetMapping
    public String showImportPage(Model model) {
        model.addAttribute("maxFileSize", MAX_FILE_SIZE / (1024 * 1024) + "MB");
        return "import";
    }

    @GetMapping("/formats")
    public String showFormats(Model model) {
        model.addAttribute("employeeFormat", 
            "Ref,Nom,Prenom,Genre,Date embauche,Date naissance,Société");
        model.addAttribute("structureFormat", 
            "Nom structure,Composant,Abréviation,Type,Formule,Société");
        model.addAttribute("salaryFormat", 
            "Date effet,Ref employé,Base,Structure salariale,Société");
        return "formats";
    }

    @PostMapping("/api/complete")
    public String handleFileUpload(
            @RequestParam("employee_file") MultipartFile employeeFile,
            @RequestParam("structure_file") MultipartFile structureFile,
            @RequestParam("salary_file") MultipartFile salaryFile,
            HttpSession session,
            Model model
    ) {
        ImportErrors errors = new ImportErrors();
        logger.info("Début de la procédure d'importation");

        try {
            // 1. Vérification de la session principale
            if (!sessionService.isAuthenticated()) {
                logger.error("L'utilisateur n'est pas authentifié");
                errors.addMessage("Veuillez vous connecter pour effectuer une importation.");
                model.addAttribute("errors", errors);
                return "redirect:/login";
            }

            // 2. Vérification/création de la session d'importation
            String sid = sessionService.getOrCreateImportSid(session);
            if (sid == null) {
                logger.error("Impossible de créer ou récupérer une session ERPNext");
                errors.addMessage("Erreur de connexion avec ERPNext. Veuillez réessayer.");
                model.addAttribute("errors", errors);
                return "import";
            }
            logger.info("Session ERPNext obtenue : {}", sid);

            // 3. Validation des fichiers
            if (!validateFiles(employeeFile, structureFile, salaryFile, errors)) {
                logger.error("Validation des fichiers échouée");
                model.addAttribute("errors", errors);
                return "import";
            }
            logger.info("Validation des fichiers réussie");

            // 4. Tentative d'importation avec gestion de session
            try {
                if (!sessionService.hasValidImportSession(session)) {
                    logger.warn("Session d'importation invalide, tentative de renouvellement");
                    sid = sessionService.ensureImportSessionValid(session);
                    if (sid == null) {
                        throw new ImportException("Session invalide et impossible à renouveler");
                    }
                }

                boolean success = importService.import_data(employeeFile, structureFile, salaryFile, session);
                
                if (success) {
                    logger.info("Importation réussie");
                    model.addAttribute("success", Arrays.asList(
                        "Importation réussie !",
                        "Les employés ont été importés avec succès",
                        "Les structures salariales ont été importées avec succès",
                        "Les attributions de salaire ont été importées avec succès"
                    ));
                } else {
                    logger.error("Échec de l'importation");
                    errors.addMessage("L'importation a échoué. Veuillez vérifier les fichiers et réessayer.");
                    model.addAttribute("errors", errors);
                }
            } catch (ImportException e) {
                logger.error("Erreur lors de l'importation", e);
                // Si l'erreur est liée à la session, on la renouvelle
                if (e.getMessage().contains("Session")) {
                    sessionService.invalidateImportSid(session);
                    errors.addMessage("Session expirée. Veuillez réessayer l'importation.");
                } else {
                    errors.addMessage("Erreur lors de l'importation : " + e.getMessage());
                }
                model.addAttribute("errors", errors);
            }
            
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de l'importation", e);
            errors.addMessage("Une erreur inattendue s'est produite : " + e.getMessage());
            model.addAttribute("errors", errors);
        }

        return "import";
    }

    // New endpoint for resetting HR data
    @PostMapping("/api/reset")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleReset(HttpSession session) {
        logger.info("Début de la procédure de réinitialisation RH");
        Map<String, Object> response = new HashMap<>();

        try {
            // Verify session
            if (!sessionService.isAuthenticated()) {
                logger.error("L'utilisateur n'est pas authentifié");
                response.put("message", "Veuillez vous connecter pour réinitialiser les données RH.");
                return ResponseEntity.status(401).body(response);
            }

            // Get or create import session
            String sid = sessionService.getOrCreateImportSid(session);
            if (sid == null) {
                logger.error("Impossible de créer ou récupérer une session ERPNext");
                response.put("message", "Erreur de connexion avec ERPNext. Veuillez réessayer.");
                return ResponseEntity.status(500).body(response);
            }

            // Call reset_rh via ImportService
            boolean success = importService.reset_rh(session);
            if (success) {
                logger.info("Réinitialisation RH réussie");
                response.put("message", "Données RH réinitialisées avec succès.");
                return ResponseEntity.ok(response);
            } else {
                logger.error("Échec de la réinitialisation RH");
                response.put("message", "Échec de la réinitialisation des données RH.");
                return ResponseEntity.status(500).body(response);
            }
        } catch (ImportException e) {
            logger.error("Erreur lors de la réinitialisation RH", e);
            response.put("message", "Erreur lors de la réinitialisation : " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de la réinitialisation RH", e);
            response.put("message", "Une erreur inattendue s'est produite : " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private boolean validateFiles(
            MultipartFile employeeFile,
            MultipartFile structureFile,
            MultipartFile salaryFile,
            ImportErrors errors) {
        
        logger.debug("Début de la validation des fichiers");
        boolean isValid = true;

        // Vérification des fichiers manquants
        if (employeeFile == null || employeeFile.isEmpty()) {
            errors.addMessage("Le fichier des employés est requis");
            logger.error("Fichier employés manquant ou vide");
            isValid = false;
        }
        if (structureFile == null || structureFile.isEmpty()) {
            errors.addMessage("Le fichier des structures salariales est requis");
            logger.error("Fichier structures salariales manquant ou vide");
            isValid = false;
        }
        if (salaryFile == null || salaryFile.isEmpty()) {
            errors.addMessage("Le fichier des attributions de salaire est requis");
            logger.error("Fichier attributions de salaire manquant ou vide");
            isValid = false;
        }

        if (!isValid) {
            logger.error("Validation des fichiers échouée : fichiers manquants");
            return false;
        }

        // Validation du type de fichier
        if (!isCsvFile(employeeFile)) {
            errors.addEmployeeError("Le fichier des employés doit être au format CSV");
            logger.error("Format invalide pour le fichier employés : {}", employeeFile.getContentType());
            isValid = false;
        }
        if (!isCsvFile(structureFile)) {
            errors.addStructureError("Le fichier des structures salariales doit être au format CSV");
            logger.error("Format invalide pour le fichier structures : {}", structureFile.getContentType());
            isValid = false;
        }
        if (!isCsvFile(salaryFile)) {
            errors.addAssignmentError("Le fichier des attributions de salaire doit être au format CSV");
            logger.error("Format invalide pour le fichier attributions : {}", salaryFile.getContentType());
            isValid = false;
        }

        // Validation de la taille des fichiers
        if (employeeFile.getSize() > MAX_FILE_SIZE) {
            errors.addEmployeeError("Le fichier des employés ne doit pas dépasser " + MAX_FILE_SIZE / (1024 * 1024) + "MB");
            logger.error("Fichier employés trop volumineux : {} bytes", employeeFile.getSize());
            isValid = false;
        }
        if (structureFile.getSize() > MAX_FILE_SIZE) {
            errors.addStructureError("Le fichier des structures salariales ne doit pas dépasser " + MAX_FILE_SIZE / (1024 * 1024) + "MB");
            logger.error("Fichier structures trop volumineux : {} bytes", structureFile.getSize());
            isValid = false;
        }
        if (salaryFile.getSize() > MAX_FILE_SIZE) {
            errors.addAssignmentError("Le fichier des attributions de salaire ne doit pas dépasser " + MAX_FILE_SIZE / (1024 * 1024) + "MB");
            logger.error("Fichier attributions trop volumineux : {} bytes", salaryFile.getSize());
            isValid = false;
        }

        if (isValid) {
            logger.info("Validation des fichiers réussie");
        } else {
            logger.error("Validation des fichiers échouée");
        }

        return isValid;
    }

    private boolean isCsvFile(MultipartFile file) {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        
        return (contentType != null && (contentType.equals("text/csv") || contentType.equals("application/vnd.ms-excel"))) ||
               (fileName != null && fileName.toLowerCase().endsWith(".csv"));
    }

    public static class ImportErrors {
        private java.util.List<String> message = new java.util.ArrayList<>();
        private java.util.List<String> employee_errors = new java.util.ArrayList<>();
        private java.util.List<String> structure_errors = new java.util.ArrayList<>();
        private java.util.List<String> assignment_errors = new java.util.ArrayList<>();

        public java.util.List<String> getMessage() { return message; }
        public java.util.List<String> getEmployee_errors() { return employee_errors; }
        public java.util.List<String> getStructure_errors() { return structure_errors; }
        public java.util.List<String> getAssignment_errors() { return assignment_errors; }

        public void addMessage(String msg) { message.add(msg); }
        public void addEmployeeError(String error) { employee_errors.add(error); }
        public void addStructureError(String error) { structure_errors.add(error); }
        public void addAssignmentError(String error) { assignment_errors.add(error); }

        public boolean hasErrors() {
            return !message.isEmpty() || 
                   !employee_errors.isEmpty() || 
                   !structure_errors.isEmpty() || 
                   !assignment_errors.isEmpty();
        }
    }
}