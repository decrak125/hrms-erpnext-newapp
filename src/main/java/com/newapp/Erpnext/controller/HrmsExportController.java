package com.newapp.Erpnext.controller;

import com.newapp.Erpnext.services.HrmsService;
import com.newapp.Erpnext.services.SessionService;
import com.opencsv.CSVWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/hrms-export")
public class HrmsExportController {

    private static final Logger logger = LoggerFactory.getLogger(HrmsExportController.class);
    private final SessionService sessionService;
    private final HrmsService hrmsService;

    public HrmsExportController(SessionService sessionService, HrmsService hrmsService) {
        this.sessionService = sessionService;
        this.hrmsService = hrmsService;
    }

    @GetMapping
    public String showHrmsExportPage(
            @RequestParam(name = "table", required = false) String table,
            @RequestParam(name = "filter1", required = false) String filter1,
            @RequestParam(name = "filter2", required = false) String filter2,
            @RequestParam(name = "filter3", required = false) String filter3,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!sessionService.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            logger.info("Affichage de la page HRMS Export avec table={}, filter1={}, filter2={}, filter3={}, sortBy={}",
                table, filter1, filter2, filter3, sortBy);

            // Liste des doctypes disponibles
            List<Map<String, String>> availableTables = hrmsService.getHrmsDoctypes();
            model.addAttribute("availableTables", availableTables);
            model.addAttribute("selectedTable", table);
            model.addAttribute("filter1", filter1);
            model.addAttribute("filter2", filter2);
            model.addAttribute("filter3", filter3);
            model.addAttribute("sortBy", sortBy);

            // Récupérer les données si une table est sélectionnée
            if (table != null && !table.isEmpty()) {
                List<Map<String, Object>> records = hrmsService.getDoctypeData(table, filter1, filter2, filter3, sortBy);
                model.addAttribute("records", records);
                model.addAttribute("tableType", table);
            } else {
                model.addAttribute("records", List.of());
                model.addAttribute("tableType", "");
            }

            return "hrms-export";

        } catch (Exception e) {
            logger.error("Erreur lors de l'affichage de la page HRMS Export: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                "Une erreur est survenue lors du chargement de la page: " + e.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/export/csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportToCsv(
            @RequestParam(name = "table") String table,
            @RequestParam(name = "filter1", required = false) String filter1,
            @RequestParam(name = "filter2", required = false) String filter2,
            @RequestParam(name = "filter3", required = false) String filter3,
            @RequestParam(name = "sortBy", required = false) String sortBy) {
        if (!sessionService.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<Map<String, Object>> records = hrmsService.getDoctypeData(table, filter1, filter2, filter3, sortBy);
            StringWriter stringWriter = new StringWriter();
            try (CSVWriter csvWriter = new CSVWriter(stringWriter, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.DEFAULT_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END)) {
                if ("Employee".equals(table)) {
                    csvWriter.writeNext(new String[]{"ID", "Nom", "Département", "Date d'embauche"});
                    for (Map<String, Object> record : records) {
                        csvWriter.writeNext(new String[]{
                            (String) record.get("name"),
                            (String) record.get("employee_name"),
                            (String) record.get("department"),
                            record.get("date_of_joining") != null ? record.get("date_of_joining").toString() : ""
                        });
                    }
                } else if ("Salary Slip".equals(table)) {
                    csvWriter.writeNext(new String[]{"ID", "Employé", "Département", "Mois", "Date de paiement", "Montant brut", "Montant net", "Impôts", "Statut"});
                    for (Map<String, Object> record : records) {
                        csvWriter.writeNext(new String[]{
                            (String) record.get("name"),
                            (String) record.get("employee_name"),
                            (String) record.get("department"),
                            (String) record.get("month"),
                            record.get("posting_date") != null ? record.get("posting_date").toString() : "",
                            record.get("gross_pay") != null ? record.get("gross_pay").toString() : "0",
                            record.get("net_pay") != null ? record.get("net_pay").toString() : "0",
                            record.get("total_deduction") != null ? record.get("total_deduction").toString() : "0",
                            (String) record.get("status")
                        });
                    }
                } else {
                    return ResponseEntity.badRequest().build();
                }
            }

            byte[] csvBytes = stringWriter.toString().getBytes("UTF-8");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("filename", table + "_export.csv");

            return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Erreur lors de l'exportation CSV: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/export/pdf")
    @ResponseBody
    public ResponseEntity<byte[]> exportToPdf(
            @RequestParam(name = "table") String table,
            @RequestParam(name = "filter1", required = false) String filter1,
            @RequestParam(name = "filter2", required = false) String filter2,
            @RequestParam(name = "filter3", required = false) String filter3,
            @RequestParam(name = "sortBy", required = false) String sortBy) {
        if (!sessionService.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<Map<String, Object>> records = hrmsService.getDoctypeData(table, filter1, filter2, filter3, sortBy);
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Exportation HRMS - " + ("Employee".equals(table) ? "Employés" : "Fiches de paie"), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            PdfPTable pdfTable;
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            if ("Employee".equals(table)) {
                pdfTable = new PdfPTable(4);
                pdfTable.setWidthPercentage(100);
                pdfTable.setWidths(new float[]{1, 2, 2, 2});

                PdfPCell cell;
                cell = new PdfPCell(new Phrase("ID", headerFont));
                cell.setBackgroundColor(new Color(200, 200, 200));
                pdfTable.addCell(cell);
                cell = new PdfPCell(new Phrase("Nom", headerFont));
                cell.setBackgroundColor(new Color(200, 200, 200));
                pdfTable.addCell(cell);
                cell = new PdfPCell(new Phrase("Département", headerFont));
                cell.setBackgroundColor(new Color(200, 200, 200));
                pdfTable.addCell(cell);
                cell = new PdfPCell(new Phrase("Date d'embauche", headerFont));
                cell.setBackgroundColor(new Color(200, 200, 200));
                pdfTable.addCell(cell);

                for (Map<String, Object> record : records) {/*-*/
                    pdfTable.addCell(new Phrase((String) record.get("name"), normalFont));
                    pdfTable.addCell(new Phrase((String) record.get("employee_name"), normalFont));
                    pdfTable.addCell(new Phrase((String) record.get("department"), normalFont));
                    pdfTable.addCell(new Phrase(record.get("date_of_joining") != null ? record.get("date_of_joining").toString() : "", normalFont));
                }
            } else if ("Salary Slip".equals(table)) {
                pdfTable = new PdfPTable(9);
                pdfTable.setWidthPercentage(100);
                pdfTable.setWidths(new float[]{1, 2, 2, 1, 2, 2, 2, 2, 1});

                PdfPCell cell;
                cell = new PdfPCell(new Phrase("ID", headerFont));
                cell.setBackgroundColor(new Color(200, 200, 200));
                pdfTable.addCell(cell);
                cell = new PdfPCell(new Phrase("Employé", headerFont));
                cell.setBackgroundColor(new Color(200, 200, 200));
                pdfTable.addCell(cell);
                cell = new PdfPCell(new Phrase("Département", headerFont));
                cell.setBackgroundColor(new Color(200, 200, 200));
                pdfTable.addCell(cell);
                cell = new PdfPCell(new Phrase("Mois", headerFont));
                cell.setBackgroundColor(new Color(200, 200, 200));
                pdfTable.addCell(cell);
                cell = new PdfPCell(new Phrase("Date de paiement", headerFont));
                cell.setBackgroundColor(new Color(200, 200, 200));
                pdfTable.addCell(cell);
                cell = new PdfPCell(new Phrase("Montant brut", headerFont));
                cell.setBackgroundColor(new Color(200, 200, 200));
                pdfTable.addCell(cell);
                cell = new PdfPCell(new Phrase("Montant net", headerFont));
                cell.setBackgroundColor(new Color(200, 200, 200));
                pdfTable.addCell(cell);
                cell = new PdfPCell(new Phrase("Impôts", headerFont));
                cell.setBackgroundColor(new Color(200, 200, 200));
                pdfTable.addCell(cell);
                cell = new PdfPCell(new Phrase("Statut", headerFont));
                cell.setBackgroundColor(new Color(200, 200, 200));
                pdfTable.addCell(cell);

                for (Map<String, Object> record : records) {
                    pdfTable.addCell(new Phrase((String) record.get("name"), normalFont));
                    pdfTable.addCell(new Phrase((String) record.get("employee_name"), normalFont));
                    pdfTable.addCell(new Phrase((String) record.get("department"), normalFont));
                    pdfTable.addCell(new Phrase((String) record.get("month"), normalFont));
                    pdfTable.addCell(new Phrase(record.get("posting_date") != null ? record.get("posting_date").toString() : "", normalFont));
                    pdfTable.addCell(new Phrase(record.get("gross_pay") != null ? record.get("gross_pay").toString() : "0", normalFont));
                    pdfTable.addCell(new Phrase(record.get("net_pay") != null ? record.get("net_pay").toString() : "0", normalFont));
                    pdfTable.addCell(new Phrase(record.get("total_deduction") != null ? record.get("total_deduction").toString() : "0", normalFont));
                    pdfTable.addCell(new Phrase((String) record.get("status"), normalFont));
                }
            } else {
                document.close();
                return ResponseEntity.badRequest().build();
            }

            document.add(pdfTable);
            document.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", table + "_export.pdf");

            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Erreur lors de l'exportation PDF: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}