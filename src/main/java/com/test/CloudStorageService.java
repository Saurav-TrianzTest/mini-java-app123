package com.test;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Cloud-Native Storage Service
 * - Uses AWS S3 for file storage instead of local file system
 * - Supports cloud-native file operations
 * - Follows cloud storage best practices
 */
public class CloudStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(CloudStorageService.class);
    
    private final String bucketName;
    private final String region;
    private final AmazonS3 s3Client;
    private final boolean useS3;
    
    public CloudStorageService() {
        // Cloud-Native: Configuration from environment variables
        this.bucketName = System.getenv().getOrDefault("S3_BUCKET_NAME", "");
        this.region = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
        this.useS3 = "s3".equalsIgnoreCase(
            System.getenv().getOrDefault("APP_STORAGE_TYPE", "local")
        );
        
        if (useS3 && !bucketName.isEmpty()) {
            // Initialize S3 client with default credentials provider
            this.s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
            
            logger.info("Cloud storage initialized with S3 bucket: {}", bucketName);
        } else {
            this.s3Client = null;
            logger.info("Cloud storage not configured, using local fallback");
        }
    }
    
    /**
     * Upload file to cloud storage
     */
    public void uploadFile(String key, byte[] content, String contentType) {
        if (useS3 && s3Client != null) {
            uploadToS3(key, content, contentType);
        } else {
            logger.warn("S3 not configured, file upload skipped: {}", key);
        }
    }
    
    /**
     * Upload file to S3
     */
    private void uploadToS3(String key, byte[] content, String contentType) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(content.length);
            metadata.setContentType(contentType);
            
            InputStream inputStream = new ByteArrayInputStream(content);
            PutObjectRequest request = new PutObjectRequest(bucketName, key, inputStream, metadata);
            
            s3Client.putObject(request);
            logger.info("File uploaded to S3: s3://{}/{}", bucketName, key);
            
        } catch (Exception e) {
            logger.error("Failed to upload file to S3: {}", key, e);
            throw new RuntimeException("S3 upload failed", e);
        }
    }
    
    /**
     * Download file from cloud storage
     */
    public InputStream downloadFile(String key) {
        if (useS3 && s3Client != null) {
            return downloadFromS3(key);
        } else {
            logger.warn("S3 not configured, file download failed: {}", key);
            return null;
        }
    }
    
    /**
     * Download file from S3
     */
    private InputStream downloadFromS3(String key) {
        try {
            S3Object s3Object = s3Client.getObject(bucketName, key);
            logger.info("File downloaded from S3: s3://{}/{}", bucketName, key);
            return s3Object.getObjectContent();
            
        } catch (Exception e) {
            logger.error("Failed to download file from S3: {}", key, e);
            throw new RuntimeException("S3 download failed", e);
        }
    }
    
    /**
     * Delete file from cloud storage
     */
    public void deleteFile(String key) {
        if (useS3 && s3Client != null) {
            deleteFromS3(key);
        } else {
            logger.warn("S3 not configured, file deletion skipped: {}", key);
        }
    }
    
    /**
     * Delete file from S3
     */
    private void deleteFromS3(String key) {
        try {
            s3Client.deleteObject(bucketName, key);
            logger.info("File deleted from S3: s3://{}/{}", bucketName, key);
            
        } catch (Exception e) {
            logger.error("Failed to delete file from S3: {}", key, e);
            throw new RuntimeException("S3 deletion failed", e);
        }
    }
    
    /**
     * Check if file exists in cloud storage
     */
    public boolean fileExists(String key) {
        if (useS3 && s3Client != null) {
            return existsInS3(key);
        } else {
            logger.warn("S3 not configured, file existence check failed: {}", key);
            return false;
        }
    }
    
    /**
     * Check if file exists in S3
     */
    private boolean existsInS3(String key) {
        try {
            return s3Client.doesObjectExist(bucketName, key);
        } catch (Exception e) {
            logger.error("Failed to check file existence in S3: {}", key, e);
            return false;
        }
    }
    
    /**
     * Get file URL from cloud storage
     */
    public String getFileUrl(String key) {
        if (useS3 && s3Client != null) {
            return String.format("s3://%s/%s", bucketName, key);
        } else {
            return null;
        }
    }
}
