package io.tahawus.lynx.operations.service;

import io.tahawus.lynx.operations.model.DisposalTicket;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Service for generating PDF documents from DisposalTickets.
 *
 * Produces professional-looking tickets with business branding.
 *
 * @author Dan Pasco
 */
@ApplicationScoped
public class DisposalTicketPdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    private static final float MARGIN = 50f;
    private static final float PAGE_WIDTH = PDRectangle.LETTER.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.LETTER.getHeight();

    /**
     * Generate a PDF for the given disposal ticket.
     *
     * @param ticket The disposal ticket
     * @return PDF as byte array
     */
    public byte[] generatePdf(DisposalTicket ticket) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                // Header with business info and logo
                y = drawHeader(document, cs, ticket, y);
                y -= 30;

                // Ticket Information
                y = drawSection(cs, "Ticket Information", y);
                y = drawField(cs, "Ticket Number:", "#" + ticket.ticketNumber, y);
                y = drawField(cs, "Date:", formatDate(ticket.ticketDate), y);
                if (ticket.ticketTime != null) {
                    y = drawField(cs, "Time:", formatTime(ticket.ticketTime), y);
                }
                y = drawField(cs, "Status:", ticket.status.name(), y);
                y -= 20;

                // Company Information
                y = drawSection(cs, "Company Information", y);
                y = drawField(cs, "Trucking Company:",
                        ticket.truckingCompany != null ? ticket.truckingCompany.name : "N/A", y);
                y = drawField(cs, "Oil Company:",
                        ticket.oilCompany != null ? ticket.oilCompany.name : "N/A", y);
                if (ticket.leaseWellNumber != null && !ticket.leaseWellNumber.isEmpty()) {
                    y = drawField(cs, "Lease/Well Number:", ticket.leaseWellNumber, y);
                }
                y -= 20;

                // Barrel Quantities
                y = drawSection(cs, "Barrel Quantities", y);
                y = drawField(cs, "Production (BBL):", formatDecimal(ticket.bblProduction), y);
                y = drawField(cs, "Flowback (BBL):", formatDecimal(ticket.bblFlowback), y);
                y = drawField(cs, "Other (BBL):", formatDecimal(ticket.bblOther), y);
                y -= 10;
                y = drawTotalField(cs, "Total Barrels:", formatDecimal(ticket.getTotalBarrels()), y);
                y -= 20;

                // Notes
                if (ticket.notes != null && !ticket.notes.isEmpty()) {
                    y = drawSection(cs, "Notes", y);
                    y = drawMultilineText(cs, ticket.notes, y);
                }

                // Footer
                drawFooter(cs, ticket);
            }

            document.save(baos);
            return baos.toByteArray();
        }
    }

    private float drawHeader(PDDocument document, PDPageContentStream cs,
                             DisposalTicket ticket, float y) throws IOException {

        float logoHeight = 0;

        // Draw business logo if available
        if (ticket.business != null && ticket.business.hasLogo() ) {
            try {
                PDImageXObject logo = PDImageXObject.createFromByteArray(
                        document, ticket.business.logoData, "logo");

                // Scale logo to max 60px height while maintaining aspect ratio
                float maxLogoHeight = 60;
                float scale = maxLogoHeight / logo.getHeight();
                float logoWidth = logo.getWidth() * scale;
                logoHeight = logo.getHeight() * scale;

                cs.drawImage(logo, MARGIN, y - logoHeight, logoWidth, logoHeight);
            } catch (IOException e) {
                // Logo failed to load, continue without it
                logoHeight = 0;
            }
        }

        // Business name
        float textX = MARGIN;
        float textY = y;

        if (logoHeight > 0) {
            // Position text to the right of logo
            textX = MARGIN + 80;
        }

        if (ticket.business != null && ticket.business.legalName != null) {
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
            cs.beginText();
            cs.newLineAtOffset(textX, textY);
            cs.showText(ticket.business.legalName);
            cs.endText();
            textY -= 18;
        }

        // Document title
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
        cs.beginText();
        cs.newLineAtOffset(textX, textY);
        cs.showText("DISPOSAL TICKET");
        cs.endText();

        // Ticket number on right side
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
        String ticketNum = "#" + ticket.ticketNumber;
        float textWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                .getStringWidth(ticketNum) / 1000 * 16;
        cs.beginText();
        cs.newLineAtOffset(PAGE_WIDTH - MARGIN - textWidth, y);
        cs.showText(ticketNum);
        cs.endText();

        // Line under header
        float lineY = Math.min(y - logoHeight - 10, textY - 10);
        cs.setLineWidth(2f);
        cs.moveTo(MARGIN, lineY);
        cs.lineTo(PAGE_WIDTH - MARGIN, lineY);
        cs.stroke();

        return lineY - 10;
    }

    private float drawSection(PDPageContentStream cs, String title, float y) throws IOException {
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
        cs.beginText();
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(title);
        cs.endText();

        y -= 5;
        cs.setLineWidth(1f);
        cs.moveTo(MARGIN, y);
        cs.lineTo(PAGE_WIDTH - MARGIN, y);
        cs.stroke();

        return y - 15;
    }

    private float drawField(PDPageContentStream cs, String label, String value, float y) throws IOException {
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
        cs.beginText();
        cs.newLineAtOffset(MARGIN + 20, y);
        cs.showText(label);
        cs.endText();

        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
        cs.beginText();
        cs.newLineAtOffset(MARGIN + 180, y);
        cs.showText(value != null ? value : "");
        cs.endText();

        return y - 18;
    }

    private float drawTotalField(PDPageContentStream cs, String label, String value, float y) throws IOException {
        float boxHeight = 25;
        float boxY = y - 5;

        cs.setLineWidth(2f);
        cs.addRect(MARGIN + 20, boxY, PAGE_WIDTH - (2 * MARGIN) - 40, boxHeight);
        cs.stroke();

        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
        cs.beginText();
        cs.newLineAtOffset(MARGIN + 30, boxY + 8);
        cs.showText(label);
        cs.endText();

        cs.beginText();
        cs.newLineAtOffset(MARGIN + 180, boxY + 8);
        cs.showText(value);
        cs.endText();

        return boxY - 10;
    }

    private float drawMultilineText(PDPageContentStream cs, String text, float y) throws IOException {
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);

        String[] lines = text.split("\n");
        for (String line : lines) {
            String[] words = line.split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                float textWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
                        .getStringWidth(testLine) / 1000 * 10;

                if (textWidth > PAGE_WIDTH - (2 * MARGIN) - 40) {
                    cs.beginText();
                    cs.newLineAtOffset(MARGIN + 20, y);
                    cs.showText(currentLine.toString());
                    cs.endText();
                    y -= 14;
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine = new StringBuilder(testLine);
                }
            }

            if (currentLine.length() > 0) {
                cs.beginText();
                cs.newLineAtOffset(MARGIN + 20, y);
                cs.showText(currentLine.toString());
                cs.endText();
                y -= 14;
            }
        }

        return y;
    }

    private void drawFooter(PDPageContentStream cs, DisposalTicket ticket) throws IOException {
        float y = MARGIN - 10;

        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
        cs.beginText();
        cs.newLineAtOffset(MARGIN, y);

        StringBuilder footer = new StringBuilder();
        footer.append("Created: ").append(formatDate(ticket.createdAt.toLocalDate()));
        if (ticket.createdBy != null) {
            footer.append(" by ").append(ticket.createdBy);
        }

        if (ticket.modifiedAt != null) {
            footer.append(" | Modified: ").append(formatDate(ticket.modifiedAt.toLocalDate()));
            if (ticket.modifiedBy != null) {
                footer.append(" by ").append(ticket.modifiedBy);
            }
        }

        cs.showText(footer.toString());
        cs.endText();
    }

    private String formatDate(java.time.LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    private String formatTime(java.time.LocalTime time) {
        return time != null ? time.format(TIME_FORMATTER) : "";
    }

    private String formatDecimal(BigDecimal value) {
        return value != null ? String.format("%.2f", value) : "0.00";
    }
}
