package com.spyder.qdrant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingProperties {
    private int dimensions = 384;
    private Model model = new Model();
    
    @Data
    public static class Model {
        private String name = "BAAI/bge-small-en-v1.5";
        private String onnxPath = "models/bge-small-en-v1.5.onnx";
        private String tokenizerPath = "models/tokenizer.json";
        private int maxLength = 512;
    }
}