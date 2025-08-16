package com.spyder.qdrant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingProperties {
    private int dimensions = 384;
    private Model model = new Model();
    
    @Data
    public static class Model {
        private String name;
        private String onnxPath;
        private String tokenizerPath = "models/tokenizer.json";
        private int maxLength = 512;
    }
}