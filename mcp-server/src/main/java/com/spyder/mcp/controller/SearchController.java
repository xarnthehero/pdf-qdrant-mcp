package com.spyder.mcp.controller;

import com.spyder.mcp.service.QdrantMcpSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final QdrantMcpSearchService searchService;

    @GetMapping("/similar")
    public ResponseEntity<List<Map<String, Object>>> searchSimilar(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "3") Integer limit
    ) {
        try {
            log.info("REST: Searching similar chunks for query: '{}', limit: {}", query, limit);
            List<Map<String, Object>> results = searchService.searchSimilarChunks(query, limit);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("REST: Error searching similar chunks", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/filters")
    public ResponseEntity<List<Map<String, Object>>> searchWithFilters(
            @RequestParam(required = false) String chapter,
            @RequestParam(required = false) String heading,
            @RequestParam(required = false) String subheading,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false, defaultValue = "50") Integer limit
    ) {
        try {
            log.info("REST: Searching with filters - chapter: '{}', heading: '{}', subheading: '{}', page: {}, limit: {}", 
                    chapter, heading, subheading, pageNumber, limit);
            List<Map<String, Object>> results = searchService.searchWithFilters(chapter, heading, subheading, pageNumber, limit);
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            log.error("REST: Invalid parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("REST: Error searching with filters", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/filters")
    public ResponseEntity<List<Map<String, Object>>> searchWithFiltersPost(
            @RequestBody FilterSearchRequest request
    ) {
        try {
            log.info("REST: POST Searching with filters - request: {}", request);
            List<Map<String, Object>> results = searchService.searchWithFilters(
                    request.getChapter(),
                    request.getHeading(),
                    request.getSubheading(),
                    request.getPageNumber(),
                    request.getLimit()
            );
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            log.error("REST: Invalid parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("REST: Error searching with filters", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/similar")
    public ResponseEntity<List<Map<String, Object>>> searchSimilarPost(
            @RequestBody SimilarSearchRequest request
    ) {
        try {
            log.info("REST: POST Searching similar chunks for request: {}", request);
            List<Map<String, Object>> results = searchService.searchSimilarChunks(request.getQuery(), request.getLimit());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("REST: Error searching similar chunks", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // DTO classes
    public static class FilterSearchRequest {
        private String chapter;
        private String heading;
        private String subheading;
        private Integer pageNumber;
        private Integer limit = 50;

        public String getChapter() { return chapter; }
        public void setChapter(String chapter) { this.chapter = chapter; }

        public String getHeading() { return heading; }
        public void setHeading(String heading) { this.heading = heading; }

        public String getSubheading() { return subheading; }
        public void setSubheading(String subheading) { this.subheading = subheading; }

        public Integer getPageNumber() { return pageNumber; }
        public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }

        @Override
        public String toString() {
            return "FilterSearchRequest{" +
                    "chapter='" + chapter + '\'' +
                    ", heading='" + heading + '\'' +
                    ", subheading='" + subheading + '\'' +
                    ", pageNumber=" + pageNumber +
                    ", limit=" + limit +
                    '}';
        }
    }

    public static class SimilarSearchRequest {
        private String query;
        private Integer limit = 3;

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }

        @Override
        public String toString() {
            return "SimilarSearchRequest{" +
                    "query='" + query + '\'' +
                    ", limit=" + limit +
                    '}';
        }
    }
}