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

    // @Mock убран - теперь это реальный объект
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
        // Создаем реальные HttpHeaders
        yandexHeaders = new HttpHeaders();
        yandexHeaders.setContentType(MediaType.APPLICATION_JSON);
        yandexHeaders.set("Authorization", "OAuth test-token");

        // Настройка моков через Reflection для установки приватного поля
        setPrivateField(yandexDiskService, "yandexFolder", testFolder);
        setPrivateField(yandexDiskService, "yandexHeaders", yandexHeaders);

        // Настройка mock файла
        lenient().when(mockFile.getOriginalFilename()).thenReturn(testFileName);
        lenient().when(mockFile.getSize()).thenReturn(testFileSize);
        lenient().when(mockFile.getContentType()).thenReturn(testContentType);
        lenient().when(mockFile.isEmpty()).thenReturn(false);
        lenient().when(mockFile.getBytes()).thenReturn(testFileContent.getBytes());
        lenient().when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(testFileContent.getBytes()));
    }

    // Вспомогательный метод для установки приватных полей
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ============= ТЕСТЫ ДЛЯ uploadFile =============

    @Test
    void uploadFile_WithValidImage_ShouldReturnDirectLink() throws Exception {
        // Given
        String uploadUrl = "https://upload-url.com";
        String publicUrl = "https://yadi.sk/d/abc123";
        String directLink = "https://downloader.disk.yandex.ru/disk/abc123";

        // Мок для проверки папки (папка существует)
        ResponseEntity<String> folderCheckResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources?path=" + testFolder),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(folderCheckResponse);

        // Мок для получения URL загрузки
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

        // Мок для загрузки файла
        ResponseEntity<String> uploadFileResponse = new ResponseEntity<>("{}", HttpStatus.CREATED);
        when(restTemplate.exchange(
                eq(uploadUrl),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(uploadFileResponse);

        // Мок для публикации файла
        ResponseEntity<String> publishResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources/publish?path="),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(publishResponse);

        // Мок для получения информации о файле (публичная ссылка)
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

        // Мок для получения прямой ссылки
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

        // When
        String result = yandexDiskService.uploadFile(mockFile);

        // Then
        assertNotNull(result);
        assertEquals(directLink, result);
    }

    @Test
    void uploadFile_WithNullFile_ShouldThrowException() {
        // Given
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.uploadFile(null));
    }

    @Test
    void uploadFile_WithEmptyFile_ShouldThrowException() {
        // Given
        when(mockFile.isEmpty()).thenReturn(true);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WithFileTooLarge_ShouldThrowException() {
        // Given
        when(mockFile.getSize()).thenReturn(11 * 1024 * 1024L); // 11MB

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WithNonImageType_ShouldThrowException() {
        // Given
        when(mockFile.getContentType()).thenReturn("application/pdf");

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WithNullContentType_ShouldThrowException() {
        // Given
        when(mockFile.getContentType()).thenReturn(null);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WithNullFilename_ShouldThrowException() {
        // Given
        when(mockFile.getOriginalFilename()).thenReturn(null);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WithEmptyFilename_ShouldThrowException() {
        // Given
        when(mockFile.getOriginalFilename()).thenReturn("   ");

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WithFilenameWithoutExtension_ShouldThrowException() {
        // Given
        when(mockFile.getOriginalFilename()).thenReturn("testfile");

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WithInvalidExtension_ShouldThrowException() {
        // Given
        when(mockFile.getOriginalFilename()).thenReturn("testfile.txt");

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WithFilenameWithInvalidCharacters_ShouldThrowException() {
        // Given
        when(mockFile.getOriginalFilename()).thenReturn("test@#$.jpg");

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WhenGetUploadUrlFails_ShouldThrowIOException() throws Exception {
        // Given
        // Мок для проверки папки (папка существует)
        ResponseEntity<String> folderCheckResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources?path=" + testFolder),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(folderCheckResponse);

        // Мок для получения URL загрузки (ошибка)
        when(restTemplate.exchange(
                contains("/disk/resources/upload?path="),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RuntimeException("API Error"));

        // When & Then
        assertThrows(IOException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WhenUploadFileFails_ShouldThrowIOException() throws Exception {
        // Given
        String uploadUrl = "https://upload-url.com";

        // Мок для проверки папки (папка существует)
        ResponseEntity<String> folderCheckResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources?path=" + testFolder),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(folderCheckResponse);

        // Мок для получения URL загрузки
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

        // Мок для загрузки файла (ошибка)
        when(restTemplate.exchange(
                eq(uploadUrl),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RuntimeException("Upload failed"));

        // When & Then
        assertThrows(IOException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WhenPublishFails_ShouldThrowIOException() throws Exception {
        // Given
        String uploadUrl = "https://upload-url.com";

        // Мок для проверки папки (папка существует)
        ResponseEntity<String> folderCheckResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources?path=" + testFolder),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(folderCheckResponse);

        // Мок для получения URL загрузки
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

        // Мок для загрузки файла
        ResponseEntity<String> uploadFileResponse = new ResponseEntity<>("{}", HttpStatus.CREATED);
        when(restTemplate.exchange(
                eq(uploadUrl),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(uploadFileResponse);

        // Мок для публикации файла (ошибка)
        when(restTemplate.exchange(
                contains("/disk/resources/publish?path="),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RuntimeException("Publish failed"));

        // When & Then
        assertThrows(IOException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WhenGetInfoFails_ShouldThrowIOException() throws Exception {
        // Given
        String uploadUrl = "https://upload-url.com";

        // Мок для проверки папки (папка существует)
        ResponseEntity<String> folderCheckResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources?path=" + testFolder),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(folderCheckResponse);

        // Мок для получения URL загрузки
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

        // Мок для загрузки файла
        ResponseEntity<String> uploadFileResponse = new ResponseEntity<>("{}", HttpStatus.CREATED);
        when(restTemplate.exchange(
                eq(uploadUrl),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(uploadFileResponse);

        // Мок для публикации файла
        ResponseEntity<String> publishResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources/publish?path="),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(publishResponse);

        // Мок для получения информации о файле (ошибка)
        when(restTemplate.exchange(
                contains("/disk/resources?path="),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RuntimeException("Info failed"));

        // When & Then
        assertThrows(IOException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    @Test
    void uploadFile_WhenGetDirectLinkFails_ShouldThrowIOException() throws Exception {
        // Given
        String uploadUrl = "https://upload-url.com";
        String publicUrl = "https://yadi.sk/d/abc123";

        // Мок для проверки папки (папка существует)
        ResponseEntity<String> folderCheckResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources?path=" + testFolder),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(folderCheckResponse);

        // Мок для получения URL загрузки
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

        // Мок для загрузки файла
        ResponseEntity<String> uploadFileResponse = new ResponseEntity<>("{}", HttpStatus.CREATED);
        when(restTemplate.exchange(
                eq(uploadUrl),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(uploadFileResponse);

        // Мок для публикации файла
        ResponseEntity<String> publishResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/disk/resources/publish?path="),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(publishResponse);

        // Мок для получения информации о файле (публичная ссылка)
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

        // Мок для получения прямой ссылки (ошибка)
        when(restTemplate.exchange(
                contains("/disk/public/resources/download?public_key="),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RuntimeException("Direct link failed"));

        // When & Then
        assertThrows(IOException.class,
                () -> yandexDiskService.uploadFile(mockFile));
    }

    // ============= ТЕСТЫ ДЛЯ deleteFile =============

    @Test
    void deleteFile_WithValidPath_ShouldDelete() throws Exception {
        // Given
        String remotePath = testFolder + "/test-file.jpg";

        ResponseEntity<String> deleteResponse = new ResponseEntity<>("{}", HttpStatus.NO_CONTENT);
        when(restTemplate.exchange(
                contains("/disk/resources?path=" + remotePath),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(deleteResponse);

        // When
        yandexDiskService.deleteFile(remotePath);

        // Then
        verify(restTemplate, times(1)).exchange(
                contains("/disk/resources?path=" + remotePath),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void deleteFile_WithNullPath_ShouldThrowException() {
        // Given
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.deleteFile(null));
    }

    @Test
    void deleteFile_WithEmptyPath_ShouldThrowException() {
        // Given
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> yandexDiskService.deleteFile("   "));
    }

    @Test
    void deleteFile_WhenDeleteFails_ShouldThrowIOException() throws Exception {
        // Given
        String remotePath = testFolder + "/test-file.jpg";

        when(restTemplate.exchange(
                contains("/disk/resources?path=" + remotePath),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RuntimeException("Delete failed"));

        // When & Then
        assertThrows(IOException.class,
                () -> yandexDiskService.deleteFile(remotePath));
    }
}