package com.spyder.pdfprocessing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "")
public class ApplicationProperties {
    
    private Pdf pdf = new Pdf();
    
    @Data
    public static class Pdf {
        private Chunking chunking = new Chunking();
        
        @Data
        public static class Chunking {
            private int size = 1000;
            private int overlap = 100;
        }
    }
    
}