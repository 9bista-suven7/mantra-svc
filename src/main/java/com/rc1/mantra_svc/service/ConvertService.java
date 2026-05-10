package com.rc1.mantra_svc.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.util.Matrix;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * File conversion service supporting PDF operations, image↔PDF, and Word↔PDF.
 * All operations are synchronous and should be called on a bounded-elastic scheduler.
 */
@Service
public class ConvertService {

    /**
     * Converts a JPG or PNG image to a single-page A4 PDF.
     *
     * @param imageBytes raw image bytes
     * @param filename   original filename (used for type detection)
     * @return PDF bytes
     */
    public byte[] imageToPdf(byte[] imageBytes, String filename) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDImageXObject image = PDImageXObject.createFromByteArray(doc, imageBytes, filename);
            float pageW = page.getMediaBox().getWidth();
            float pageH = page.getMediaBox().getHeight();
            float margin = 40f;
            float maxW = pageW - margin * 2;
            float maxH = pageH - margin * 2;
            float scale = Math.min(maxW / image.getWidth(), maxH / image.getHeight());
            float imgW = image.getWidth() * scale;
            float imgH = image.getHeight() * scale;
            float x = (pageW - imgW) / 2;
            float y = (pageH - imgH) / 2;
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.drawImage(image, x, y, imgW, imgH);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Renders the first page of a PDF to a JPEG image at 150 DPI.
     *
     * @param pdfBytes raw PDF bytes
     * @return JPEG bytes of the first page
     */
    public byte[] pdfToImage(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage image = renderer.renderImageWithDPI(0, 150);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", out);
            return out.toByteArray();
        }
    }

    /**
     * Splits a PDF into individual pages and returns them as a ZIP archive.
     *
     * @param pdfBytes raw PDF bytes
     * @return ZIP bytes containing one PDF per page
     */
    public byte[] splitPdf(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            Splitter splitter = new Splitter();
            List<PDDocument> pages = splitter.split(doc);
            ByteArrayOutputStream zip = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(zip)) {
                for (int i = 0; i < pages.size(); i++) {
                    try (PDDocument page = pages.get(i)) {
                        ByteArrayOutputStream pageOut = new ByteArrayOutputStream();
                        page.save(pageOut);
                        ZipEntry entry = new ZipEntry("page_" + (i + 1) + ".pdf");
                        zos.putNextEntry(entry);
                        zos.write(pageOut.toByteArray());
                        zos.closeEntry();
                    }
                }
            }
            return zip.toByteArray();
        }
    }

    /**
     * Merges multiple PDFs into a single PDF document.
     *
     * @param pdfList list of raw PDF byte arrays to merge in order
     * @return merged PDF bytes
     */
    public byte[] mergePdfs(List<byte[]> pdfList) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        merger.setDestinationStream(out);
        for (byte[] pdf : pdfList) {
            merger.addSource(new RandomAccessReadBuffer(pdf));
        }
        merger.mergeDocuments(null);
        return out.toByteArray();
    }

    /**
     * Extracts text from a DOCX file and writes it as a plain-text PDF.
     * Note: formatting (fonts, tables, images) is not preserved.
     *
     * @param docxBytes raw DOCX bytes
     * @return PDF bytes
     */
    public byte[] wordToPdf(byte[] docxBytes) throws IOException {
        // Read paragraphs from DOCX
        List<String> lines;
        try (XWPFDocument docx = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            lines = docx.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .toList();
        }

        // Write text into PDF pages
        try (PDDocument pdf = new PDDocument()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float margin = 50f;
            float fontSize = 11f;
            float leading = fontSize * 1.5f;
            float pageHeight = PDRectangle.A4.getHeight();
            PDPage page = null;
            PDPageContentStream cs = null;
            float y = 0;

            for (String line : lines) {
                if (page == null || y < margin + leading) {
                    if (cs != null) {
                        cs.endText();
                        cs.close();
                    }
                    page = new PDPage(PDRectangle.A4);
                    pdf.addPage(page);
                    cs = new PDPageContentStream(pdf, page);
                    cs.beginText();
                    cs.setFont(font, fontSize);
                    cs.newLineAtOffset(margin, pageHeight - margin);
                    y = pageHeight - margin;
                }
                // Strip non-latin1 chars to avoid encoding issues with PDType1Font
                String safe = line.replaceAll("[^\\x20-\\x7E]", " ");
                cs.showText(safe);
                cs.newLineAtOffset(0, -leading);
                y -= leading;
            }
            if (cs != null) {
                cs.endText();
                cs.close();
            }
            if (pdf.getNumberOfPages() == 0) {
                pdf.addPage(new PDPage(PDRectangle.A4));
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdf.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Extracts text from a PDF and writes each line into a DOCX paragraph.
     * Note: original formatting is not preserved.
     *
     * @param pdfBytes raw PDF bytes
     * @return DOCX bytes
     */
    public byte[] pdfToWord(byte[] pdfBytes) throws IOException {
        String text;
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            text = new PDFTextStripper().getText(doc);
        }
        try (XWPFDocument docx = new XWPFDocument()) {
            for (String line : text.split("\n")) {
                XWPFParagraph p = docx.createParagraph();
                XWPFRun run = p.createRun();
                run.setFontFamily("Calibri");
                run.setFontSize(11);
                run.setText(line);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            docx.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Stamps a diagonal text watermark on every page of the PDF.
     *
     * @param pdfBytes      raw PDF bytes
     * @param watermarkText text to stamp (max 30 chars)
     * @return watermarked PDF bytes
     */
    public byte[] addWatermark(byte[] pdfBytes, String watermarkText) throws IOException {
        String label = (watermarkText == null || watermarkText.isBlank()) ? "CONFIDENTIAL" : watermarkText;
        String safe  = label.replaceAll("[^\\x20-\\x7E]", " ");
        if (safe.length() > 30) safe = safe.substring(0, 30);

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            double angle = Math.toRadians(45);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            for (PDPage page : doc.getPages()) {
                float w = page.getMediaBox().getWidth();
                float h = page.getMediaBox().getHeight();
                try (PDPageContentStream cs = new PDPageContentStream(
                        doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    cs.saveGraphicsState();
                    cs.setNonStrokingColor(new Color(180, 180, 180));
                    cs.beginText();
                    cs.setFont(font, 48f);
                    cs.setTextMatrix(new Matrix(cos, sin, -sin, cos, w / 2 - 90f, h / 2 - 30f));
                    cs.showText(safe);
                    cs.endText();
                    cs.restoreGraphicsState();
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Removes standard PDF Watermark annotations from all pages.
     * Works for annotation-based watermarks; content-stream watermarks are not removed.
     *
     * @param pdfBytes raw PDF bytes
     * @return cleaned PDF bytes
     */
    public byte[] removeWatermark(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            for (PDPage page : doc.getPages()) {
                List<PDAnnotation> kept = page.getAnnotations().stream()
                        .filter(a -> !"Watermark".equals(a.getSubtype()))
                        .collect(Collectors.toList());
                page.setAnnotations(kept);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Password-protects a PDF with the given user password (128-bit RC4).
     *
     * @param pdfBytes raw PDF bytes
     * @param password password to apply
     * @return encrypted PDF bytes
     */
    public byte[] lockPdf(byte[] pdfBytes, String password) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            AccessPermission ap = new AccessPermission();
            StandardProtectionPolicy policy = new StandardProtectionPolicy(password, password, ap);
            policy.setEncryptionKeyLength(128);
            doc.protect(policy);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Removes encryption from a password-protected PDF.
     *
     * @param pdfBytes raw PDF bytes
     * @param password current user/owner password
     * @return decrypted PDF bytes
     */
    public byte[] unlockPdf(byte[] pdfBytes, String password) throws IOException {
        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(pdfBytes), password)) {
            doc.setAllSecurityToBeRemoved(true);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Extracts specific pages from a PDF into a new document.
     * Accepts comma-separated page numbers and ranges, e.g. "1,3,5-8".
     *
     * @param pdfBytes  raw PDF bytes
     * @param pageRange page selection string (1-indexed)
     * @return PDF containing only the requested pages
     */
    public byte[] extractPages(byte[] pdfBytes, String pageRange) throws IOException {
        try (PDDocument src = Loader.loadPDF(pdfBytes)) {
            int total = src.getNumberOfPages();
            List<Integer> pageNums = parsePageRange(pageRange, total);
            if (pageNums.isEmpty()) throw new IOException("No valid pages in range: " + pageRange);

            Splitter splitter = new Splitter();
            List<PDDocument> allPages = splitter.split(src);

            List<byte[]> selectedPdfs = new ArrayList<>();
            for (int n : pageNums) {
                ByteArrayOutputStream pageOut = new ByteArrayOutputStream();
                allPages.get(n - 1).save(pageOut);
                selectedPdfs.add(pageOut.toByteArray());
            }
            for (PDDocument pd : allPages) {
                try { pd.close(); } catch (IOException ignored) {}
            }

            PDFMergerUtility merger = new PDFMergerUtility();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            merger.setDestinationStream(out);
            for (byte[] pdf : selectedPdfs) {
                merger.addSource(new RandomAccessReadBuffer(pdf));
            }
            merger.mergeDocuments(null);
            return out.toByteArray();
        }
    }

    /**
     * Overlays a text string at the bottom-left of the specified page (1-indexed).
     *
     * @param pdfBytes raw PDF bytes
     * @param text     text to add
     * @param pageNum  1-indexed page number
     * @return modified PDF bytes
     */
    public byte[] addTextToPdf(byte[] pdfBytes, String text, int pageNum) throws IOException {
        String safe = text.replaceAll("[^\\x20-\\x7E]", " ");
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            int idx = Math.max(0, Math.min(pageNum - 1, doc.getNumberOfPages() - 1));
            PDPage page = doc.getPage(idx);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream cs = new PDPageContentStream(
                    doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                cs.saveGraphicsState();
                cs.setNonStrokingColor(new Color(20, 20, 20));
                cs.beginText();
                cs.setFont(font, 12f);
                cs.newLineAtOffset(50f, 50f);
                cs.showText(safe.length() > 200 ? safe.substring(0, 200) : safe);
                cs.endText();
                cs.restoreGraphicsState();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Parses a page range string like "1,3,5-8" into a sorted list of 1-indexed page numbers.
     */
    private List<Integer> parsePageRange(String range, int totalPages) {
        Set<Integer> pages = new LinkedHashSet<>();
        for (String part : range.split(",")) {
            part = part.trim();
            if (part.contains("-")) {
                String[] bounds = part.split("-", 2);
                try {
                    int from = Integer.parseInt(bounds[0].trim());
                    int to   = Integer.parseInt(bounds[1].trim());
                    for (int i = from; i <= to; i++) pages.add(i);
                } catch (NumberFormatException ignored) {}
            } else if (!part.isEmpty()) {
                try { pages.add(Integer.parseInt(part)); } catch (NumberFormatException ignored) {}
            }
        }
        return pages.stream()
                .filter(p -> p >= 1 && p <= totalPages)
                .sorted()
                .collect(Collectors.toList());
    }
}

