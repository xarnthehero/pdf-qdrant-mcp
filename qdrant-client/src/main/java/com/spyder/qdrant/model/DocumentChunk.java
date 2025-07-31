package com.spyder.qdrant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class DocumentChunk {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("metadata")
    private Metadata metadata;
    
    public DocumentChunk(String content, String source, int pageNumber, String chapter, String heading, String subheading, int chunkIndex) {
        this.id = UUID.randomUUID().toString();
        this.content = content;
        this.metadata = new Metadata(source, pageNumber, chapter, heading, subheading, chunkIndex, content.length());
    }
    
    @Data
    @NoArgsConstructor
    public static class Metadata {
        @JsonProperty("source")
        private String source;
        
        @JsonProperty("page_number")
        private int pageNumber;
        
        @JsonProperty("chapter")
        private String chapter;
        
        @JsonProperty("heading")
        private String heading;
        
        @JsonProperty("subheading")
        private String subheading;
        
        @JsonProperty("chunk_index")
        private int chunkIndex;
        
        @JsonProperty("content_length")
        private int contentLength;
        
        @JsonProperty("document_type")
        private String documentType;
        
        public Metadata(String source, int pageNumber, String chapter, String heading, String subheading, int chunkIndex, int contentLength) {
            this.source = source;
            this.pageNumber = pageNumber;
            this.chapter = chapter;
            this.heading = heading;
            this.subheading = subheading;
            this.chunkIndex = chunkIndex;
            this.contentLength = contentLength;
            this.documentType = "pdf";
        }
    }
}