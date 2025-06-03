package com.newapp.Erpnext.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.newapp.Erpnext.services.ImportService;
import com.newapp.Erpnext.services.SessionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/import")
public class ImportController {

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
    public ResponseEntity<?> uploadFiles(
            @RequestParam("employeeFile") MultipartFile employeeFile,
            @RequestParam("salaryStructureFile") MultipartFile salaryStructureFile,
            @RequestParam("salaryAssignmentFile") MultipartFile salaryAssignmentFile) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Validation du format des fichiers
            if (!validateFileFormat(employeeFile) || 
                !validateFileFormat(salaryStructureFile) || 
                !validateFileFormat(salaryAssignmentFile)) {
                response.put("success", false);
                response.put("message", "Format de fichier invalide. Seuls les fichiers CSV sont acceptés.");
                return ResponseEntity.badRequest().body(response);
            }

            // Validation de la taille des fichiers
            if (!validateFileSize(employeeFile) || 
                !validateFileSize(salaryStructureFile) || 
                !validateFileSize(salaryAssignmentFile)) {
                response.put("success", false);
                response.put("message", "Taille de fichier dépassée. La taille maximale est de 10MB.");
                return ResponseEntity.badRequest().body(response);
            }

            // Stockage temporaire des fichiers
            String employeeFilePath = importService.storeFile(employeeFile, "employees");
            String salaryStructureFilePath = importService.storeFile(salaryStructureFile, "salary_structure");
            String salaryAssignmentFilePath = importService.storeFile(salaryAssignmentFile, "salary_assignment");
            
            // Envoyer les fichiers à l'API ERPNext HRMS
            String uploadUrl = erpnextUrl + "/api/method/hrms.api.import_upload";
            
            // Préparer les headers avec authentification
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Authorization", "token " + apiKey + ":" + apiSecret);
            
            // Préparer le corps de la requête
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("employee_file", importService.getFileResource(employeeFilePath));
            body.add("salary_structure_file", importService.getFileResource(salaryStructureFilePath));
            body.add("salary_assignment_file", importService.getFileResource(salaryAssignmentFilePath));
            
            // Créer l'entité HTTP
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // Envoyer la requête à ERPNext
            ResponseEntity<Map> erpnextResponse = restTemplate.postForEntity(uploadUrl, requestEntity, Map.class);
            
            // Traiter la réponse d'ERPNext
            if (erpnextResponse.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> erpnextData = erpnextResponse.getBody();
                response.put("success", true);
                response.put("erpnextResponse", erpnextData);
                response.put("message", "Fichiers téléchargés avec succès vers ERPNext.");
            } else {
                response.put("success", false);
                response.put("message", "Erreur lors du téléchargement des fichiers vers ERPNext.");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erreur lors du téléchargement des fichiers: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateFiles(
            @RequestParam("employeeFile") String employeeFilePath,
            @RequestParam("salaryStructureFile") String salaryStructureFilePath,
            @RequestParam("salaryAssignmentFile") String salaryAssignmentFilePath) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            // Validation des colonnes obligatoires
            Map<String, List<String>> employeeValidation = importService.validateEmployeeFile(employeeFilePath);
            Map<String, List<String>> structureValidation = importService.validateSalaryStructureFile(salaryStructureFilePath);
            Map<String, List<String>> assignmentValidation = importService.validateSalaryAssignmentFile(salaryAssignmentFilePath);
            
            // Vérification des données de référence
            Map<String, Object> referenceValidation = importService.validateReferenceData(employeeFilePath, 
                salaryStructureFilePath, salaryAssignmentFilePath);
            
            response.put("success", true);
            response.put("employeeValidation", employeeValidation);
            response.put("structureValidation", structureValidation);
            response.put("assignmentValidation", assignmentValidation);
            response.put("referenceValidation", referenceValidation);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erreur lors de la validation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private boolean validateFileFormat(MultipartFile file) {
        if (file.isEmpty()) {
            return false;
        }
        String filename = file.getOriginalFilename();
        return filename != null && filename.toLowerCase().endsWith(".csv");
    }

    private boolean validateFileSize(MultipartFile file) {
        // 10MB en octets
        long maxSize = 10 * 1024 * 1024;
        return file.getSize() <= maxSize;
    }
}