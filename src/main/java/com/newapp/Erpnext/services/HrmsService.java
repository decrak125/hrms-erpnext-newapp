package com.newapp.Erpnext.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HrmsService {

    private static final Logger logger = LoggerFactory.getLogger(HrmsService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SessionService sessionService;

    @Value("${frappe.api.url}")
    private String apiUrl;

    @Value("${frappe.api.key}")
    private String apiKey;

    @Value("${frappe.api.secret}")
    private String apiSecret;

    public HrmsService(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper, SessionService sessionService) {
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
        this.sessionService = sessionService;
    }

    // Récupérer la liste des doctypes HRMS
    public List<Map<String, String>> getHrmsDoctypes() {
        List<Map<String, String>> doctypes = new ArrayList<>();
        doctypes.add(createDoctypeOption("Employee", "Employés"));
        doctypes.add(createDoctypeOption("Salary Slip", "Fiches de paie"));
        // Ajoutez d'autres doctypes HRMS si nécessaire (par exemple, Leave Application)
        return doctypes;
    }

    private Map<String, String> createDoctypeOption(String value, String display) {
        Map<String, String> option = new HashMap<>();
        option.put("value", value);
        option.put("display", display);
        return option;
    }

    // Récupérer les données d'un doctype via l'API Frappe
    public List<Map<String, Object>> getDoctypeData(String doctype, String filter1, String filter2, String filter3, String sortBy) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String sid = sessionService.ensureImportSessionValid(null);
            if (sid == null) {
                throw new RuntimeException("Session non valide");
            }
            headers.set("Authorization", "token " + apiKey + ":" + apiSecret);
            
            String url = apiUrl + "/api/resource/" + doctype + "?fields=[\"*\"]";

            // Appliquer les filtres
            List<String> filters = new ArrayList<>();
            if (filter1 != null && !filter1.isEmpty() && !"Tous".equals(filter1)) {
                filters.add("[\"" + (doctype.equals("Employee") ? "department" : "department") + "\",\"=\",\"" + filter1 + "\"]");
            }
            if (filter2 != null && !filter2.isEmpty() && !"Tous".equals(filter2)) {
                filters.add("[\"" + (doctype.equals("Employee") ? "status" : "status") + "\",\"=\",\"" + filter2 + "\"]");
            }
            if (filter3 != null && !filter3.isEmpty()) {
                filters.add("[\"" + (doctype.equals("Employee") ? "employee_name" : "employee_name") + "\",\"like\",\"%" + filter3 + "%\"]");
            }
            
            if (!filters.isEmpty()) {
                url += "&filters=[[" + String.join("],[", filters) + "]]";
            }

            // Appliquer le tri
            if (sortBy != null && !sortBy.isEmpty()) {
                String field = doctype.equals("Employee") ? "date_of_joining" : "posting_date";
                String order = sortBy.endsWith("Asc") ? "asc" : "desc";
                url += "&order_by=" + field + " " + order;
            }

            logger.debug("URL de l'API: {}", url);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            List<Map<String, Object>> records = new ArrayList<>();
            for (JsonNode node : jsonNode.get("data")) {
                Map<String, Object> record = objectMapper.convertValue(node, Map.class);
                records.add(record);
            }
            return records;
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des données de l'API: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la récupération des données de l'API: " + e.getMessage());
        }
    }
}