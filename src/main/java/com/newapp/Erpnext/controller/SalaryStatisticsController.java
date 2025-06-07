package com.newapp.Erpnext.controller;

import com.newapp.Erpnext.models.SalaryStatistics;
import com.newapp.Erpnext.models.EmployeeSalaryDetail;
import com.newapp.Erpnext.services.SalaryStatisticsService;
import com.newapp.Erpnext.services.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.time.Year;

@Controller
@RequestMapping("/salary-statistics")
public class SalaryStatisticsController {
    private static final Logger logger = LoggerFactory.getLogger(SalaryStatisticsController.class);

    @Autowired
    private SalaryStatisticsService salaryStatisticsService;

    @Autowired
    private SessionService sessionService;

    @GetMapping
    public String showStatistics(@RequestParam(required = false) Integer year, Model model) {
        logger.info("Affichage des statistiques salariales pour l'année: {}", year);

        try {
            if (!sessionService.isAuthenticated()) {
                logger.error("L'utilisateur n'est pas authentifié");
                return "redirect:/login";
            }

            int selectedYear = (year != null) ? year : Year.now().getValue();
            List<SalaryStatistics> statistics = salaryStatisticsService.getYearlyStatistics(selectedYear);
            
            model.addAttribute("statistics", statistics);
            model.addAttribute("selectedYear", selectedYear);
            model.addAttribute("currentYear", Year.now().getValue());
            
            logger.info("Statistiques salariales récupérées avec succès pour l'année {}", selectedYear);
            return "salary-statistics";

        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des statistiques salariales", e);
            model.addAttribute("error", "Une erreur est survenue lors de la récupération des statistiques : " + e.getMessage());
            return "salary-statistics";
        }
    }

    @GetMapping("/details")
    public String showEmployeeSalaryDetails(@RequestParam("year") int year, @RequestParam("month") int month, Model model) {
        logger.info("Affichage des détails des salaires des employés pour l'année: {}, mois: {}", year, month);

        try {
            if (!sessionService.isAuthenticated()) {
                logger.error("L'utilisateur n'est pas authent raffiché");
                return "redirect:/login";
            }

            List<EmployeeSalaryDetail> employeeDetails = salaryStatisticsService.getEmployeeSalaryDetails(year, month);
            
            model.addAttribute("employeeDetails", employeeDetails);
            model.addAttribute("year", year);
            model.addAttribute("month", month);
            
            logger.info("Détails des salaires des employés récupérés avec succès pour {}-{}", year, month);
            return "employee-salary-details";

        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des détails des salaires des employés", e);
            model.addAttribute("error", "Une erreur est survenue lors de la récupération des détails : " + e.getMessage());
            return "employee-salary-details";
        }
    }
}