package com.spyder;

import com.spyder.service.PdfProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties
@RequiredArgsConstructor
public class PdfProcessingApplication implements CommandLineRunner {
    
    private final PdfProcessingService pdfProcessingService;
    
    public static void main(String[] args) {
        SpringApplication.run(PdfProcessingApplication.class, args);
    }
    
    @Override
    public void run(String... args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            log.error("Usage: java -jar starforge-mcp.jar <path-to-pdf> [--skip-qdrant]");
            System.exit(1);
        }
        
        String pdfPath = args[0];
        boolean skipQdrant = args.length > 1 && "--skip-qdrant".equals(args[1]);
        
        try {
            pdfProcessingService.processPdf(pdfPath, !skipQdrant);
            log.info("Application completed successfully");
        } catch (Exception e) {
            log.error("Error processing PDF: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}