package com.spyder.qdrant.service;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.spyder.qdrant.config.EmbeddingProperties;
import com.spyder.qdrant.model.DocumentChunk;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
public class EmbeddingService {
    private final EmbeddingProperties properties;
    private OrtEnvironment environment;
    private OrtSession session;
    private HuggingFaceTokenizer tokenizer;
    
    public EmbeddingService(EmbeddingProperties properties) {
        this.properties = properties;
    }
    
    @PostConstruct
    public void initialize() throws OrtException, IOException {
        log.info("Initializing ONNX embedding service with model: {}", properties.getModel().getName());
        
        // Initialize ONNX Runtime environment
        environment = OrtEnvironment.getEnvironment();
        
        // Load the ONNX model
        Path modelPath = Paths.get(properties.getModel().getOnnxPath());
        if (!Files.exists(modelPath)) {
            throw new RuntimeException("ONNX model file not found at: " + modelPath + 
                "\nPlease see EMBEDDING_SETUP.md for setup instructions.");
        }
        
        session = environment.createSession(modelPath.toString(), new OrtSession.SessionOptions());
        log.info("ONNX model loaded successfully from: {}", modelPath);
        
        // Load the tokenizer
        Path tokenizerPath = Paths.get(properties.getModel().getTokenizerPath());
        if (!Files.exists(tokenizerPath)) {
            throw new RuntimeException("Tokenizer file not found at: " + tokenizerPath + 
                "\nPlease see EMBEDDING_SETUP.md for setup instructions.");
        }
        
        tokenizer = HuggingFaceTokenizer.newInstance(tokenizerPath);
        log.info("Tokenizer loaded successfully from: {}", tokenizerPath);
        
        log.info("Embedding service initialized successfully");
    }
    
    @PreDestroy
    public void cleanup() throws OrtException {
        if (session != null) {
            session.close();
        }
        if (environment != null) {
            environment.close();
        }
        if (tokenizer != null) {
            tokenizer.close();
        }
        log.info("Embedding service cleaned up");
    }
    
    /**
     * Generate embeddings for document chunks using ONNX Runtime.
     */
    public List<float[]> generateEmbeddings(List<DocumentChunk> chunks) {
        log.info("Generating {}-dimensional embeddings for {} chunks using BAAI/bge-small-en-v1.5", 
                properties.getDimensions(), chunks.size());
        
        List<float[]> embeddings = new ArrayList<>();
        
        for (DocumentChunk chunk : chunks) {
            try {
                float[] embedding = generateSingleEmbedding(chunk.getContent());
                embeddings.add(embedding);
            } catch (Exception e) {
                log.error("Failed to generate embedding for chunk {}: {}", chunk.getId(), e.getMessage());
                // Fallback to zero vector if embedding fails
                embeddings.add(new float[properties.getDimensions()]);
            }
        }
        
        log.info("Generated embeddings for {} chunks", chunks.size());
        return embeddings;
    }
    
    /**
     * Generate embedding for a single text query.
     */
    public float[] generateQueryEmbedding(String query) {
        try {
            return generateSingleEmbedding(query);
        } catch (Exception e) {
            log.error("Failed to generate query embedding: {}", e.getMessage());
            return new float[properties.getDimensions()];
        }
    }
    
    /**
     * Generate embedding for a single text using ONNX model.
     */
    private float[] generateSingleEmbedding(String text) throws OrtException {
        // Tokenize the input text
        Encoding encoding = tokenizer.encode(text);
        long[] inputIds = encoding.getIds();
        long[] attentionMask = encoding.getAttentionMask();
        
        // Truncate to max length if necessary
        int maxLength = properties.getModel().getMaxLength();
        if (inputIds.length > maxLength) {
            inputIds = Arrays.copyOf(inputIds, maxLength);
            attentionMask = Arrays.copyOf(attentionMask, maxLength);
        }
        
        // Create token_type_ids (all zeros for single sentence)
        long[] tokenTypeIds = new long[inputIds.length];
        Arrays.fill(tokenTypeIds, 0L);
        
        // Create ONNX tensors
        try (OnnxTensor inputIdsTensor = OnnxTensor.createTensor(environment, new long[][]{inputIds});
             OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(environment, new long[][]{attentionMask});
             OnnxTensor tokenTypeIdsTensor = OnnxTensor.createTensor(environment, new long[][]{tokenTypeIds})) {
            
            // Create input map
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", inputIdsTensor);
            inputs.put("attention_mask", attentionMaskTensor);
            inputs.put("token_type_ids", tokenTypeIdsTensor);
            
            // Run inference
            try (OrtSession.Result results = session.run(inputs)) {
                // Get the last hidden state (typically the first output)
                OnnxTensor outputTensor = (OnnxTensor) results.get(0);
                float[][][] output = (float[][][]) outputTensor.getValue();
                
                // Apply mean pooling over sequence length dimension
                float[] embedding = meanPooling(output[0], attentionMask);
                
                // L2 normalize the embedding
                return l2Normalize(embedding);
            }
        }
    }
    
    /**
     * Apply mean pooling to the token embeddings.
     */
    private float[] meanPooling(float[][] tokenEmbeddings, long[] attentionMask) {
        int embeddingDim = tokenEmbeddings[0].length;
        float[] pooledEmbedding = new float[embeddingDim];
        
        int sumMask = 0;
        for (int i = 0; i < tokenEmbeddings.length; i++) {
            if (attentionMask[i] == 1) {
                for (int j = 0; j < embeddingDim; j++) {
                    pooledEmbedding[j] += tokenEmbeddings[i][j];
                }
                sumMask++;
            }
        }
        
        // Average by the number of non-padded tokens
        if (sumMask > 0) {
            for (int j = 0; j < embeddingDim; j++) {
                pooledEmbedding[j] /= sumMask;
            }
        }
        
        return pooledEmbedding;
    }
    
    /**
     * L2 normalize the embedding vector.
     */
    private float[] l2Normalize(float[] embedding) {
        float norm = 0;
        for (float value : embedding) {
            norm += value * value;
        }
        norm = (float) Math.sqrt(norm);
        
        if (norm > 0) {
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] /= norm;
            }
        }
        
        return embedding;
    }
}