package com.test.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

/**
 * Cloud-native storage service using Amazon S3
 * Replaces local file system operations with S3 object storage
 */
public class S3StorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);
    
    private final S3Client s3Client;
    private final String bucketName;
    
    public S3StorageService(String bucketName) {
        // Get AWS region from environment variable or default to us-east-1
        String regionName = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
        Region region = Region.of(regionName);
        
        // Initialize S3 client with proper timeouts
        this.s3Client = S3Client.builder()
                .region(region)
                .overrideConfiguration(config -> config
                        .apiCallTimeout(Duration.ofSeconds(30))
                        .apiCallAttemptTimeout(Duration.ofSeconds(10)))
                .build();
        
        this.bucketName = bucketName;
        
        logger.info("S3StorageService initialized for bucket: {} in region: {}", bucketName, regionName);
    }
    
    /**
     * Load properties from S3 object
     * @param key The S3 object key (path)
     * @return Properties object loaded from S3
     */
    public Properties loadPropertiesFromS3(String key) {
        try {
            logger.info("Loading properties from S3: s3://{}/{}", bucketName, key);
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            InputStream inputStream = s3Client.getObject(getObjectRequest, ResponseTransformer.toInputStream());
            
            Properties properties = new Properties();
            properties.load(inputStream);
            
            logger.info("Successfully loaded properties from S3: {}", key);
            return properties;
            
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                logger.warn("Properties file not found in S3: s3://{}/{}", bucketName, key);
                return new Properties(); // Return empty properties
            }
            logger.error("Failed to load properties from S3: {}", key, e);
            throw new RuntimeException("Failed to load properties from S3: " + key, e);
        } catch (Exception e) {
            logger.error("Failed to load properties from S3: {}", key, e);
            throw new RuntimeException("Failed to load properties from S3: " + key, e);
        }
    }
    
    /**
     * Write text content to S3
     * @param key The S3 object key (path)
     * @param content The content to write
     */
    public void writeToS3(String key, String content) {
        try {
            logger.info("Writing content to S3: s3://{}/{}", bucketName, key);
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("text/plain")
                    .build();
            
            s3Client.putObject(putObjectRequest, 
                    RequestBody.fromString(content, StandardCharsets.UTF_8));
            
            logger.info("Successfully wrote content to S3: {}", key);
            
        } catch (Exception e) {
            logger.error("Failed to write to S3: {}", key, e);
            throw new RuntimeException("Failed to write to S3: " + key, e);
        }
    }
    
    /**
     * Write byte array to S3
     * @param key The S3 object key (path)
     * @param data The byte array to write
     */
    public void writeToS3(String key, byte[] data) {
        try {
            logger.info("Writing binary data to S3: s3://{}/{}", bucketName, key);
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("application/octet-stream")
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(data));
            
            logger.info("Successfully wrote binary data to S3: {}", key);
            
        } catch (Exception e) {
            logger.error("Failed to write binary data to S3: {}", key, e);
            throw new RuntimeException("Failed to write binary data to S3: " + key, e);
        }
    }
    
    /**
     * Read text content from S3
     * @param key The S3 object key (path)
     * @return The content as a string
     */
    public String readFromS3(String key) {
        try {
            logger.info("Reading content from S3: s3://{}/{}", bucketName, key);
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            String content = s3Client.getObject(getObjectRequest, ResponseTransformer.toBytes())
                    .asUtf8String();
            
            logger.info("Successfully read content from S3: {}", key);
            return content;
            
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                logger.warn("Object not found in S3: s3://{}/{}", bucketName, key);
                return null;
            }
            logger.error("Failed to read from S3: {}", key, e);
            throw new RuntimeException("Failed to read from S3: " + key, e);
        } catch (Exception e) {
            logger.error("Failed to read from S3: {}", key, e);
            throw new RuntimeException("Failed to read from S3: " + key, e);
        }
    }
    
    /**
     * Check if object exists in S3
     * @param key The S3 object key (path)
     * @return true if object exists, false otherwise
     */
    public boolean objectExists(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            s3Client.headObject(headObjectRequest);
            return true;
            
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            logger.error("Failed to check object existence in S3: {}", key, e);
            return false;
        }
    }
    
    /**
     * Delete object from S3
     * @param key The S3 object key (path)
     */
    public void deleteFromS3(String key) {
        try {
            logger.info("Deleting object from S3: s3://{}/{}", bucketName, key);
            
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            s3Client.deleteObject(deleteObjectRequest);
            
            logger.info("Successfully deleted object from S3: {}", key);
            
        } catch (Exception e) {
            logger.error("Failed to delete from S3: {}", key, e);
            throw new RuntimeException("Failed to delete from S3: " + key, e);
        }
    }
    
    /**
     * Close S3 client
     */
    public void close() {
        if (s3Client != null) {
            s3Client.close();
            logger.info("S3StorageService closed");
        }
    }
}
