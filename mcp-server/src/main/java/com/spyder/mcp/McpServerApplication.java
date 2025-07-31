package com.spyder.mcp;

import com.spyder.mcp.service.QdrantMcpSearchService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@ComponentScan(
        basePackages = {"com.spyder"}
)
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider qdrantToolProvider(QdrantMcpSearchService qdrantMcpSearchService) {
        return MethodToolCallbackProvider.builder().toolObjects(qdrantMcpSearchService).build();
    }

}