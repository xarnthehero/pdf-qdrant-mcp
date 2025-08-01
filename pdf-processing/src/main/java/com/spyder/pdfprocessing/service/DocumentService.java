package com.spyder.pdfprocessing.service;

import com.spyder.qdrant.model.DocumentChunk;
import com.spyder.qdrant.service.QdrantService;
import io.qdrant.client.grpc.Points.PointStruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {
    private final QdrantService qdrantService;

    public void upsertDocumentChunks(List<DocumentChunk> chunks, List<float[]> embeddings)
            throws ExecutionException, InterruptedException {
        
        if (chunks.size() != embeddings.size()) {
            throw new IllegalArgumentException("Number of chunks must match number of embeddings");
        }
        
        log.info("Clearing existing points from collection before inserting new document");
        qdrantService.clearAllPoints();
        
        log.info("Upserting {} document chunks to Qdrant", chunks.size());
        
        List<PointStruct> points = new java.util.ArrayList<>();
        
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            float[] embedding = embeddings.get(i);
            
            // Build metadata map
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", chunk.getMetadata().getSource());
            metadata.put("page_number", chunk.getMetadata().getPageNumber());
            metadata.put("chunk_index", chunk.getMetadata().getChunkIndex());
            metadata.put("content_length", chunk.getMetadata().getContentLength());
            metadata.put("document_type", chunk.getMetadata().getDocumentType());
            
            // Add hierarchy fields if they exist
            if (chunk.getMetadata().getChapter() != null) {
                metadata.put("chapter", chunk.getMetadata().getChapter());
            }
            if (chunk.getMetadata().getHeading() != null) {
                metadata.put("heading", chunk.getMetadata().getHeading());
            }
            if (chunk.getMetadata().getSubheading() != null) {
                metadata.put("subheading", chunk.getMetadata().getSubheading());
            }
            
            PointStruct point = qdrantService.createDocumentPoint(
                chunk.getId(), 
                chunk.getContent(), 
                embedding, 
                metadata
            );
            points.add(point);
        }
        
        qdrantService.upsertPoints(points);
    }
}