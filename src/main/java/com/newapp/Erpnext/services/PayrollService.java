package com.newapp.Erpnext.services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.newapp.Erpnext.models.SalarySlip;

import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PayrollService {

    private static final Logger logger = LoggerFactory.getLogger(PayrollService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SessionService sessionService;

    @Value("${erpnext.api.url:http://erpnext.localhost:8000/api/resource/Salary Slip}")
    private String erpnextApiUrl;

    @Value("${erpnext.base.url:http://erpnext.localhost:8000}")
    private String erpnextBaseUrl;

    /**
     * Génère les salary slips pour un employé entre deux dates
     * @param employeeId ID de l'employé
     * @param startDate Date de début (format YYYY-MM-DD)
     * @param endDate Date de fin (format YYYY-MM-DD)
     * @param baseSalary Salaire de base (peut être null ou 0)
     * @return Message de résultat
     */

public String generateSalarySlips(String employeeId, String startDate, String endDate, Double baseSalary) {
    try {
        // Parse dates
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        // Validate dates
        if (start.isAfter(end)) {
            return "Error: startDate must be before or equal to endDate";
        }

        // Déterminer le salaire de base à utiliser
        Double salaryToUse = determineSalaryToUse(employeeId, start, baseSalary);
        if (salaryToUse == null) {
            return "Error: No base salary provided and no previous salary slip found before " + startDate;
        }

        // Récupérer la Salary Structure Assignment si nécessaire
        Map<String, Object> salaryStructureAssignment = null;
        if (baseSalary == null || baseSalary == 0.0) {
            salaryStructureAssignment = getLastSalaryStructureAssignment(employeeId, start.minusDays(1).toString());
            if (salaryStructureAssignment == null) {
                logger.warn("No Salary Structure Assignment found for employee {} before {}", employeeId, startDate);
            }
        }

        // Initialiser la génération par mois - CORRECTION ICI
        YearMonth currentMonth = YearMonth.from(start);
        YearMonth endMonth = YearMonth.from(end);
        int createdSlips = 0;
        int skippedSlips = 0;

        // Générer les salary slips mois par mois
        while (!currentMonth.isAfter(endMonth)) {
            // CORRECTION: Calculer les dates de début et fin pour chaque mois
            LocalDate monthStartDate = currentMonth.atDay(1);
            LocalDate monthEndDate = currentMonth.atEndOfMonth();
            
            // Ajuster les dates selon la période demandée
            if (monthStartDate.isBefore(start)) {
                monthStartDate = start;
            }
            if (monthEndDate.isAfter(end)) {
                monthEndDate = end;
            }
            
            String monthStart = monthStartDate.toString();
            String monthEnd = monthEndDate.toString();

            // Vérifier si un salary slip existe déjà pour ce mois
            boolean slipExists = checkSalary(employeeId, monthStart, monthEnd);
            if (!slipExists) {
                // Créer un nouveau salary slip pour ce mois spécifique
                String result = createSalarySlipWithBaseSalary(employeeId, monthStart, monthEnd, salaryToUse, salaryStructureAssignment);
                if (result.startsWith("Error")) {
                    return result;
                }
                createdSlips++;
                logger.info("Created salary slip for employee {} for period {} to {}", employeeId, monthStart, monthEnd);
            } else {
                skippedSlips++;
                logger.info("Salary slip already exists for employee {} for period {} to {}", employeeId, monthStart, monthEnd);
            }
            
            
            currentMonth = currentMonth.plusMonths(1); // Passer au mois suivant
        }

        return String.format("Successfully created %d salary slip(s). Skipped %d existing slip(s).",
                            createdSlips, skippedSlips);
    } catch (Exception e) {
        logger.error("Error generating salary slips: {}", e.getMessage());
        return "Error generating salary slips: " + e.getMessage();
    }
}

    /**
     * Détermine le salaire de base à utiliser selon la logique métier
     */
    private Double determineSalaryToUse(String employeeId, LocalDate startDate, Double baseSalary) {
        // Si un salaire de base est fourni et différent de 0, l'utiliser
        if (baseSalary != null && baseSalary > 0) {
            return baseSalary;
        }

        // Sinon, chercher le dernier salary slip avant la date de début
        SalarySlip lastSalarySlip = getLastSalarySlip(employeeId, startDate.minusDays(1).toString());
        if (lastSalarySlip != null && (Double)lastSalarySlip.getGrossPay() != null) {
            return lastSalarySlip.getGrossPay();
        }

        return null; // Aucun salaire trouvé
    }

    /**
     * Récupère le dernier Salary Structure Assignment avant une date donnée
     */
    private Map<String, Object> getLastSalaryStructureAssignment(String employeeId, String beforeDate) {
        HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
        String sid = sessionService.ensureImportSessionValid(session);

        if (sid != null) {
            HttpHeaders headers = createHeaders(sid);
            
            String url = erpnextBaseUrl + "/api/resource/Salary Structure Assignment" +
                        "?filters=[[\"employee\",\"=\",\"" + employeeId + "\"],[\"from_date\",\"<\",\"" + beforeDate + "\"]]" +
                        "&order_by=from_date desc&limit_page_length=1";
            
            try {
                ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Object data = response.getBody().get("data");
                    List<Map<String, Object>> assignments = safeCastToList(data);
                    
                    if (!assignments.isEmpty()) {
                        return assignments.get(0);
                    }
                }
            } catch (Exception e) {
                logger.error("Error fetching last salary structure assignment for employee {}: {}", employeeId, e.getMessage());
            }
        }
        return null;
    }

    /**
     * Récupère la liste des employés actifs
     */
    public List<Map<String, Object>> getActiveEmployees() {
        HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
        String sid = sessionService.ensureImportSessionValid(session);

        if (sid != null) {
            HttpHeaders headers = createHeaders(sid);
            String url = erpnextBaseUrl + "/api/resource/Employee" +
                        "?fields=[\"name\",\"employee_name\",\"designation\"]" +
                        "&filters=[[\"status\",\"=\",\"Active\"]]" +
                        "&order_by=employee_name asc";

            try {
                ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Object data = response.getBody().get("data");
                    return safeCastToList(data);
                }
            } catch (Exception e) {
                logger.error("Error fetching active employees: {}", e.getMessage());
            }
        }
        return Collections.emptyList();
    }

    private SalarySlip getLastSalarySlip(String employeeId, String beforeDate) {
        HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
        String sid = sessionService.ensureImportSessionValid(session);

        if (sid != null) {
            HttpHeaders headers = createHeaders(sid);
            
            String url = erpnextApiUrl + "?filters=[[\"employee\",\"=\",\"" + employeeId + "\"],[\"posting_date\",\"<\",\"" + beforeDate + "\"]]"
                       + "&order_by=posting_date desc&limit_page_length=1";
            
            try {
                ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Object data = response.getBody().get("data");
                    List<Map<String, Object>> salarySlips = safeCastToList(data);
                    
                    if (!salarySlips.isEmpty()) {
                        return convertToSalarySlip(salarySlips.get(0));
                    }
                }
            } catch (Exception e) {
                logger.error("Error fetching last salary slip for employee {}: {}", employeeId, e.getMessage());
            }
        }
        return null;
    }

