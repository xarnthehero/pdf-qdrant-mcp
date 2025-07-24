package com.spyder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.spyder.model.DocumentChunk;
import com.spyder.model.PagedFontResult;
import com.spyder.config.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfProcessingService {
    
    private final PdfExtractor pdfExtractor;
    private final TextChunker textChunker;
    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;
    private final ObjectMapper objectMapper;
    private final ApplicationProperties properties;
    
    private int getEmbeddingDimensions() {
        return properties.getEmbedding().getDimensions();
    }
    
    public void processPdf(String pdfPath, boolean uploadToQdrant) throws IOException, ExecutionException, InterruptedException {
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
        saveToJsonFile(chunks);
        
        // Upload to Qdrant if requested
        if (uploadToQdrant) {
            log.info("Uploading chunks to Qdrant vector database");
            uploadToQdrant(chunks);
        } else {
            log.info("Skipping Qdrant upload");
        }
        
        log.info("PDF processing completed successfully");
    }
    
    private void saveToJsonFile(List<DocumentChunk> chunks) throws IOException {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        java.nio.file.Path targetDir = java.nio.file.Paths.get("target");
        java.nio.file.Files.createDirectories(targetDir);
        java.nio.file.Path outputFile = targetDir.resolve("chunked-output.json");
        
        try (FileWriter writer = new FileWriter(outputFile.toFile())) {
            objectMapper.writeValue(writer, chunks);
        }
        
        log.info("Successfully saved {} chunks to {}", chunks.size(), outputFile);
    }
    
    private void uploadToQdrant(List<DocumentChunk> chunks) throws ExecutionException, InterruptedException {
        // Create collection if it doesn't exist
        qdrantService.createCollectionIfNotExists(getEmbeddingDimensions());
        
        // Generate embeddings
        log.info("Generating embeddings for {} chunks", chunks.size());
        List<float[]> embeddings = embeddingService.generateEmbeddings(chunks);
        
        // Upsert to Qdrant
        log.info("Upserting chunks to Qdrant");
        qdrantService.upsertDocumentChunks(chunks, embeddings);
        
        log.info("Successfully uploaded {} chunks to Qdrant", chunks.size());
    }
}