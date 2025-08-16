package com.spyder.mcp.service;

import com.spyder.qdrant.service.EmbeddingService;
import com.spyder.qdrant.service.QdrantService;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class QdrantMcpSearchService {

    private final QdrantService qdrantService;
    private final EmbeddingService embeddingService;

    @Tool(
            name = "search_similar_chunks",
            description = "Semantic search qdrant vector database"
    )
    public List<Map<String, Object>> searchSimilarChunks(
            @ToolParam(description = "The query string to search in the vector db") String query,
            @ToolParam(description = "Limit on result count", required = false) Integer limit
    ) {
        try {
            // Set default limit to 3 if not provided
            int searchLimit = Optional.ofNullable(limit).orElse(3);
            
            log.info("Searching for similar chunks with query: '{}', limit: {}", query, searchLimit);
            
            // Generate embedding for the query
            float[] queryEmbedding = embeddingService.generateQueryEmbedding(query);
            
            // Search for similar vectors in Qdrant
            List<Points.ScoredPoint> results = qdrantService.searchSimilarVectors(queryEmbedding, searchLimit);
            
            // Convert results to the expected format
            List<Map<String, Object>> formattedResults = new ArrayList<>();
            for (Points.ScoredPoint point : results) {
                formattedResults.add(convertPointToMap(point));
            }
            
            // Sort by score (highest first) for relevance-based ordering
            formattedResults.sort(sortByScoreComparator());

            log.info("Found {} similar chunks for query: '{}'", formattedResults.size(), query);
            return formattedResults;
            
        } catch (Exception e) {
            log.error("Failed to search for similar chunks with query: '{}', error: {}", query, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private Comparator<Map<String, Object>> sortByScoreComparator() {
        Comparator<Map<String, Object>> ascendingOrderComparator = Comparator.comparingDouble(value -> Optional.ofNullable((float)value.get("score"))
                .orElseThrow(() -> new RuntimeException("No score found")));
        return ascendingOrderComparator.reversed();
    }

    @Tool(
            name = "search_with_filters",
            description = "Search for document chunks based on metadata filters (chapter, heading, subheading, page number)"
    )
    public List<Map<String, Object>> searchWithFilters(
            @ToolParam(description = "Chapter name to filter by", required = false) String chapter,
            @ToolParam(description = "Heading text to filter by", required = false) String heading,
            @ToolParam(description = "Subheading text to filter by", required = false) String subheading,
            @ToolParam(description = "Page number to filter by", required = false) Integer pageNumber,
            @ToolParam(description = "Limit on result count", required = false) Integer limit
    ) {
        try {
            // Validate that at least one filter is provided
            if ((chapter == null || chapter.trim().isEmpty()) && 
                (heading == null || heading.trim().isEmpty()) && 
                (subheading == null || subheading.trim().isEmpty()) && 
                pageNumber == null) {
                throw new IllegalArgumentException("At least one filter parameter (chapter, heading, subheading, pageNumber) must be provided");
            }
            
            // Set default limit to 50 if not provided (higher than semantic search since we're filtering)
            int searchLimit = Optional.ofNullable(limit).orElse(50);
            
            log.info("Searching with filters - chapter: '{}', heading: '{}', subheading: '{}', page: {}, limit: {}", 
                    chapter, heading, subheading, pageNumber, searchLimit);
            
            // Search with filters using QdrantService
            List<Points.RetrievedPoint> results = qdrantService.searchWithFilters(chapter, heading, subheading, pageNumber, searchLimit);
            
            // Convert results to the expected format
            List<Map<String, Object>> formattedResults = new ArrayList<>();
            for (Points.RetrievedPoint point : results) {
                formattedResults.add(convertRetrievedPointToMap(point));
            }
            
            // Sort by chunk index for better readability (especially important for filtered searches)
            sortByChunkIndex(formattedResults);
            
            log.info("Found {} filtered chunks", formattedResults.size());
            return formattedResults;
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid filter parameters: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to search with filters - chapter: '{}', heading: '{}', subheading: '{}', page: {}, error: {}", 
                    chapter, heading, subheading, pageNumber, e.getMessage(), e);
            return new ArrayList<>();
        }
    }


    private Map<String, Object> convertPointToMap(Points.ScoredPoint point) {
        return qdrantService.convertPointToMap(point);
    }
    
    private Map<String, Object> convertRetrievedPointToMap(Points.RetrievedPoint point) {
        Map<String, Object> result = new HashMap<>();
        
        // RetrievedPoint doesn't have a score since it's from filtered search, not similarity search
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
     * Sort search results by chunk index for better readability.
     * This is especially important for filtered searches to maintain document order.
     */
    private void sortByChunkIndex(List<Map<String, Object>> results) {
        results.sort((a, b) -> {
            try {
                // Extract chunk index from payload
                Map<String, Object> payloadA = (Map<String, Object>) a.get("payload");
                Map<String, Object> payloadB = (Map<String, Object>) b.get("payload");
                
                Integer chunkIndexA = extractChunkIndex(payloadA);
                Integer chunkIndexB = extractChunkIndex(payloadB);
                
                // Handle cases where chunk index might be missing
                if (chunkIndexA == null && chunkIndexB == null) return 0;
                if (chunkIndexA == null) return 1;  // Put missing indices at the end
                if (chunkIndexB == null) return -1;
                
                return Integer.compare(chunkIndexA, chunkIndexB);
                
            } catch (Exception e) {
                log.warn("Error sorting by chunk index: {}", e.getMessage());
                return 0; // Keep original order if sorting fails
            }
        });
    }

    /**
     * Extract chunk index from payload, trying different possible field names.
     */
    private Integer extractChunkIndex(Map<String, Object> payload) {
        if (payload == null) return null;
        
        // Try different possible field names for chunk index
        Object chunkIndex = payload.get("chunkIndex");
        if (chunkIndex == null) chunkIndex = payload.get("chunk_index");
        if (chunkIndex == null) chunkIndex = payload.get("index");
        if (chunkIndex == null) chunkIndex = payload.get("order");
        if (chunkIndex == null) chunkIndex = payload.get("sequence");
        
        if (chunkIndex instanceof Integer) {
            return (Integer) chunkIndex;
        } else if (chunkIndex instanceof Long) {
            return ((Long) chunkIndex).intValue();
        } else if (chunkIndex instanceof String) {
            try {
                return Integer.parseInt((String) chunkIndex);
            } catch (NumberFormatException e) {
                log.debug("Could not parse chunk index from string: {}", chunkIndex);
                return null;
            }
        }
        
        return null;
    }
}