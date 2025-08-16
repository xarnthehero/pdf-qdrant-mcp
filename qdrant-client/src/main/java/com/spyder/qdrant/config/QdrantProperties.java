package com.spyder.qdrant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "qdrant")
@Component
@Data
public class QdrantProperties {
    private String host = "localhost";
    private int port = 6334;
    private int restPort = 6333;
    private String collection = "starforged";
}