public boolean checkSalary(String employeeId, String startDate, String endDate) {
    HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
    String sid = sessionService.ensureImportSessionValid(session);

    if (sid == null) {
        logger.error("Invalid session for employee {}, cannot check salary slips for period {}-{}", 
                     employeeId, startDate, endDate);
        return false;
    }

    HttpHeaders headers = createHeaders(sid);
    
    // CORRECTION: Utiliser des filtres de chevauchement au lieu d'égalité exacte
    // Chercher les salary slips qui chevauchent avec la période donnée
    String url = erpnextApiUrl + "?fields=[\"name\",\"start_date\",\"end_date\"]&filters=[[\"employee\",\"=\",\"" + employeeId + "\"]," +
               "[\"start_date\",\"<=\",\"" + endDate + "\"],[\"end_date\",\">=\",\"" + startDate + "\"]]" +
               "&limit_page_length=10";

    try {
        logger.info("Checking salary slips for employee {} for period {}-{} with URL: {}", 
                    employeeId, startDate, endDate, url);
        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class
        );

        if (response.getBody() != null) {
            Object data = response.getBody().get("data");
            List<?> salarySlips = safeCastToList(data);
            
            if (!salarySlips.isEmpty()) {
                // Log des salary slips trouvés pour debugging
                logger.info("Found {} overlapping salary slips for employee {} for period {}-{}:", 
                            salarySlips.size(), employeeId, startDate, endDate);
                for (Object slip : salarySlips) {
                    if (slip instanceof Map) {
                        Map<?, ?> slipMap = (Map<?, ?>) slip;
                        logger.info("  - Slip: {}, Period: {} to {}", 
                                   slipMap.get("name"), 
                                   slipMap.get("start_date"), 
                                   slipMap.get("end_date"));
                    }
                }
                return true;
            } else {
                logger.info("No overlapping salary slips found for employee {} for period {}-{}", 
                            employeeId, startDate, endDate);
                return false;
            }
        } else {
            logger.warn("No data in response for employee {} for period {}-{}", 
                        employeeId, startDate, endDate);
        }
    } catch (Exception e) {
        logger.error("Error checking salary for employee {} for period {}-{}: {}", 
                     employeeId, startDate, endDate, e.getMessage());
    }
    return false;
}

