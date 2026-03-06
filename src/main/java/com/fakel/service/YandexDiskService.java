package com.fakel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class YandexDiskService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private HttpHeaders yandexHeaders;

    @Value("${yandex.folder}")
    private String yandexFolder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String uploadFile(MultipartFile file) throws IOException {

        // Проверка входных данных
        if (file == null) {
            throw new IllegalArgumentException("Файл не может быть null");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }

        // Проверка размера файла (макс 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Размер файла не может превышать 10MB");
        }

        // Проверка типа файла
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Можно загружать только изображения");
        }

        // Проверка расширения файла
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя файла не может быть пустым");
        }

        String extension = "";
        if (originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        } else {
            throw new IllegalArgumentException("Файл должен иметь расширение");
        }

        // Проверка допустимых расширений
        if (!extension.matches("\\.(jpg|jpeg|png|gif|webp)$")) {
            throw new IllegalArgumentException("Допустимые форматы: JPG, JPEG, PNG, GIF, WEBP");
        }

        // Проверка имени файла на недопустимые символы
        if (!originalFilename.matches("^[a-zA-Zа-яА-Я0-9\\-_.() ]+\\.[a-zA-Z0-9]+$")) {
            throw new IllegalArgumentException("Имя файла содержит недопустимые символы");
        }

        // Проверка конфигурации
        if (yandexFolder == null || yandexFolder.trim().isEmpty()) {
            throw new IllegalStateException("Не указана папка для загрузки на Яндекс.Диске");
        }

        // Генерируем уникальное имя
        String fileName = UUID.randomUUID().toString() + extension;
        String remotePath = yandexFolder + "/" + fileName;

        // Создаем папку если нужно
        createFolderIfNotExists();

        // Получаем URL для загрузки
        String uploadUrl = getUploadUrl(remotePath);

        // Загружаем файл
        uploadFileToUrl(uploadUrl, file);

        // Делаем файл публичным и возвращаем прямую ссылку
        return publishAndGetDirectLink(remotePath);
    }

    private void createFolderIfNotExists() throws IOException {

        if (yandexFolder == null || yandexFolder.trim().isEmpty()) {
            throw new IllegalStateException("Не указана папка для загрузки на Яндекс.Диске");
        }

        String url = "https://cloud-api.yandex.net/v1/disk/resources?path=" + yandexFolder;

        try {
            HttpEntity<String> entity = new HttpEntity<>(yandexHeaders);
            restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        } catch (Exception e) {
            // Папки нет - создаем
            String createUrl = "https://cloud-api.yandex.net/v1/disk/resources?path=" + yandexFolder;
            HttpEntity<String> createEntity = new HttpEntity<>(yandexHeaders);

            try {
                restTemplate.exchange(createUrl, HttpMethod.PUT, createEntity, String.class);
            } catch (Exception ex) {
                throw new IOException("Не удалось создать папку на Яндекс.Диске", ex);
            }
        }
    }

    private String getUploadUrl(String remotePath) throws IOException {

        if (remotePath == null || remotePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Путь к файлу не может быть пустым");
        }

        String url = "https://cloud-api.yandex.net/v1/disk/resources/upload?path=" +
                remotePath + "&overwrite=true";

        HttpEntity<String> entity = new HttpEntity<>(yandexHeaders);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        } catch (Exception e) {
            throw new IOException("Не удалось получить URL для загрузки: " + e.getMessage());
        }

        if (response.getBody() == null) {
            throw new IOException("Пустой ответ от Яндекс.Диска при получении URL для загрузки");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new IOException("Ошибка парсинга ответа от Яндекс.Диска", e);
        }

        JsonNode hrefNode = root.get("href");
        if (hrefNode == null) {
            throw new IOException("В ответе отсутствует поле href");
        }

        return hrefNode.asText();
    }

    private void uploadFileToUrl(String uploadUrl, MultipartFile file) throws IOException {

        if (uploadUrl == null || uploadUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("URL для загрузки не может быть пустым");
        }

        if (file == null) {
            throw new IllegalArgumentException("Файл не может быть null");
        }

        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new IOException("Не удалось прочитать файл", e);
        }

        HttpEntity<byte[]> entity = new HttpEntity<>(fileBytes, uploadHeaders);

        try {
            restTemplate.exchange(uploadUrl, HttpMethod.PUT, entity, String.class);
        } catch (Exception e) {
            throw new IOException("Не удалось загрузить файл на Яндекс.Диск", e);
        }
    }

    private String publishAndGetDirectLink(String remotePath) throws IOException {

        if (remotePath == null || remotePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Путь к файлу не может быть пустым");
        }

        HttpEntity<String> entity = new HttpEntity<>(yandexHeaders);

        // Делаем файл публичным
        String publishUrl = "https://cloud-api.yandex.net/v1/disk/resources/publish?path=" + remotePath;

        try {
            restTemplate.exchange(publishUrl, HttpMethod.PUT, entity, String.class);
        } catch (Exception e) {
            throw new IOException("Не удалось опубликовать файл на Яндекс.Диске", e);
        }

        // Получаем публичную ссылку на файл
        String infoUrl = "https://cloud-api.yandex.net/v1/disk/resources?path=" + remotePath;

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(infoUrl, HttpMethod.GET, entity, String.class);
        } catch (Exception e) {
            throw new IOException("Не удалось получить информацию о файле", e);
        }

        if (response.getBody() == null) {
            throw new IOException("Пустой ответ от Яндекс.Диска при получении информации о файле");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new IOException("Ошибка парсинга ответа от Яндекс.Диска", e);
        }

        JsonNode publicUrlNode = root.get("public_url");
        if (publicUrlNode == null) {
            throw new IOException("В ответе отсутствует поле public_url");
        }

        String publicUrl = publicUrlNode.asText();

        // Получаем прямую ссылку на скачивание через API
        return getDirectDownloadLink(publicUrl);
    }

    private String getDirectDownloadLink(String publicUrl) throws IOException {

        if (publicUrl == null || publicUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Публичная ссылка не может быть пустой");
        }

        String apiUrl = "https://cloud-api.yandex.net/v1/disk/public/resources/download?public_key=" + publicUrl;

        HttpEntity<String> entity = new HttpEntity<>(yandexHeaders);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);
        } catch (Exception e) {
            throw new IOException("Не удалось получить прямую ссылку на файл", e);
        }

        if (response.getBody() == null) {
            throw new IOException("Пустой ответ от Яндекс.Диска при получении прямой ссылки");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new IOException("Ошибка парсинга ответа от Яндекс.Диска", e);
        }

        JsonNode hrefNode = root.get("href");
        if (hrefNode == null) {
            throw new IOException("В ответе отсутствует поле href");
        }

        return hrefNode.asText();
    }

    public void deleteFile(String remotePath) throws IOException {

        if (remotePath == null || remotePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Путь к файлу не может быть пустым");
        }

        String url = "https://cloud-api.yandex.net/v1/disk/resources?path=" + remotePath;

        HttpEntity<String> entity = new HttpEntity<>(yandexHeaders);

        try {
            restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
        } catch (Exception e) {
            throw new IOException("Не удалось удалить файл с Яндекс.Диска", e);
        }
    }
}