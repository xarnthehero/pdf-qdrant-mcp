package com.spyder.pdfprocessing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "pdf")
public class PdfProperties {

    private Chunking chunking = new Chunking();

    @Data
    public static class Chunking {
        private int size;
        private int overlap;
    }

}