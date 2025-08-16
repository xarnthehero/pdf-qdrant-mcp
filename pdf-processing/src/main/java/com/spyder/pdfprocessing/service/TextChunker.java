package com.spyder.pdfprocessing.service;

import com.spyder.pdfprocessing.config.PdfProperties;
import com.spyder.pdfprocessing.model.PagedFontResult;
import com.spyder.qdrant.model.DocumentChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TextChunker {
    
    private final PdfProperties properties;
    
    private int getChunkSize() {
        return properties.getChunking().getSize();
    }
    
    private int getOverlap() {
        return properties.getChunking().getOverlap();
    }

    public List<DocumentChunk> chunkTextWithOutlineMetadata(PagedFontResult pagedResult, Map<Integer, String[]> outline, String sourcePath) {
        List<DocumentChunk> chunks = new ArrayList<>();
        Map<Integer, String> pageTexts = pagedResult.getPageTexts();
        
        // Extract just the filename from the full path
        String fileName = java.nio.file.Paths.get(sourcePath).getFileName().toString();
        
        int chunkIndex = 0;
        String currentChapter = null;
        String currentHeading = null;
        String currentSubheading = null;
        
        for (Map.Entry<Integer, String> entry : pageTexts.entrySet()) {
            int pageNumber = entry.getKey();
            String pageText = entry.getValue();
            
            if (pageText == null || pageText.trim().isEmpty()) {
                continue;
            }
            
            // Check if this page starts a new section according to outline
            if (outline.containsKey(pageNumber)) {
                String[] hierarchy = outline.get(pageNumber);
                currentChapter = hierarchy[0];
                currentHeading = hierarchy[1];
                currentSubheading = hierarchy[2];
                
                String hierarchyStr = String.join(" → ", java.util.Arrays.stream(hierarchy)
                    .filter(s -> s != null)
                    .toArray(String[]::new));
                log.debug("Outline hierarchy on page {}: '{}'", pageNumber, hierarchyStr);
            }
            
            List<String> pageChunks = chunkText(pageText);
            
            for (String chunkContent : pageChunks) {
                DocumentChunk chunk = new DocumentChunk(
                    chunkContent, 
                    fileName, 
                    pageNumber, 
                    currentChapter,
                    currentHeading,
                    currentSubheading,
                    chunkIndex++
                );
                chunks.add(chunk);
            }
        }
        
        return chunks;
    }

    public List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + getChunkSize(), text.length());
            
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
            }
            
            chunks.add(text.substring(start, end).trim());
            
            if (end >= text.length()) {
                break;
            }
            
            start = Math.max(start + 1, end - getOverlap());
        }
        
        return chunks;
    }
    
    public List<String> chunkBySentences(String text, int maxSentencesPerChunk) {
        List<String> chunks = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        
        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder currentChunk = new StringBuilder();
        int sentenceCount = 0;
        
        for (String sentence : sentences) {
            if (sentenceCount > 0 && sentenceCount >= maxSentencesPerChunk) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
                sentenceCount = 0;
            }
            
            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
            }
            currentChunk.append(sentence.trim());
            sentenceCount++;
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
}