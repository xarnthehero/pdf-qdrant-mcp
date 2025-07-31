package com.spyder.pdfprocessing;

import com.spyder.pdfprocessing.service.DocumentService;
import com.spyder.pdfprocessing.service.PdfProcessingService;
import com.spyder.qdrant.config.EmbeddingProperties;
import com.spyder.qdrant.model.DocumentChunk;
import com.spyder.qdrant.service.EmbeddingService;
import com.spyder.qdrant.service.QdrantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.List;

@Slf4j
@SpringBootApplication(
        scanBasePackages = {"com.spyder"}
)
@EnableConfigurationProperties
@RequiredArgsConstructor
public class PdfProcessingApplication implements CommandLineRunner {

    private final QdrantService qdrantService;
    private final PdfProcessingService pdfProcessingService;
    private final EmbeddingProperties embeddingProperties;
    private final EmbeddingService embeddingService;
    private final DocumentService documentService;

    public static void main(String[] args) {
        SpringApplication.run(PdfProcessingApplication.class, args);
    }
    
    @Override
    public void run(String... args) {
        if (args.length < 1 || args.length > 2) {
            log.error("Usage: java -jar starforge-mcp.jar <path-to-pdf> [--skip-qdrant]");
            System.exit(1);
        }
        
        String pdfPath = args[0];
        boolean skipQdrant = args.length > 1 && "--skip-qdrant".equals(args[1]);

        try {
            List<DocumentChunk> documentChunks = pdfProcessingService.processPdf(pdfPath);
            if(!skipQdrant) {
                List<float[]> embeddings = embeddingService.generateEmbeddings(documentChunks);
                qdrantService.createCollectionIfNotExists(embeddingProperties);
                documentService.upsertDocumentChunks(documentChunks,  embeddings);
            }
            log.info("Application completed successfully");
        } catch (Exception e) {
            log.error("Error processing PDF: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}