package com.newapp.Erpnext.services;

import com.newapp.Erpnext.models.SalaryStatistics;
import com.newapp.Erpnext.models.EmployeeSalaryDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.*;
import java.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SalaryStatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(SalaryStatisticsService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SessionService sessionService;

    @Value("${erpnext.api.url:http://erpnext.localhost:8000/api}")
    private String erpnextApiUrl;

    public List<SalaryStatistics> getYearlyStatistics(int year) {
        List<SalaryStatistics> statistics = new ArrayList<>();
        
        for (int month = 1; month <= 12; month++) {
            SalaryStatistics monthStats = calculateMonthlyStatistics(year, month);
            statistics.add(monthStats);
        }
        
        return statistics;
    }

    public List<EmployeeSalaryDetail> getEmployeeSalaryDetails(int year, int month) {
        List<EmployeeSalaryDetail> employeeDetails = new ArrayList<>();

        try {
            HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
            String sid = sessionService.ensureImportSessionValid(session);

            if (sid != null) {
                String url = erpnextApiUrl + "/resource/Salary Slip";
                
                // Calculer le dernier jour du mois
                YearMonth yearMonth = YearMonth.of(year, month);
                int lastDay = yearMonth.lengthOfMonth();
                
                String filters = String.format("[[\"posting_date\",\"between\",[\"'%d-%02d-01'\",\"'%d-%02d-%02d'\"]]]",
                        year, month, year, month, lastDay);

                HttpHeaders headers = new HttpHeaders();
                headers.add("Cookie", "sid=" + sid);
                
                logger.info("Récupération des détails des salaires pour {}-{} avec filtre: {}", year, month, filters);

                ResponseEntity<Map> response = restTemplate.exchange(
                    url + "?filters=" + filters,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
                );

                if (response.getBody() != null && response.getBody().get("data") != null) {
                    List<Map<String, Object>> salarySlips = (List<Map<String, Object>>) response.getBody().get("data");
                    logger.debug("Nombre de fiches de paie trouvées: {}", salarySlips.size());
                    
                    for (Map<String, Object> slip : salarySlips) {
                        String slipUrl = erpnextApiUrl + "/resource/Salary Slip/" + slip.get("name");
                        ResponseEntity<Map> slipResponse = restTemplate.exchange(
                            slipUrl,
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            Map.class
                        );

                        if (slipResponse.getBody() != null && slipResponse.getBody().get("data") != null) {
                            Map<String, Object> slipData = (Map<String, Object>) slipResponse.getBody().get("data");
                            String employeeId = (String) slipData.get("employee");
                            String employeeName = (String) slipData.get("employee_name");
                            List<Map<String, Object>> earnings = (List<Map<String, Object>>) slipData.get("earnings");
                            List<Map<String, Object>> deductions = (List<Map<String, Object>>) slipData.get("deductions");

                            EmployeeSalaryDetail detail = new EmployeeSalaryDetail(employeeId, employeeName);
                            double totalSalary = 0.0;

                            if (earnings != null) {
                                for (Map<String, Object> earning : earnings) {
                                    String componentName = (String) earning.get("salary_component");
                                    Double amount = Double.valueOf(earning.get("amount").toString());
                                    detail.getSalaryComponentDetails().merge(componentName, amount, Double::sum);
                                    totalSalary += amount;
                                }
                            }

                            if (deductions != null) {
                                for (Map<String, Object> deduction : deductions) {
                                    String componentName = (String) deduction.get("salary_component");
                                    Double amount = Double.valueOf(deduction.get("amount").toString());
                                    detail.getSalaryComponentDetails().merge(componentName, -amount, Double::sum);
                                    totalSalary -= amount;
                                }
                            }

                            detail.setTotalSalary(totalSalary);
                            employeeDetails.add(detail);
                            logger.debug("Ajout des détails de salaire pour l'employé {} ({})", employeeName, employeeId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des détails des salaires des employés pour {}-{}: {}", year, month, e.getMessage());
        }

        return employeeDetails;
    }

    private SalaryStatistics calculateMonthlyStatistics(int year, int month) {
        SalaryStatistics stats = new SalaryStatistics(year, month);
        Map<String, Double> componentDetails = new HashMap<>();
        double totalSalary = 0.0;

        try {
            HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
            String sid = sessionService.ensureImportSessionValid(session);

            if (sid != null) {
                String url = erpnextApiUrl + "/resource/Salary Slip";
                
                // Calculer le dernier jour du mois
                YearMonth yearMonth = YearMonth.of(year, month);
                int lastDay = yearMonth.lengthOfMonth();
                
                String filters = String.format("[[\"posting_date\",\"between\",[\"'%d-%02d-01'\",\"'%d-%02d-%02d'\"]]]",
                        year, month, year, month, lastDay);

                HttpHeaders headers = new HttpHeaders();
                headers.add("Cookie", "sid=" + sid);
                
                logger.info("Récupération des statistiques pour {}-{} avec filtre: {}", year, month, filters);

                ResponseEntity<Map> response = restTemplate.exchange(
                    url + "?filters=" + filters,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
                );

                if (response.getBody() != null && response.getBody().get("data") != null) {
                    List<Map<String, Object>> salarySlips = (List<Map<String, Object>>) response.getBody().get("data");
                    logger.debug("Nombre de fiches de paie trouvées: {}", salarySlips.size());
                    
                    for (Map<String, Object> slip : salarySlips) {
                        String slipUrl = erpnextApiUrl + "/resource/Salary Slip/" + slip.get("name");
                        ResponseEntity<Map> slipResponse = restTemplate.exchange(
                            slipUrl,
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            Map.class
                        );

                        if (slipResponse.getBody() != null && slipResponse.getBody().get("data") != null) {
                            Map<String, Object> slipData = (Map<String, Object>) slipResponse.getBody().get("data");
                            List<Map<String, Object>> earnings = (List<Map<String, Object>>) slipData.get("earnings");
                            List<Map<String, Object>> deductions = (List<Map<String, Object>>) slipData.get("deductions");

                            if (earnings != null) {
                                for (Map<String, Object> earning : earnings) {
                                    String componentName = (String) earning.get("salary_component");
                                    Double amount = Double.valueOf(earning.get("amount").toString());
                                    componentDetails.merge(componentName, amount, Double::sum);
                                    totalSalary += amount;
                                }
                            }

                            if (deductions != null) {
                                for (Map<String, Object> deduction : deductions) {
                                    String componentName = (String) deduction.get("salary_component");
                                    Double amount = Double.valueOf(deduction.get("amount").toString());
                                    componentDetails.merge(componentName, -amount, Double::sum);
                                    totalSalary -= amount;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des données de salaire pour {}-{}: {}", year, month, e.getMessage());
        }

        stats.setTotalSalary(totalSalary);
        stats.setSalaryComponentDetails(componentDetails);
        
        return stats;
    }
}