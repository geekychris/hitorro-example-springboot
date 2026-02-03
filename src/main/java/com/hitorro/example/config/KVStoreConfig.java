package com.hitorro.example.config;

import com.hitorro.kvstore.DatabaseConfig;
import com.hitorro.kvstore.KVStore;
import com.hitorro.kvstore.RocksDBStore;
import com.hitorro.kvstore.config.CompressionType;
import com.hitorro.kvstore.config.StorageMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Spring Boot configuration for RocksDB-based key-value store.
 * Provides document storage alongside Lucene indexing.
 * 
 * Enabled when hitorro.kvstore.enabled=true (default: false)
 */
@Configuration
@ConditionalOnProperty(name = "hitorro.kvstore.enabled", havingValue = "true", matchIfMissing = false)
public class KVStoreConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(KVStoreConfig.class);
    
    @Value("${hitorro.kvstore.path:${java.io.tmpdir}/hitorro-kvstore}")
    private String kvStorePath;
    
    @Value("${hitorro.kvstore.compression:SNAPPY}")
    private String compressionType;
    
    @Value("${hitorro.kvstore.writeBufferSize:67108864}")
    private long writeBufferSize;
    
    @Value("${hitorro.kvstore.maxOpenFiles:1000}")
    private int maxOpenFiles;
    
    private KVStore documentStore;
    
    /**
     * Creates the primary document store bean.
     * Documents are stored with their ID as the key and full JSON as the value.
     */
    @Bean(name = "documentStore")
    public KVStore documentStore() {
        try {
            logger.info("Initializing RocksDB document store at: {}", kvStorePath);
            
            // Parse compression type
            CompressionType compression;
            try {
                compression = CompressionType.valueOf(compressionType.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid compression type '{}', defaulting to SNAPPY", compressionType);
                compression = CompressionType.SNAPPY;
            }
            
            // Build database configuration
            DatabaseConfig config = DatabaseConfig.builder(kvStorePath)
                    .storageMode(StorageMode.DISK)
                    .compressionType(compression)
                    .createIfMissing(true)
                    .writeBufferSize(writeBufferSize)
                    .build();
            
            documentStore = new RocksDBStore(config);
            
            logger.info("Document store initialized successfully");
            logger.info("  Path: {}", kvStorePath);
            logger.info("  Compression: {}", compression);
            logger.info("  Write Buffer: {} bytes ({} MB)", writeBufferSize, writeBufferSize / 1024 / 1024);
            logger.info("  Max Open Files: {}", maxOpenFiles);
            
            return documentStore;
            
        } catch (Exception e) {
            logger.error("Failed to initialize document store", e);
            throw new RuntimeException("Failed to initialize KVStore", e);
        }
    }
    
    /**
     * Cleanup on application shutdown.
     */
    @PreDestroy
    public void closeStore() {
        if (documentStore != null) {
            try {
                logger.info("Closing document store...");
                documentStore.close();
                logger.info("Document store closed successfully");
            } catch (Exception e) {
                logger.error("Error closing document store", e);
            }
        }
    }
}
