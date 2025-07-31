package com.spyder.qdrant.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(QdrantProperties.class)
@RequiredArgsConstructor
public class QdrantClientConfig {

    private final QdrantProperties properties;

    @Bean
    public QdrantClient qdrantClient() {
        return new QdrantClient(QdrantGrpcClient.newBuilder(
            properties.getHost(),
            properties.getPort(),
            false
        ).build());
    }
}