package com.spyder.pdfprocessing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.spyder.pdfprocessing.model.PagedFontResult;
import com.spyder.qdrant.model.DocumentChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfProcessingService {
    
    private final PdfExtractor pdfExtractor;
    private final TextChunker textChunker;
    private final ObjectMapper objectMapper;

    public List<DocumentChunk> processPdf(String pdfPath) throws IOException {
        log.info("Starting PDF processing for: {}", pdfPath);
        
        // Extract text and outline from PDF
        log.info("Extracting text and outline from PDF");
        PagedFontResult pagedResult = pdfExtractor.extractTextWithFontInfo(pdfPath);
        Map<Integer, String[]> outline = pdfExtractor.extractOutlineHierarchy(pdfPath);
        
        // Chunk the text with metadata
        log.info("Chunking text with outline metadata");
        List<DocumentChunk> chunks = textChunker.chunkTextWithOutlineMetadata(pagedResult, outline, pdfPath);
        
        // Save to JSON file
        log.info("Saving {} chunks to JSON file", chunks.size());
        Path outputFile = saveToJsonFile(chunks);
        
        log.info("PDF processing completed successfully");
        return chunks;
    }
    
    private Path saveToJsonFile(List<DocumentChunk> chunks) throws IOException {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path outputFile = tempDir.resolve("chunked-output.json");
        
        try (FileWriter writer = new FileWriter(outputFile.toFile())) {
            objectMapper.writeValue(writer, chunks);
        }
        
        log.info("Successfully saved {} chunks to {}", chunks.size(), outputFile);
        return outputFile;
    }
    
}