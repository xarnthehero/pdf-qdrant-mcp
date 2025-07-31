package com.spyder.mcp.tools;

import com.spyder.mcp.service.QdrantMcpSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class QdrantTools {

    private final QdrantMcpSearchService qdrantMcpSearchService;

    @Tool(
            name = "qdrant-find",
            description = "Search for relevant document chunks in the Qdrant vector database using semantic similarity"
    )
    public List<Map<String, Object>> searchDocuments(
            @ToolParam(description = "The search query to find relevant document chunks") String query,
            @ToolParam(description = "Maximum number of results to return (default: 5)") Integer limit) {
        
        if (limit == null) limit = 5;
        
        log.info("Searching Qdrant for query: '{}' with limit: {}", query, limit);
        return qdrantMcpSearchService.searchSimilarChunks(query, limit);
    }

    @Tool(
            name = "qdrant-filter",
            description = "Search for document chunks with specific metadata filters (e.g., chapter, page number)"
    )
    public List<Map<String, Object>> searchWithFilters(
            @ToolParam(description = "The search query") String query,
            @ToolParam(description = "Chapter name to filter by") String chapter,
            @ToolParam(description = "Page number to filter by (0 for no filter)") Integer pageNumber,
            @ToolParam(description = "Maximum number of results to return (default: 5)") Integer limit) {
        
        if (chapter == null) chapter = "";
        if (pageNumber == null) pageNumber = 0;
        if (limit == null) limit = 5;
        
        log.info("Searching Qdrant with filters - query: '{}', chapter: '{}', page: {}, limit: {}", 
                query, chapter, pageNumber, limit);
        return qdrantMcpSearchService.searchWithFilters(query, chapter, pageNumber, limit);
    }
}