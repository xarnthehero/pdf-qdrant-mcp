package com.spyder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "")
public class ApplicationProperties {
    
    private Pdf pdf = new Pdf();
    private Embedding embedding = new Embedding();
    private Qdrant qdrant = new Qdrant();
    
    @Data
    public static class Pdf {
        private Chunking chunking = new Chunking();
        
        @Data
        public static class Chunking {
            private int size = 1000;
            private int overlap = 100;
        }
    }
    
    @Data
    public static class Embedding {
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
    
    @Data
    public static class Qdrant {
        private String host = "localhost";
        private int port = 6334;  // gRPC port
        private int restPort = 6333;  // REST API port
        private String collection;
    }
}