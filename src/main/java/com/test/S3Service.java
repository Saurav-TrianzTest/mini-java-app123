package com.test;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * S3 Service for cloud-native file operations
 * Replaces local file system dependencies with Amazon S3 object storage
 */
public class S3Service {
    
    private final S3Client s3Client;
    private final String bucketName;
    
    public S3Service() {
        // Initialize S3 client with default credentials provider (uses IAM roles, environment variables, etc.)
        this.s3Client = S3Client.builder()
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1")))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        
        // Get bucket name from environment variable
        this.bucketName = System.getenv().getOrDefault("S3_BUCKET_NAME", "mini-app-storage");
    }
    
    /**
     * Read a file from S3 as Properties
     * @param key S3 object key (path)
     * @return Properties object loaded from S3
     */
    public Properties readPropertiesFromS3(String key) {
        Properties props = new Properties();
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            InputStream inputStream = s3Client.getObject(getObjectRequest);
            props.load(inputStream);
            inputStream.close();
            
            System.out.println("Configuration loaded from S3: s3://" + bucketName + "/" + key);
            return props;
        } catch (S3Exception e) {
            System.err.println("Failed to read from S3: " + e.awsErrorDetails().errorMessage());
            return props;
        } catch (IOException e) {
            System.err.println("Failed to load properties from S3: " + e.getMessage());
            return props;
        }
    }
    
    /**
     * Check if an object exists in S3
     * @param key S3 object key (path)
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
        } catch (S3Exception e) {
            System.err.println("Error checking S3 object existence: " + e.awsErrorDetails().errorMessage());
            return false;
        }
    }
    
    /**
     * Write content to S3
     * @param key S3 object key (path)
     * @param content Content to write
     */
    public void writeToS3(String key, String content) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromString(content));
            System.out.println("Content written to S3: s3://" + bucketName + "/" + key);
        } catch (S3Exception e) {
            System.err.println("Failed to write to S3: " + e.awsErrorDetails().errorMessage());
        }
    }
    
    /**
     * Create a log entry in S3
     * @param key S3 object key (path)
     * @param logMessage Log message to append
     */
    public void createLogInS3(String key, String logMessage) {
        try {
            // Check if log file exists
            String existingContent = "";
            if (objectExists(key)) {
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();
                
                InputStream inputStream = s3Client.getObject(getObjectRequest);
                existingContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                inputStream.close();
            }
            
            // Append new log message
            String newContent = existingContent + logMessage + "\n";
            writeToS3(key, newContent);
            
            System.out.println("Log initialized in S3: s3://" + bucketName + "/" + key);
        } catch (S3Exception e) {
            System.err.println("Failed to create log in S3: " + e.awsErrorDetails().errorMessage());
        } catch (IOException e) {
            System.err.println("Failed to read existing log from S3: " + e.getMessage());
        }
    }
    
    /**
     * Close the S3 client
     */
    public void close() {
        if (s3Client != null) {
            s3Client.close();
        }
    }
}
