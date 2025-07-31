package com.spyder.mcp.service;

import com.spyder.qdrant.service.QdrantService;
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

    @Tool(
            name = "search_similar_chunks",
            description = "Semantic search qdrant vector database"
    )
    public List<Map<String, Object>> searchSimilarChunks(
            @ToolParam(description = "The query string to search in the vector db") String query,
            @ToolParam(description = "Limit on result count", required = false) Integer limit
    ) {
        // TODO: Implement embedding-based search
        log.warn("Embedding-based search not yet implemented for query: '{}'", query);

        return Arrays.asList(
                Map.of("TestKey1", "val1")
        );
    }

    public List<Map<String, Object>> searchWithFilters(String query, String chapter, Integer pageNumber, Integer limit) {
        // TODO: Implement filtered search
        log.warn("Filtered search not yet implemented for query: '{}', chapter: '{}', page: {}",
                query, chapter, pageNumber);
        return new ArrayList<>();
    }


    private Map<String, Object> convertPointToMap(Points.ScoredPoint point) {
        return qdrantService.convertPointToMap(point);
    }
}