// Version alternative plus stricte si vous voulez vérifier les mois spécifiquement
public boolean checkSalaryForMonth(String employeeId, String startDate, String endDate) {
    HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
    String sid = sessionService.ensureImportSessionValid(session);

    if (sid == null) {
        logger.error("Invalid session for employee {}, cannot check salary slips for period {}-{}", 
                     employeeId, startDate, endDate);
        return false;
    }

    HttpHeaders headers = createHeaders(sid);
    
    try {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        YearMonth targetMonth = YearMonth.from(start);
        
        // Chercher tous les salary slips de l'employé pour ce mois
        String url = erpnextApiUrl + "?fields=[\"name\",\"start_date\",\"end_date\"]&filters=[[\"employee\",\"=\",\"" + employeeId + "\"]," +
                   "[\"start_date\",\">=\",\"" + targetMonth.atDay(1) + "\"]," +
                   "[\"start_date\",\"<=\",\"" + targetMonth.atEndOfMonth() + "\"]]" +
                   "&limit_page_length=10";

        logger.info("Checking salary slips for employee {} for month {} with URL: {}", 
                    employeeId, targetMonth, url);
        
        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class
        );

        if (response.getBody() != null) {
            Object data = response.getBody().get("data");
            List<?> salarySlips = safeCastToList(data);
            
            if (!salarySlips.isEmpty()) {
                logger.info("Found {} salary slips for employee {} for month {}:", 
                            salarySlips.size(), employeeId, targetMonth);
                for (Object slip : salarySlips) {
                    if (slip instanceof Map) {
                        Map<?, ?> slipMap = (Map<?, ?>) slip;
                        logger.info("  - Slip: {}, Period: {} to {}", 
                                   slipMap.get("name"), 
                                   slipMap.get("start_date"), 
                                   slipMap.get("end_date"));
                    }
                }
                return true;
            } else {
                logger.info("No salary slips found for employee {} for month {}", 
                            employeeId, targetMonth);
                return false;
            }
        } else {
            logger.warn("No data in response for employee {} for month {}", employeeId, targetMonth);
        }
    } catch (Exception e) {
        logger.error("Error checking salary for employee {} for month: {}", employeeId, e.getMessage());
    }
    return false;
}

