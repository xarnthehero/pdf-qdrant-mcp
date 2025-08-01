package com.spyder.pdfprocessing.service;

import com.spyder.pdfprocessing.model.FontAwareTextStripper;
import com.spyder.pdfprocessing.model.PagedFontResult;
import com.spyder.pdfprocessing.model.PagedTextResult;
import org.apache.pdfbox.Loader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
public class PdfExtractor {
    
    public String extractText(String pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }
    
    public PagedTextResult extractTextWithPages(String pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            
            String fullText = pdfStripper.getText(document);
            int totalPages = document.getNumberOfPages();
            Map<Integer, String> pageTexts = new HashMap<>();
            
            for (int page = 1; page <= totalPages; page++) {
                pdfStripper.setStartPage(page);
                pdfStripper.setEndPage(page);
                String pageText = pdfStripper.getText(document);
                pageTexts.put(page, pageText);
            }
            
            return new PagedTextResult(fullText, pageTexts, totalPages);
        }
    }
    
    public PagedFontResult extractTextWithFontInfo(String pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDFTextStripper basicStripper = new PDFTextStripper();
            String fullText = basicStripper.getText(document);
            
            int totalPages = document.getNumberOfPages();
            Map<Integer, String> pageTexts = new HashMap<>();
            Map<Integer, List<FontAwareTextStripper.TextWithFont>> pageFontElements = new HashMap<>();

            for (int page = 1; page <= totalPages; page++) {
                FontAwareTextStripper fontStripper = new FontAwareTextStripper();
                fontStripper.setStartPage(page);
                fontStripper.setEndPage(page);
                
                String pageText = fontStripper.getText(document);
                List<FontAwareTextStripper.TextWithFont> fontElements = fontStripper.getTextElements();
                
                pageTexts.put(page, pageText);
                pageFontElements.put(page, fontElements);
            }
            
            return new PagedFontResult(fullText, pageTexts, pageFontElements, totalPages);
        }
    }
    
    public Map<Integer, String[]> extractOutlineHierarchy(String pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDDocumentOutline outline = catalog.getDocumentOutline();
            
            Map<Integer, String[]> pageToHierarchy = new TreeMap<>();
            
            if (outline != null) {
                log.info("PDF Outline Structure:");
                processOutlineItemHierarchy(outline.getFirstChild(), document, pageToHierarchy, 0, new ArrayList<>());
            } else {
                log.info("No outline found in PDF");
            }
            
            return pageToHierarchy;
        }
    }
    
    public Map<Integer, String> extractOutline(String pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDDocumentOutline outline = catalog.getDocumentOutline();
            
            Map<Integer, String> pageToChapter = new TreeMap<>();
            
            if (outline != null) {
                log.info("PDF Outline Structure:");
                processOutlineItem(outline.getFirstChild(), document, pageToChapter, 0, new ArrayList<>());
            } else {
                log.info("No outline found in PDF");
            }
            
            return pageToChapter;
        }
    }
    
    private void processOutlineItemHierarchy(PDOutlineItem item, PDDocument document, Map<Integer, String[]> pageToHierarchy, int depth, List<String> parentPath) throws IOException {
        while (item != null) {
            String title = item.getTitle();
            
            // Create current path including this item
            List<String> currentPath = new ArrayList<>(parentPath);
            currentPath.add(title);
            
            // Create indentation for tree structure
            String indent = "  ".repeat(depth);
            String prefix = depth == 0 ? "" : "→ ";
            log.info("{}{}{}", indent, prefix, title);

            // Try to resolve destination and associate with page
            Integer pageNum = resolveDestination(item, document, indent);
            if (pageNum != null) {
                // Convert to array: [chapter, heading, subheading]
                String[] hierarchy = new String[3];
                hierarchy[0] = currentPath.size() > 0 ? currentPath.get(0) : null; // chapter
                hierarchy[1] = currentPath.size() > 1 ? currentPath.get(1) : null; // heading
                hierarchy[2] = currentPath.size() > 2 ? currentPath.get(2) : null; // subheading
                
                String hierarchyStr = String.join(" → ", currentPath);
                log.info("{}    [Page {}] {}", indent, pageNum, hierarchyStr);
                pageToHierarchy.put(pageNum, hierarchy);
            }
            
            if (item.hasChildren()) {
                processOutlineItemHierarchy(item.getFirstChild(), document, pageToHierarchy, depth + 1, currentPath);
            }
            
            item = item.getNextSibling();
        }
    }
    
    private void processOutlineItem(PDOutlineItem item, PDDocument document, Map<Integer, String> pageToChapter, int depth, List<String> parentPath) throws IOException {
        while (item != null) {
            String title = item.getTitle();
            
            // Create current path including this item
            List<String> currentPath = new ArrayList<>(parentPath);
            currentPath.add(title);
            
            // Create indentation for tree structure
            String indent = "  ".repeat(depth);
            String prefix = depth == 0 ? "" : "→ ";
            log.info("{}{}{}", indent, prefix, title);
            
            // Build hierarchical chapter string
            String hierarchicalChapter = String.join(" → ", currentPath);
            
            // Try to resolve destination and associate with page
            Integer pageNum = resolveDestination(item, document, indent);
            if (pageNum != null) {
                log.info("{}    [Page {}] {}", indent, pageNum, hierarchicalChapter);
                pageToChapter.put(pageNum, hierarchicalChapter);
            }
            
            if (item.hasChildren()) {
                processOutlineItem(item.getFirstChild(), document, pageToChapter, depth + 1, currentPath);
            }
            
            item = item.getNextSibling();
        }
    }
    
    private Integer resolveDestination(PDOutlineItem item, PDDocument document, String indent) throws IOException {
        // Check if destination is null
        if (item.getDestination() == null) {
            // Check if there's an action instead of destination
            if (item.getAction() != null) {
                // Handle GoTo action which contains destination
                if (item.getAction() instanceof PDActionGoTo gotoAction) {
                    var destination = gotoAction.getDestination();
                    
                    if (destination instanceof PDPageDestination dest) {
                        PDPage page = dest.getPage();
                        return document.getPages().indexOf(page) + 1;
                    } else if (destination instanceof PDNamedDestination namedDest) {
                        return resolveNamedDestination(namedDest, document);
                    }
                }
            }
        } else {
            if (item.getDestination() instanceof PDPageDestination dest) {
                PDPage page = dest.getPage();
                return document.getPages().indexOf(page) + 1;
            } else if (item.getDestination() instanceof PDNamedDestination namedDest) {
                return resolveNamedDestination(namedDest, document);
            }
        }
        return null;
    }
    
    private Integer resolveNamedDestination(PDNamedDestination namedDest, PDDocument document) throws IOException {
        // Try multiple approaches to resolve named destination
        PDDestination resolvedDest = null;
        
        // Approach 1: Try getDests() if it exists
        if (document.getDocumentCatalog().getDests() != null) {
            resolvedDest = document.getDocumentCatalog().getDests().getDestination(namedDest.getNamedDestination());
        }
        
        // Approach 2: Try Names dictionary
        if (resolvedDest == null && document.getDocumentCatalog().getNames() != null) {
            var names = document.getDocumentCatalog().getNames();
            if (names.getDests() != null) {
                resolvedDest = names.getDests().getValue(namedDest.getNamedDestination());
            }
        }
        
        // Approach 3: Try parsing as page number if it's numeric
        if (resolvedDest == null) {
            try {
                int pageNum = Integer.parseInt(namedDest.getNamedDestination());
                if (pageNum > 0 && pageNum <= document.getNumberOfPages()) {
                    return pageNum;
                }
            } catch (NumberFormatException ignored) {
                // Not a number, continue
            }
        }
        
        if (resolvedDest instanceof PDPageDestination pageDest) {
            PDPage page = pageDest.getPage();
            return document.getPages().indexOf(page) + 1;
        }
        
        return null;
    }
    
    public String extractTextFromPage(String pdfPath, int pageNumber) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            pdfStripper.setStartPage(pageNumber);
            pdfStripper.setEndPage(pageNumber);
            return pdfStripper.getText(document);
        }
    }
}