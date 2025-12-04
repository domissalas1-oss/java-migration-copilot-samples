package com.microsoft.migration.assets.worker.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobProperties;
import com.microsoft.migration.assets.worker.repository.ImageMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class S3FileProcessingServiceTest {

    @Mock
    private BlobServiceClient blobServiceClient;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private ImageMetadataRepository imageMetadataRepository;

    @InjectMocks
    private S3FileProcessingService s3FileProcessingService;

    private final String containerName = "test-container";
    private final String testKey = "test-image.jpg";
    private final String thumbnailKey = "test-image_thumbnail.jpg";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3FileProcessingService, "containerName", containerName);

        // Setup mock chain
        when(blobServiceClient.getBlobContainerClient(anyString())).thenReturn(blobContainerClient);
        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    }

    @Test
    void getStorageTypeReturnsAzureBlob() {
        // Act
        String result = s3FileProcessingService.getStorageType();

        // Assert
        assertEquals("azure-blob", result);
    }

    @Test
    void downloadOriginalDownloadsFileFromBlobStorage() throws Exception {
        // Arrange
        Path tempFile = Files.createTempFile("download-", ".tmp");

        doNothing().when(blobClient).downloadToFile(anyString(), anyBoolean());

        // Act
        s3FileProcessingService.downloadOriginal(testKey, tempFile);

        // Assert
        verify(blobServiceClient).getBlobContainerClient(containerName);
        verify(blobContainerClient).getBlobClient(testKey);
        verify(blobClient).downloadToFile(tempFile.toString(), true);

        // Clean up
        Files.deleteIfExists(tempFile);
    }

    @Test
    void uploadThumbnailUploadFileToBlobStorage() throws Exception {
        // Arrange
        Path tempFile = Files.createTempFile("thumbnail-", ".tmp");
        when(imageMetadataRepository.findAll()).thenReturn(Collections.emptyList());
        when(blobClient.uploadFromFileWithResponse(any(), any(), any())).thenReturn(null);

        // Act
        s3FileProcessingService.uploadThumbnail(tempFile, thumbnailKey, "image/jpeg");

        // Assert
        verify(blobServiceClient, atLeastOnce()).getBlobContainerClient(containerName);
        verify(blobContainerClient, atLeastOnce()).getBlobClient(thumbnailKey);
        verify(blobClient).uploadFromFileWithResponse(any(), any(), any());

        // Clean up
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testExtractOriginalKey() throws Exception {
        // Use reflection to test private method
        String result = (String) ReflectionTestUtils.invokeMethod(
                s3FileProcessingService,
                "extractOriginalKey",
                "image_thumbnail.jpg");

        // Assert
        assertEquals("image.jpg", result);
    }
}