public int countSalary(String employeeId, String startDate, String endDate) {
    HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
    String sid = sessionService.ensureImportSessionValid(session);

    if (sid != null) {
        HttpHeaders headers = createHeaders(sid);
        
        // CORRECTION: Même logique que checkSalary avec chevauchement
        String url = erpnextApiUrl + "?fields=[\"name\"]&filters=[[\"employee\",\"=\",\"" + employeeId + "\"]," +
                   "[\"start_date\",\"<=\",\"" + endDate + "\"],[\"end_date\",\">=\",\"" + startDate + "\"]]";

        try {
            logger.info("Counting salary slips for employee {} for period {}-{} with URL: {}", 
                        employeeId, startDate, endDate, url);
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );

            if (response.getBody() != null) {
                Object data = response.getBody().get("data");
                List<?> salarySlips = safeCastToList(data);
                logger.info("Found {} salary slips for employee {} for period {}-{}", 
                            salarySlips.size(), employeeId, startDate, endDate);
                return salarySlips.size();
            }
        } catch (Exception e) {
            logger.error("Error counting salary slips for employee {} for period {}-{}: {}", 
                         employeeId, startDate, endDate, e.getMessage());
        }
    }
    return 0;
}

    private String createSalarySlipWithBaseSalary(String employeeId, String startDate, String endDate,
                                              Double baseSalary, Map<String, Object> salaryStructureAssignment) {
    HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
    String sid = sessionService.ensureImportSessionValid(session);
             
    if (sid != null) {
        HttpHeaders headers = createHeaders(sid);
        String url = erpnextApiUrl;

        // Préparer le payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("doctype", "Salary Slip");
        payload.put("employee", employeeId);
        payload.put("start_date", startDate);
        payload.put("end_date", endDate);
        payload.put("posting_date", LocalDate.now().toString());
        payload.put("payroll_frequency", "Monthly");
        payload.put("gross_pay", baseSalary);
        payload.put("net_pay", baseSalary); // Peut être ajusté selon la logique métier
        payload.put("docstatus", "1");

        // Récupérer les détails de l'employé
        Map<String, Object> employeeDetails = getEmployeeDetails(employeeId);
        if (employeeDetails != null) {
            payload.put("company", employeeDetails.getOrDefault("company", "Your Company"));
            payload.put("salary_structure", employeeDetails.getOrDefault("salary_structure", "g1"));
        } else {
            return "Error: Unable to fetch employee details";
        }

        // Ajouter les informations du Salary Structure Assignment si disponible
        if (salaryStructureAssignment != null) {
            payload.put("salary_structure", salaryStructureAssignment.getOrDefault("salary_structure", "g1"));
            payload.put("base", salaryStructureAssignment.getOrDefault("base", baseSalary));
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && responseBody.containsKey("data")) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                    String slipName = (String) data.get("name");
                    logger.info("Successfully created salary slip: {} for employee: {} for period: {} to {}", 
                               slipName, employeeId, startDate, endDate);
                    return slipName;
                }
            }
            return "Error: Failed to create salary slip - " + response.getStatusCode();
        } catch (Exception e) {
            logger.error("Error creating salary slip for employee {} for period {} to {}: {}", 
                        employeeId, startDate, endDate, e.getMessage());
            return "Error: Failed to create salary slip - " + e.getMessage();
        }
    }
    return "Error: Invalid session or unable to create salary slip";
}
    // Méthodes utilitaires existantes

    private HttpHeaders createHeaders(String sid) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", "sid=" + sid);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> safeCastToList(Object data) {
        if (data instanceof List) {
            return (List<Map<String, Object>>) data;
        } else if (data instanceof Map) {
            return Collections.singletonList((Map<String, Object>) data);
        }
        return Collections.emptyList();
    }

    private SalarySlip convertToSalarySlip(Map<String, Object> map) {
        SalarySlip slip = new SalarySlip();
        slip.setName((String) map.get("name"));
        slip.setGrossPay(Double.parseDouble(map.getOrDefault("gross_pay", "0.0").toString()));
        slip.setNetPay(Double.parseDouble(map.getOrDefault("net_pay", "0.0").toString()));
        return slip;
    }

    private Map<String, Object> getEmployeeDetails(String employeeId) {
        HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
        String sid = sessionService.ensureImportSessionValid(session);
        
        if (sid != null) {
            HttpHeaders headers = createHeaders(sid);
            String encodedEmployeeId = URLEncoder.encode(employeeId, StandardCharsets.UTF_8);
            String url = erpnextBaseUrl + "/api/resource/Employee/" + encodedEmployeeId + 
                        "?fields=[\"company\",\"salary_structure\"]";

            try {
                ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    Map<String, Object> responseBody = response.getBody();
                    if (responseBody != null && responseBody.containsKey("data")) {
                        return (Map<String, Object>) responseBody.get("data");
                    }
                }
            } catch (Exception e) {
                logger.error("Error fetching employee details: {}", e.getMessage());
            }
        }
        return null;
    }

    // Méthode de compatibilité avec l'ancien code
    @Deprecated
    public String generateSalarySlips(String employeeId, String startDate, String endDate) {
        return generateSalarySlips(employeeId, startDate, endDate, null);
    }
}