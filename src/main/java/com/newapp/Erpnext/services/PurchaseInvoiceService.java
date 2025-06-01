package com.newapp.Erpnext.services;

import com.newapp.Erpnext.models.PurchaseInvoice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.util.Locale;
import java.awt.Color;

@Service
public class PurchaseInvoiceService {

    @Value("${erpnext.url}")
    private String erpNextUrl;
    
    private final RestTemplate restTemplate;
    private final ErpNextAuthService authService;
    
    public PurchaseInvoiceService(RestTemplateBuilder restTemplateBuilder, ErpNextAuthService authService) {
        this.restTemplate = restTemplateBuilder.build();
        this.authService = authService;
    }
    
    /**
     * Récupère toutes les factures d'achat
     * @return Liste des factures d'achat
     */
    public List<PurchaseInvoice> getAllPurchaseInvoices() {
        HttpHeaders headers = createHeaders();
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        String url = erpNextUrl + "/api/resource/Purchase Invoice?fields=[\"name\",\"status\",\"creation\",\"supplier\",\"company\"]";
        
        try {
            System.out.println("Calling API: " + url);
            System.out.println("Headers: " + headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            System.out.println("Response status: " + response.getStatusCode());
            if (response.getBody() != null) {
                System.out.println("Response body keys: " + response.getBody().keySet());
            }
            
            List<PurchaseInvoice> purchaseInvoices = new ArrayList<>();
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                if (responseBody.containsKey("data")) {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                    System.out.println("Number of purchase invoices found: " + data.size());
                    
                    for (Map<String, Object> invoiceData : data) {
                        System.out.println("Purchase invoice data: " + invoiceData);
                        String name = (String) invoiceData.get("name");
                        String status = (String) invoiceData.get("status");
                        String creationStr = (String) invoiceData.get("creation");
                        String supplier = (String) invoiceData.get("supplier");
                        String company = (String) invoiceData.get("company");
                        
                        System.out.println("Processing purchase invoice: " + name);
                        System.out.println("  Status: " + status);
                        System.out.println("  Creation: " + creationStr);
                        System.out.println("  Supplier: " + supplier);
                        System.out.println("  Company: " + company);
                        
                        // Créer un formateur personnalisé pour le format de date renvoyé par l'API
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
                        
                        // Convertir la date de création
                        LocalDateTime creation;
                        try {
                            creation = LocalDateTime.parse(creationStr, formatter);
                        } catch (Exception e) {
                            System.err.println("Error parsing date: " + creationStr + " - " + e.getMessage());
                            // Utiliser la date actuelle en cas d'échec de l'analyse
                            creation = LocalDateTime.now();
                        }
                        
                        PurchaseInvoice purchaseInvoice = new PurchaseInvoice(name, status, creation, supplier, company);
                        purchaseInvoices.add(purchaseInvoice);
                    }
                } else {
                    System.err.println("No 'data' key found in response");
                }
            }
            
            System.out.println("Returning " + purchaseInvoices.size() + " purchase invoices");
            return purchaseInvoices;
        } catch (Exception e) {
            System.err.println("Error retrieving purchase invoices: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Marque une facture comme payée
     * @param invoiceId ID de la facture
     * @return true si la mise à jour a réussi, false sinon
     */
    public boolean markAsPaid(String invoiceId) {
        // Cette méthode serait implémentée pour mettre à jour le statut de la facture
        // via l'API ERPNext. Pour l'instant, elle renvoie simplement true.
        System.out.println("Marking invoice as paid: " + invoiceId);
        return true;
    }
    
    /**
     * Crée les en-têtes HTTP avec l'authentification
     * @return En-têtes HTTP
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Utilisation de l'authentification par API key
        String authToken = authService.getApiAuthToken();
        headers.set("Authorization", "token " + authToken);
        
        return headers;
    }
    
    /**
     * Récupère une facture d'achat par son ID
     * @param invoiceId ID de la facture
     * @return Facture d'achat ou null si non trouvée
     */
    public PurchaseInvoice getPurchaseInvoiceById(String invoiceId) {
        HttpHeaders headers = createHeaders();
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        String url = erpNextUrl + "/api/resource/Purchase Invoice/" + invoiceId;
        
        try {
            System.out.println("Fetching invoice details: " + invoiceId);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                if (responseBody.containsKey("data")) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                    
                    String name = (String) data.get("name");
                    String status = (String) data.get("status");
                    String creationStr = (String) data.get("creation");
                    String supplier = (String) data.get("supplier");
                    String company = (String) data.get("company");
                    
                    // Créer un formateur personnalisé pour le format de date renvoyé par l'API
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
                    
                    // Convertir la date de création
                    LocalDateTime creation;
                    try {
                        creation = LocalDateTime.parse(creationStr, formatter);
                    } catch (Exception e) {
                        System.err.println("Error parsing date: " + creationStr + " - " + e.getMessage());
                        creation = LocalDateTime.now();
                    }
                    
                    return new PurchaseInvoice(name, status, creation, supplier, company);
                }
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("Error retrieving invoice: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Récupère le montant d'une facture
     * @param invoiceId ID de la facture
     * @return Montant de la facture ou 0.0 en cas d'erreur
     */
    public Double getInvoiceAmount(String invoiceId) {
        HttpHeaders headers = createHeaders();
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        String url = erpNextUrl + "/api/resource/Purchase Invoice/" + invoiceId;
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                if (responseBody.containsKey("data")) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                    
                    if (data.containsKey("grand_total")) {
                        return Double.parseDouble(data.get("grand_total").toString());
                    }
                }
            }
            
            return 0.0;
        } catch (Exception e) {
            System.err.println("Error retrieving invoice amount: " + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }
    
    /**
     * Récupère les détails complets d'une facture
     *    /**
     * Récupère les détails complets d'une facture
     * @param invoiceId ID de la facture
     * @return Map contenant les détails de la facture ou null en cas d'erreur
     */
    public Map<String, Object> getInvoiceDetails(String invoiceId) {
        HttpHeaders headers = createHeaders();
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        String url = erpNextUrl + "/api/resource/Purchase Invoice/" + invoiceId + "?fields=[\"*\"]";
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                if (responseBody.containsKey("data")) {
                    return (Map<String, Object>) responseBody.get("data");
                }
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("Error retrieving invoice details: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Génère un PDF pour une facture
     * @param invoiceId ID de la facture
     * @return Tableau d'octets contenant le PDF ou null en cas d'erreur
     */
    public byte[] generateInvoicePdf(String invoiceId) {
        try {
            // Récupérer les détails complets de la facture
            Map<String, Object> invoiceDetails = getInvoiceDetails(invoiceId);
            if (invoiceDetails == null) {
                return null;
            }
            
            // Récupérer la facture de base
            PurchaseInvoice invoice = getPurchaseInvoiceById(invoiceId);
            if (invoice == null) {
                return null;
            }
            
            // Créer un document PDF
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Ajouter le logo et les informations d'en-tête
            addHeader(document, invoice);
            
            // Ajouter les informations de la facture
            addInvoiceInfo(document, invoice, invoiceDetails);
            
            // Ajouter les articles de la facture
            addInvoiceItems(document, invoiceDetails);
            
            // Ajouter le total
            addTotal(document, invoiceDetails);
            
            // Ajouter le pied de page
            addFooter(document);
            
            document.close();
            
            return baos.toByteArray();
        } catch (Exception e) {
            System.err.println("Error generating PDF: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void addHeader(Document document, PurchaseInvoice invoice) throws DocumentException {
        // Titre
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
        Paragraph title = new Paragraph("FACTURE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);
        
        // Informations de l'entreprise et du fournisseur
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        
        // Informations de l'entreprise
        PdfPCell companyCell = new PdfPCell();
        companyCell.setBorder(Rectangle.NO_BORDER);
        companyCell.addElement(new Paragraph("Entreprise: " + invoice.getCompany()));
        headerTable.addCell(companyCell);
        
        // Informations du fournisseur
        PdfPCell supplierCell = new PdfPCell();
        supplierCell.setBorder(Rectangle.NO_BORDER);
        supplierCell.addElement(new Paragraph("Fournisseur: " + invoice.getSupplier()));
        headerTable.addCell(supplierCell);
        
        document.add(headerTable);
        document.add(new Paragraph(" ")); // Espace
    }

    private void addInvoiceInfo(Document document, PurchaseInvoice invoice, Map<String, Object> invoiceDetails) throws DocumentException {
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        
        // Numéro de facture
        PdfPCell invoiceNumberCell = new PdfPCell();
        invoiceNumberCell.setBorder(Rectangle.NO_BORDER);
        invoiceNumberCell.addElement(new Paragraph("Numéro de facture: " + invoice.getName()));
        infoTable.addCell(invoiceNumberCell);
        
        // Date de création
        PdfPCell dateCell = new PdfPCell();
        dateCell.setBorder(Rectangle.NO_BORDER);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        dateCell.addElement(new Paragraph("Date: " + invoice.getCreation().format(formatter)));
        infoTable.addCell(dateCell);
        
        // Statut
        PdfPCell statusCell = new PdfPCell();
        statusCell.setBorder(Rectangle.NO_BORDER);
        String statusText = invoice.getStatus();
        switch (statusText) {
            case "Paid": statusText = "Payée"; break;
            case "Unpaid": statusText = "Non payée"; break;
            case "Overdue": statusText = "En retard"; break;
            case "Cancelled": statusText = "Annulée"; break;
        }
        statusCell.addElement(new Paragraph("Statut: " + statusText));
        infoTable.addCell(statusCell);
        
        // Cellule vide pour l'alignement
        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBorder(Rectangle.NO_BORDER);
        infoTable.addCell(emptyCell);
        
        document.add(infoTable);
        document.add(new Paragraph(" ")); // Espace
    }

    @SuppressWarnings("unchecked")
    private void addInvoiceItems(Document document, Map<String, Object> invoiceDetails) throws DocumentException {
        // Titre de la section
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
        Paragraph itemsTitle = new Paragraph("Articles", sectionFont);
        itemsTitle.setSpacingAfter(10);
        document.add(itemsTitle);
        
        // Tableau des articles
        PdfPTable itemsTable = new PdfPTable(new float[]{3, 1, 1, 1});
        itemsTable.setWidthPercentage(100);
        
        // En-têtes du tableau
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        
        PdfPCell headerCell1 = new PdfPCell(new Phrase("Description", headerFont));
        headerCell1.setBackgroundColor(Color.DARK_GRAY);
        headerCell1.setPadding(5);
        itemsTable.addCell(headerCell1);
        
        PdfPCell headerCell2 = new PdfPCell(new Phrase("Quantité", headerFont));
        headerCell2.setBackgroundColor(Color.DARK_GRAY);
        headerCell2.setPadding(5);
        itemsTable.addCell(headerCell2);
        
        PdfPCell headerCell3 = new PdfPCell(new Phrase("Prix unitaire", headerFont));
        headerCell3.setBackgroundColor(Color.DARK_GRAY);
        headerCell3.setPadding(5);
        itemsTable.addCell(headerCell3);
        
        PdfPCell headerCell4 = new PdfPCell(new Phrase("Total", headerFont));
        headerCell4.setBackgroundColor(Color.DARK_GRAY);
        headerCell4.setPadding(5);
        itemsTable.addCell(headerCell4);
        
        // Récupérer les articles de la facture
        List<Map<String, Object>> items = new ArrayList<>();
        if (invoiceDetails.containsKey("items")) {
            items = (List<Map<String, Object>>) invoiceDetails.get("items");
        }
        
        // Formatter pour les montants
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        
        // Ajouter les articles au tableau
        for (Map<String, Object> item : items) {
            String description = (String) item.getOrDefault("description", "");
            double qty = Double.parseDouble(item.getOrDefault("qty", "0").toString());
            double rate = Double.parseDouble(item.getOrDefault("rate", "0").toString());
            double amount = Double.parseDouble(item.getOrDefault("amount", "0").toString());
            
            itemsTable.addCell(new Phrase(description));
            itemsTable.addCell(new Phrase(String.valueOf(qty)));
            itemsTable.addCell(new Phrase(currencyFormatter.format(rate)));
            itemsTable.addCell(new Phrase(currencyFormatter.format(amount)));
        }
        
        document.add(itemsTable);
        document.add(new Paragraph(" ")); // Espace
    }

    private void addTotal(Document document, Map<String, Object> invoiceDetails) throws DocumentException {
        // Formatter pour les montants
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        
        // Récupérer les totaux
        double totalHT = Double.parseDouble(invoiceDetails.getOrDefault("net_total", "0").toString());
        double totalTVA = Double.parseDouble(invoiceDetails.getOrDefault("total_taxes_and_charges", "0").toString());
        double totalTTC = Double.parseDouble(invoiceDetails.getOrDefault("grand_total", "0").toString());
        
        // Tableau des totaux
        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(50);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        
        // Total HT
        PdfPCell totalHTLabelCell = new PdfPCell(new Phrase("Total HT:"));
        totalHTLabelCell.setBorder(Rectangle.NO_BORDER);
        totalHTLabelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        totalsTable.addCell(totalHTLabelCell);
        
        PdfPCell totalHTValueCell = new PdfPCell(new Phrase(currencyFormatter.format(totalHT)));
        totalHTValueCell.setBorder(Rectangle.NO_BORDER);
        totalHTValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.addCell(totalHTValueCell);
        
        // TVA
        PdfPCell totalTVALabelCell = new PdfPCell(new Phrase("TVA:"));
        totalTVALabelCell.setBorder(Rectangle.NO_BORDER);
        totalTVALabelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        totalsTable.addCell(totalTVALabelCell);
        
        PdfPCell totalTVAValueCell = new PdfPCell(new Phrase(currencyFormatter.format(totalTVA)));
        totalTVAValueCell.setBorder(Rectangle.NO_BORDER);
        totalTVAValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.addCell(totalTVAValueCell);
        
        // Total TTC
        PdfPCell totalTTCLabelCell = new PdfPCell(new Phrase("Total TTC:"));
        totalTTCLabelCell.setBorder(Rectangle.TOP);
        totalTTCLabelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        totalsTable.addCell(totalTTCLabelCell);
        
        PdfPCell totalTTCValueCell = new PdfPCell(new Phrase(currencyFormatter.format(totalTTC)));
        totalTTCValueCell.setBorder(Rectangle.TOP);
        totalTTCValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.addCell(totalTTCValueCell);
        
        document.add(totalsTable);
    }

    private void addFooter(Document document) throws DocumentException {
        document.add(new Paragraph(" ")); // Espace
        document.add(new Paragraph(" ")); // Espace
        
        Paragraph footer = new Paragraph("Merci pour votre confiance !");
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }


}