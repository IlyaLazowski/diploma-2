package com.fakel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class YandexDiskServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private HttpHeaders yandexHeaders;

    @Mock
    private MultipartFile mockFile;

    @InjectMocks
    private YandexDiskService yandexDiskService;

    @Captor
    private ArgumentCaptor<String> urlCaptor;

    @Captor
    private ArgumentCaptor<HttpMethod> methodCaptor;

    @Captor
    private ArgumentCaptor<HttpEntity<?>> entityCaptor;

    @Captor
    private ArgumentCaptor<Class<String>> responseTypeCaptor;

    private final String testFolder = "/test-folder";
    private final String testFileName = "test-image.jpg";
    private final String testFileContent = "test image content";
    private final long testFileSize = 1024;
    private final String testContentType = "image/jpeg";

    @BeforeEach
    void setUp() throws Exception {

        yandexHeaders = new HttpHeaders();
        yandexHeaders.setContentType(MediaType.APPLICATION_JSON);
        yandexHeaders.set("Authorization", "OAuth test-token");

        setPrivateField(yandexDiskService, "yandexFolder", testFolder);
        setPrivateField(yandexDiskService, "yandexHeaders", yandexHeaders);


        lenient().when(mockFile.getOriginalFilename()).thenReturn(testFileName);
        lenient().when(mockFile.getSize()).thenReturn(testFileSize);
        lenient().when(mockFile.getContentType()).thenReturn(testContentType);
        lenient().when(mockFile.isEmpty()).thenReturn(false);
        lenient().when(mockFile.getBytes()).thenReturn(testFileContent.getBytes());
        lenient().when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(testFileContent.getBytes()));
    }


    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }



    @Test
    void uploadFile_WithValidImage_ShouldReturnDirectLink() throws Exception {
        // Given
        String uploadUrl = "https://upload-url.com";
        String publicUrl = "https://yadi.sk/d/abc123";
        String directLink = "https://downloader.disk.yandex.ru/disk/abc123";


        ResponseEntity<String> folderCheckResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources?path=" + testFolder),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(folderCheckResponse);


        ObjectNode uploadResponseNode = new ObjectMapper().createObjectNode();
        uploadResponseNode.put("href", uploadUrl);
        String uploadResponseBody = uploadResponseNode.toString();
        ResponseEntity<String> uploadUrlResponse = new ResponseEntity<>(uploadResponseBody, HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources/upload?path="),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(uploadUrlResponse);


        ResponseEntity<String> uploadFileResponse = new ResponseEntity<>("{}", HttpStatus.CREATED);
        when(restTemplate.exchange(
                eq(uploadUrl),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(uploadFileResponse);


        ResponseEntity<String> publishResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources/publish?path="),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(publishResponse);


        ObjectNode infoResponseNode = new ObjectMapper().createObjectNode();
        infoResponseNode.put("public_url", publicUrl);
        String infoResponseBody = infoResponseNode.toString();
        ResponseEntity<String> infoResponse = new ResponseEntity<>(infoResponseBody, HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources?path="),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(infoResponse);


        ObjectNode directLinkResponseNode = new ObjectMapper().createObjectNode();
        directLinkResponseNode.put("href", directLink);
        String directLinkResponseBody = directLinkResponseNode.toString();
        ResponseEntity<String> directLinkResponse = new ResponseEntity<>(directLinkResponseBody, HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/public/resources/download?public_key="),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(directLinkResponse);


        String result = yandexDiskService.uploadFile(mockFile);


        assertNotNull(result);
        assertEquals(directLink, result);
    }

    @Test
    void uploadFile_WithNullFile_ShouldThrowException() {

        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.uploadFile(null));
    }

    @Test
    void uploadFile_WithEmptyFile_ShouldThrowException() {

        when(mockFile.isEmpty()).thenReturn(true);


        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WithFileTooLarge_ShouldThrowException() {

        when(mockFile.getSize()).thenReturn(11 * 1024 * 1024L); // 11MB


        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WithNonImageType_ShouldThrowException() {

        when(mockFile.getContentType()).thenReturn("application/pdf");


        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WithNullContentType_ShouldThrowException() {

        when(mockFile.getContentType()).thenReturn(null);


        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WithNullFilename_ShouldThrowException() {

        when(mockFile.getOriginalFilename()).thenReturn(null);


        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WithEmptyFilename_ShouldThrowException() {

        when(mockFile.getOriginalFilename()).thenReturn("   ");


        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WithFilenameWithoutExtension_ShouldThrowException() {

        when(mockFile.getOriginalFilename()).thenReturn("testfile");


        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WithInvalidExtension_ShouldThrowException() {

        when(mockFile.getOriginalFilename()).thenReturn("testfile.txt");


        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WithFilenameWithInvalidCharacters_ShouldThrowException() {

        when(mockFile.getOriginalFilename()).thenReturn("test@#$.jpg");


        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WhenGetUploadUrlFails_ShouldThrowIOException() throws Exception {

        ResponseEntity<String> folderCheckResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources?path=" + testFolder),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(folderCheckResponse);


        when(restTemplate.exchange(
                contains("/disk/resources/upload?path="),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RuntimeException("API Error"));


        assertThrows(IOException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WhenUploadFileFails_ShouldThrowIOException() throws Exception {

        String uploadUrl = "https://upload-url.com";


        ResponseEntity<String> folderCheckResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources?path=" + testFolder),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(folderCheckResponse);


        ObjectNode uploadResponseNode = new ObjectMapper().createObjectNode();
        uploadResponseNode.put("href", uploadUrl);
        String uploadResponseBody = uploadResponseNode.toString();
        ResponseEntity<String> uploadUrlResponse = new ResponseEntity<>(uploadResponseBody, HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources/upload?path="),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(uploadUrlResponse);


        when(restTemplate.exchange(
                eq(uploadUrl),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RuntimeException("Upload failed"));


        assertThrows(IOException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WhenPublishFails_ShouldThrowIOException() throws Exception {

        String uploadUrl = "https://upload-url.com";


        ResponseEntity<String> folderCheckResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources?path=" + testFolder),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(folderCheckResponse);


        ObjectNode uploadResponseNode = new ObjectMapper().createObjectNode();
        uploadResponseNode.put("href", uploadUrl);
        String uploadResponseBody = uploadResponseNode.toString();
        ResponseEntity<String> uploadUrlResponse = new ResponseEntity<>(uploadResponseBody, HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources/upload?path="),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(uploadUrlResponse);


        ResponseEntity<String> uploadFileResponse = new ResponseEntity<>("{}", HttpStatus.CREATED);
        when(restTemplate.exchange(
                eq(uploadUrl),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(uploadFileResponse);


        when(restTemplate.exchange(
                contains("/disk/resources/publish?path="),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RuntimeException("Publish failed"));


        assertThrows(IOException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WhenGetInfoFails_ShouldThrowIOException() throws Exception {

        String uploadUrl = "https://upload-url.com";


        ResponseEntity<String> folderCheckResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources?path=" + testFolder),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(folderCheckResponse);


        ObjectNode uploadResponseNode = new ObjectMapper().createObjectNode();
        uploadResponseNode.put("href", uploadUrl);
        String uploadResponseBody = uploadResponseNode.toString();
        ResponseEntity<String> uploadUrlResponse = new ResponseEntity<>(uploadResponseBody, HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources/upload?path="),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(uploadUrlResponse);


        ResponseEntity<String> uploadFileResponse = new ResponseEntity<>("{}", HttpStatus.CREATED);
        when(restTemplate.exchange(
                eq(uploadUrl),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(uploadFileResponse);


        ResponseEntity<String> publishResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources/publish?path="),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(publishResponse);


        when(restTemplate.exchange(
                contains("/disk/resources?path="),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RuntimeException("Info failed"));


        assertThrows(IOException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WhenGetDirectLinkFails_ShouldThrowIOException() throws Exception {

        String uploadUrl = "https://upload-url.com";
        String publicUrl = "https://yadi.sk/d/abc123";


        ResponseEntity<String> folderCheckResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources?path=" + testFolder),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(folderCheckResponse);


        ObjectNode uploadResponseNode = new ObjectMapper().createObjectNode();
        uploadResponseNode.put("href", uploadUrl);
        String uploadResponseBody = uploadResponseNode.toString();
        ResponseEntity<String> uploadUrlResponse = new ResponseEntity<>(uploadResponseBody, HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources/upload?path="),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(uploadUrlResponse);


        ResponseEntity<String> uploadFileResponse = new ResponseEntity<>("{}", HttpStatus.CREATED);
        when(restTemplate.exchange(
                eq(uploadUrl),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(uploadFileResponse);

        ResponseEntity<String> publishResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources/publish?path="),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(publishResponse);


        ObjectNode infoResponseNode = new ObjectMapper().createObjectNode();
        infoResponseNode.put("public_url", publicUrl);
        String infoResponseBody = infoResponseNode.toString();
        ResponseEntity<String> infoResponse = new ResponseEntity<>(infoResponseBody, HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources?path="),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(infoResponse);


        when(restTemplate.exchange(
                contains("/disk/public/resources/download?public_key="),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RuntimeException("Direct link failed"));


        assertThrows(IOException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }



    @Test
    void deleteFile_WithValidPath_ShouldDelete() throws Exception {

        String remotePath = testFolder + "/test-file.jpg";

        ResponseEntity<String> deleteResponse = new ResponseEntity<>("{}", HttpStatus.NO_CONTENT);
        when(restTemplate.exchange(
                contains("/disk/resources?path=" + remotePath),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(deleteResponse);


        yandexDiskService.deleteFile(remotePath);


        verify(restTemplate, times(1)).exchange(
                contains("/disk/resources?path=" + remotePath),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void deleteFile_WithNullPath_ShouldThrowException() {

        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.deleteFile(null));
    }

    @Test
    void deleteFile_WithEmptyPath_ShouldThrowException() {

        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.deleteFile("   "));
    }

    @Test
    void deleteFile_WhenDeleteFails_ShouldThrowIOException() throws Exception {

        String remotePath = testFolder + "/test-file.jpg";

        when(restTemplate.exchange(
                contains("/disk/resources?path=" + remotePath),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RuntimeException("Delete failed"));


        assertThrows(IOException.class,
                () -> yandexDiskService.deleteFile(remotePath));
    }
}