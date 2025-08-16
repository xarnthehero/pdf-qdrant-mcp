package com.spyder.qdrant.service;

import com.spyder.qdrant.config.EmbeddingProperties;
import com.spyder.qdrant.config.QdrantProperties;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.CreateCollection;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Collections.VectorsConfig;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class QdrantService {
    
    private final QdrantClient client;
    private final QdrantProperties properties;
    
    public void createCollectionIfNotExists(EmbeddingProperties embeddingProperties) {
        try {
            client.getCollectionInfoAsync(properties.getCollection()).get();
            log.info("Collection '{}' already exists", properties.getCollection());
        } catch (Exception e) {
            log.info("Creating collection '{}'", properties.getCollection());
            createCollection(embeddingProperties.getDimensions());
        }
    }
    
    private void createCollection(int vectorSize) {
        try {
            VectorParams vectorParams = VectorParams.newBuilder()
                .setSize(vectorSize)
                .setDistance(Distance.Cosine)
                .build();
            
            VectorsConfig vectorsConfig = VectorsConfig.newBuilder()
                .setParams(vectorParams)
                .build();
            
            CreateCollection createCollection = CreateCollection.newBuilder()
                .setCollectionName(properties.getCollection())
                .setVectorsConfig(vectorsConfig)
                .build();
            
            client.createCollectionAsync(createCollection).get();
            log.info("Collection '{}' created successfully", properties.getCollection());
            
        } catch (Exception ex) {
            log.error("Failed to create collection via client library: {}", ex.getMessage());
            throw new RuntimeException("Collection creation failed", ex);
        }
    }
    
    public void upsertPoints(List<PointStruct> points) throws ExecutionException, InterruptedException {
        UpsertPoints upsertPoints = UpsertPoints.newBuilder()
            .setCollectionName(properties.getCollection())
            .addAllPoints(points)
            .build();
        
        client.upsertAsync(upsertPoints).get();
        log.info("Successfully upserted {} points to collection '{}'", points.size(), properties.getCollection());
    }
    
    /**
     * Create a PointStruct from document chunk data with embeddings.
     */
    public PointStruct createDocumentPoint(String chunkId, String content, float[] embedding, Map<String, Object> metadata) {
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
        payload.put("content", JsonWithInt.Value.newBuilder().setStringValue(content).build());
        
        // Add metadata fields
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String) {
                payload.put(key, JsonWithInt.Value.newBuilder().setStringValue((String) value).build());
            } else if (value instanceof Integer) {
                payload.put(key, JsonWithInt.Value.newBuilder().setIntegerValue((Integer) value).build());
            } else if (value instanceof Long) {
                payload.put(key, JsonWithInt.Value.newBuilder().setIntegerValue(((Long) value).intValue()).build());
            } else if (value instanceof Double) {
                payload.put(key, JsonWithInt.Value.newBuilder().setDoubleValue((Double) value).build());
            } else if (value instanceof Boolean) {
                payload.put(key, JsonWithInt.Value.newBuilder().setBoolValue((Boolean) value).build());
            }
        }

        // Create vector for upload
        Points.Vectors vectors = Points.Vectors.newBuilder()
            .setVector(vector)
            .build();
        
        return PointStruct.newBuilder()
            .setId(io.qdrant.client.grpc.Points.PointId.newBuilder().setUuid(chunkId).build())
            .setVectors(vectors)
            .putAllPayload(payload)
            .build();
    }

    public void clearAllPoints() throws ExecutionException, InterruptedException {
        try {
            DeletePoints deletePoints = DeletePoints.newBuilder()
                .setCollectionName(properties.getCollection())
                .setPoints(Points.PointsSelector.newBuilder()
                    .setFilter(Filter.newBuilder().build())
                    .build())
                .build();
            
            client.deleteAsync(deletePoints).get();
            log.info("Successfully cleared all points from collection '{}'", properties.getCollection());
        } catch (Exception e) {
            log.error("Failed to clear points from collection '{}': {}", properties.getCollection(), e.getMessage());
            throw e;
        }
    }
    
    public Map<String, Object> convertPointToMap(Points.ScoredPoint point) {
        Map<String, Object> result = new HashMap<>();
        
        result.put("score", point.getScore());
        result.put("id", point.getId().getUuid());
        
        Map<String, Object> payload = new HashMap<>();
        for (Map.Entry<String, JsonWithInt.Value> entry : point.getPayloadMap().entrySet()) {
            JsonWithInt.Value value = entry.getValue();
            if (value.hasStringValue()) {
                payload.put(entry.getKey(), value.getStringValue());
            } else if (value.hasIntegerValue()) {
                payload.put(entry.getKey(), value.getIntegerValue());
            } else if (value.hasDoubleValue()) {
                payload.put(entry.getKey(), value.getDoubleValue());
            } else if (value.hasBoolValue()) {
                payload.put(entry.getKey(), value.getBoolValue());
            }
        }
        result.put("payload", payload);
        
        return result;
    }

    /**
     * Search for similar vectors in the collection.
     */
    public List<Points.ScoredPoint> searchSimilarVectors(float[] queryVector, int limit) throws ExecutionException, InterruptedException {
        // Convert float array to Qdrant vector format
        List<Float> vectorData = new ArrayList<>();
        for (float f : queryVector) {
            vectorData.add(f);
        }
        
        // Create vector for search
        SearchPoints searchPoints = SearchPoints.newBuilder()
            .setCollectionName(properties.getCollection())
            .addAllVector(vectorData)
            .setLimit(limit)
            .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
            .build();
        
        return client.searchAsync(searchPoints).get();
    }
    
    /**
     * Search points based on metadata filters using scroll API for better performance.
     */
    public List<Points.RetrievedPoint> searchWithFilters(String chapter, String heading, String subheading, Integer pageNumber, int limit) throws ExecutionException, InterruptedException {
        List<Condition> conditions = new ArrayList<>();
        
        // Add conditions based on provided filters
        if (chapter != null && !chapter.trim().isEmpty()) {
            conditions.add(Condition.newBuilder()
                .setField(FieldCondition.newBuilder()
                    .setKey("chapter")
                    .setMatch(Match.newBuilder().setText(chapter).build())
                    .build())
                .build());
        }

        if (heading != null && !heading.trim().isEmpty()) {
            conditions.add(Condition.newBuilder()
                .setField(FieldCondition.newBuilder()
                    .setKey("heading")
                    .setMatch(Match.newBuilder().setText(heading).build())
                    .build())
                .build());
        }

        if (subheading != null && !subheading.trim().isEmpty()) {
            conditions.add(Condition.newBuilder()
                .setField(FieldCondition.newBuilder()
                    .setKey("subheading")
                    .setMatch(Match.newBuilder().setText(subheading).build())
                    .build())
                .build());
        }

        if (pageNumber != null) {
            conditions.add(Condition.newBuilder()
                .setField(FieldCondition.newBuilder()
                    .setKey("page_number")
                    .setMatch(Match.newBuilder().setInteger(pageNumber.longValue()).build())
                    .build())
                .build());
        }
        
        Filter filter = Filter.newBuilder()
            .addAllMust(conditions)
            .build();
        
        ScrollPoints scrollPoints = ScrollPoints.newBuilder()
            .setCollectionName(properties.getCollection())
            .setFilter(filter)
            .setLimit(limit)
            .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
            .build();
        
        return client.scrollAsync(scrollPoints).get().getResultList();
    }

}