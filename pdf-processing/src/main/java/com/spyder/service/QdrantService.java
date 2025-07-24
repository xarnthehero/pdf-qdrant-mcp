package com.spyder.service;

import com.spyder.model.DocumentChunk;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.CreateCollection;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.UpsertPoints;
import com.spyder.config.ApplicationProperties;
import io.qdrant.client.grpc.JsonWithInt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class QdrantService {
    private final QdrantClient client;
    private final ApplicationProperties properties;
    
    public QdrantService(ApplicationProperties properties) {
        this.properties = properties;
        ApplicationProperties.Qdrant qdrantConfig = properties.getQdrant();
        this.client = new QdrantClient(QdrantGrpcClient.newBuilder(
            qdrantConfig.getHost(), 
            qdrantConfig.getPort(), 
            false
        ).build());
    }
    
    private String getCollectionName() {
        return properties.getQdrant().getCollection();
    }
    
    public void createCollectionIfNotExists(int vectorSize) throws ExecutionException, InterruptedException {
        try {
            // Try to get collection info first
            client.getCollectionInfoAsync(getCollectionName()).get();
            log.info("Collection '{}' already exists", getCollectionName());
        } catch (Exception e) {
            // Collection doesn't exist, create it using REST API since Java client doesn't support named vectors properly
            log.info("Creating collection '{}' with named vector via REST API", getCollectionName());
            createCollectionViaRest(vectorSize);
        }
    }
    
    private void createCollectionViaRest(int vectorSize) {
        try {
            String jsonPayload = String.format("""
                {
                  "vectors": {
                    "fast-bge-small-en-v1.5": {
                      "size": %d,
                      "distance": "Cosine"
                    }
                  }
                }
                """, vectorSize);
            
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(String.format("http://%s:%d/collections/%s", 
                    properties.getQdrant().getHost(), 
                    properties.getQdrant().getRestPort(), 
                    getCollectionName())))
                .header("Content-Type", "application/json")
                .PUT(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
            
            java.net.http.HttpResponse<String> response = httpClient.send(request, 
                java.net.http.HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                log.info("Collection '{}' created successfully with named vector", getCollectionName());
            } else {
                throw new RuntimeException("Failed to create collection: " + response.body());
            }
        } catch (Exception ex) {
            log.error("Failed to create collection via REST API: {}", ex.getMessage());
            throw new RuntimeException("Collection creation failed", ex);
        }
    }
    
    public void upsertDocumentChunks(List<DocumentChunk> chunks, List<float[]> embeddings)
            throws ExecutionException, InterruptedException {
        
        if (chunks.size() != embeddings.size()) {
            throw new IllegalArgumentException("Number of chunks must match number of embeddings");
        }
        
        log.info("Upserting {} document chunks to Qdrant", chunks.size());
        
        PointStruct.Builder[] points = new PointStruct.Builder[chunks.size()];
        
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            float[] embedding = embeddings.get(i);
            
            // Convert float array to Qdrant vector format
            java.util.List<Float> vectorData = new java.util.ArrayList<>();
            for (float f : embedding) {
                vectorData.add(f);
            }
            io.qdrant.client.grpc.Points.Vector vector = io.qdrant.client.grpc.Points.Vector.newBuilder()
                .addAllData(vectorData)
                .build();
            
            // Build payload with metadata using Qdrant's JsonWithInt.Value
            java.util.Map<String, JsonWithInt.Value> payload = new java.util.HashMap<>();
            payload.put("content", JsonWithInt.Value.newBuilder().setStringValue(chunk.getContent()).build());
            payload.put("source", JsonWithInt.Value.newBuilder().setStringValue(chunk.getMetadata().getSource()).build());
            payload.put("page_number", JsonWithInt.Value.newBuilder().setIntegerValue(chunk.getMetadata().getPageNumber()).build());
            payload.put("chunk_index", JsonWithInt.Value.newBuilder().setIntegerValue(chunk.getMetadata().getChunkIndex()).build());
            payload.put("content_length", JsonWithInt.Value.newBuilder().setIntegerValue(chunk.getMetadata().getContentLength()).build());
            payload.put("document_type", JsonWithInt.Value.newBuilder().setStringValue(chunk.getMetadata().getDocumentType()).build());
            
            // Add hierarchy fields if they exist
            if (chunk.getMetadata().getChapter() != null) {
                payload.put("chapter", JsonWithInt.Value.newBuilder().setStringValue(chunk.getMetadata().getChapter()).build());
            }
            if (chunk.getMetadata().getHeading() != null) {
                payload.put("heading", JsonWithInt.Value.newBuilder().setStringValue(chunk.getMetadata().getHeading()).build());
            }
            if (chunk.getMetadata().getSubheading() != null) {
                payload.put("subheading", JsonWithInt.Value.newBuilder().setStringValue(chunk.getMetadata().getSubheading()).build());
            }

            // Create named vector for upload
            Points.NamedVectors namedVectors = Points.NamedVectors.newBuilder()
                .putVectors("fast-bge-small-en-v1.5", vector)
                .build();
            
            Points.Vectors vectors = Points.Vectors.newBuilder()
                .setVectors(namedVectors)
                .build();
            
            points[i] = PointStruct.newBuilder()
                .setId(io.qdrant.client.grpc.Points.PointId.newBuilder().setUuid(chunk.getId()).build())
                .setVectors(vectors)
                .putAllPayload(payload);
        }
        
        UpsertPoints upsertPoints = UpsertPoints.newBuilder()
            .setCollectionName(getCollectionName())
            .addAllPoints(java.util.Arrays.stream(points)
                .map(PointStruct.Builder::build)
                .collect(java.util.stream.Collectors.toList()))
            .build();
        
        client.upsertAsync(upsertPoints).get();
        log.info("Successfully upserted {} chunks to collection '{}'", chunks.size(), getCollectionName());
    }
    
    public void deleteCollection() throws ExecutionException, InterruptedException {
        try {
            client.deleteCollectionAsync(getCollectionName()).get();
            log.info("Collection '{}' deleted successfully", getCollectionName());
        } catch (Exception e) {
            log.warn("Failed to delete collection '{}': {}", getCollectionName(), e.getMessage());
        }
    }
    
    public void close() {
        client.close();
    }
}