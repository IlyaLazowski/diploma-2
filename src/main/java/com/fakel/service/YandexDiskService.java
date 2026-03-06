package com.fakel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(YandexDiskService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private HttpHeaders yandexHeaders;

    @Value("${yandex.folder}")
    private String yandexFolder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String uploadFile(MultipartFile file) throws IOException {

        log.info("Начало загрузки файла на Яндекс.Диск");
        log.debug("Исходное имя файла: {}", file != null ? file.getOriginalFilename() : null);
        log.debug("Размер файла: {} байт", file != null ? file.getSize() : 0);

        // Проверка входных данных
        if (file == null) {
            log.warn("Попытка загрузки null файла");
            throw new IllegalArgumentException("Файл не может быть null");
        }

        if (file.isEmpty()) {
            log.warn("Попытка загрузки пустого файла");
            throw new IllegalArgumentException("Файл не может быть пустым");
        }

        // Проверка размера файла (макс 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            log.warn("Размер файла превышает 10MB: {} байт", file.getSize());
            throw new IllegalArgumentException("Размер файла не может превышать 10MB");
        }

        // Проверка типа файла
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            log.warn("Некорректный тип файла: {}", contentType);
            throw new IllegalArgumentException("Можно загружать только изображения");
        }
        log.debug("Тип файла: {}", contentType);

        // Проверка расширения файла
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            log.warn("Имя файла пустое");
            throw new IllegalArgumentException("Имя файла не может быть пустым");
        }

        String trimmedFilename = originalFilename.trim();
        log.debug("Имя файла после trim: '{}'", trimmedFilename);

        String extension = "";
        if (trimmedFilename.contains(".")) {
            extension = trimmedFilename.substring(trimmedFilename.lastIndexOf(".")).toLowerCase();
            log.debug("Расширение файла: {}", extension);
        } else {
            log.warn("Файл не имеет расширения");
            throw new IllegalArgumentException("Файл должен иметь расширение");
        }

        // Проверка допустимых расширений
        if (!extension.matches("\\.(jpg|jpeg|png|gif|webp)$")) {
            log.warn("Недопустимое расширение файла: {}", extension);
            throw new IllegalArgumentException("Допустимые форматы: JPG, JPEG, PNG, GIF, WEBP");
        }

        // Проверка имени файла на недопустимые символы
        if (!trimmedFilename.matches("^[a-zA-Zа-яА-Я0-9\\-_.() ]+\\.[a-zA-Z0-9]+$")) {
            log.warn("Имя файла содержит недопустимые символы: {}", trimmedFilename);
            throw new IllegalArgumentException("Имя файла содержит недопустимые символы");
        }

        // Проверка конфигурации
        if (yandexFolder == null || yandexFolder.trim().isEmpty()) {
            log.error("Не указана папка для загрузки на Яндекс.Диске (yandex.folder)");
            throw new IllegalStateException("Не указана папка для загрузки на Яндекс.Диске");
        }
        log.debug("Папка на Яндекс.Диске: {}", yandexFolder);

        // Генерируем уникальное имя
        String fileName = UUID.randomUUID().toString() + extension;
        String remotePath = yandexFolder + "/" + fileName;
        log.debug("Уникальное имя файла: {}", fileName);
        log.debug("Полный путь на Яндекс.Диске: {}", remotePath);

        // Создаем папку если нужно
        log.debug("Проверка существования папки на Яндекс.Диске");
        createFolderIfNotExists();

        // Получаем URL для загрузки
        log.debug("Получение URL для загрузки");
        String uploadUrl = getUploadUrl(remotePath);
        log.debug("URL для загрузки получен: {}", uploadUrl);

        // Загружаем файл
        log.debug("Загрузка файла на Яндекс.Диск");
        uploadFileToUrl(uploadUrl, file);
        log.info("Файл успешно загружен на Яндекс.Диск");

        // Делаем файл публичным и возвращаем прямую ссылку
        log.debug("Публикация файла и получение прямой ссылки");
        String directLink = publishAndGetDirectLink(remotePath);
        log.info("Прямая ссылка на файл: {}", directLink);

        return directLink;
    }

    private void createFolderIfNotExists() throws IOException {

        log.trace("Проверка существования папки: {}", yandexFolder);

        if (yandexFolder == null || yandexFolder.trim().isEmpty()) {
            log.error("Не указана папка для загрузки на Яндекс.Диске");
            throw new IllegalStateException("Не указана папка для загрузки на Яндекс.Диске");
        }

        String url = "https://cloud-api.yandex.net/v1/disk/resources?path=" + yandexFolder;

        try {
            HttpEntity<String> entity = new HttpEntity<>(yandexHeaders);
            restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            log.trace("Папка уже существует: {}", yandexFolder);
        } catch (Exception e) {
            // Папки нет - создаем
            log.debug("Папка не найдена, создаем: {}", yandexFolder);
            String createUrl = "https://cloud-api.yandex.net/v1/disk/resources?path=" + yandexFolder;
            HttpEntity<String> createEntity = new HttpEntity<>(yandexHeaders);

            try {
                restTemplate.exchange(createUrl, HttpMethod.PUT, createEntity, String.class);
                log.info("Папка успешно создана: {}", yandexFolder);
            } catch (Exception ex) {
                log.error("Ошибка создания папки {}: {}", yandexFolder, ex.getMessage(), ex);
                throw new IOException("Не удалось создать папку на Яндекс.Диске", ex);
            }
        }
    }

    private String getUploadUrl(String remotePath) throws IOException {

        log.trace("Получение URL для загрузки файла по пути: {}", remotePath);

        if (remotePath == null || remotePath.trim().isEmpty()) {
            log.warn("Путь к файлу пустой");
            throw new IllegalArgumentException("Путь к файлу не может быть пустым");
        }

        String url = "https://cloud-api.yandex.net/v1/disk/resources/upload?path=" +
                remotePath + "&overwrite=true";
        log.trace("Запрос к API: {}", url);

        HttpEntity<String> entity = new HttpEntity<>(yandexHeaders);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            log.trace("Ответ получен, статус: {}", response.getStatusCode());
        } catch (Exception e) {
            log.error("Ошибка при получении URL для загрузки: {}", e.getMessage(), e);
            throw new IOException("Не удалось получить URL для загрузки: " + e.getMessage());
        }

        if (response.getBody() == null) {
            log.error("Пустой ответ от Яндекс.Диска при получении URL для загрузки");
            throw new IOException("Пустой ответ от Яндекс.Диска при получении URL для загрузки");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(response.getBody());
            log.trace("JSON ответа распарсен");
        } catch (Exception e) {
            log.error("Ошибка парсинга ответа от Яндекс.Диска: {}", e.getMessage(), e);
            throw new IOException("Ошибка парсинга ответа от Яндекс.Диска", e);
        }

        JsonNode hrefNode = root.get("href");
        if (hrefNode == null) {
            log.error("В ответе отсутствует поле href: {}", response.getBody());
            throw new IOException("В ответе отсутствует поле href");
        }

        String href = hrefNode.asText();
        log.trace("Получен URL для загрузки: {}", href);
        return href;
    }

    private void uploadFileToUrl(String uploadUrl, MultipartFile file) throws IOException {

        log.trace("Загрузка файла по URL: {}", uploadUrl);

        if (uploadUrl == null || uploadUrl.trim().isEmpty()) {
            log.warn("URL для загрузки пустой");
            throw new IllegalArgumentException("URL для загрузки не может быть пустым");
        }

        if (file == null) {
            log.warn("Файл null");
            throw new IllegalArgumentException("Файл не может быть null");
        }

        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
            log.trace("Файл прочитан, размер: {} байт", fileBytes.length);
        } catch (IOException e) {
            log.error("Ошибка при чтении файла: {}", e.getMessage(), e);
            throw new IOException("Не удалось прочитать файл", e);
        }

        HttpEntity<byte[]> entity = new HttpEntity<>(fileBytes, uploadHeaders);

        try {
            restTemplate.exchange(uploadUrl, HttpMethod.PUT, entity, String.class);
            log.trace("Файл успешно загружен");
        } catch (Exception e) {
            log.error("Ошибка при загрузке файла на Яндекс.Диск: {}", e.getMessage(), e);
            throw new IOException("Не удалось загрузить файл на Яндекс.Диск", e);
        }
    }

    private String publishAndGetDirectLink(String remotePath) throws IOException {

        log.trace("Публикация файла по пути: {}", remotePath);

        if (remotePath == null || remotePath.trim().isEmpty()) {
            log.warn("Путь к файлу пустой");
            throw new IllegalArgumentException("Путь к файлу не может быть пустым");
        }

        HttpEntity<String> entity = new HttpEntity<>(yandexHeaders);

        // Делаем файл публичным
        String publishUrl = "https://cloud-api.yandex.net/v1/disk/resources/publish?path=" + remotePath;
        log.trace("Запрос на публикацию: {}", publishUrl);

        try {
            restTemplate.exchange(publishUrl, HttpMethod.PUT, entity, String.class);
            log.trace("Файл успешно опубликован");
        } catch (Exception e) {
            log.error("Ошибка при публикации файла: {}", e.getMessage(), e);
            throw new IOException("Не удалось опубликовать файл на Яндекс.Диске", e);
        }

        // Получаем публичную ссылку на файл
        String infoUrl = "https://cloud-api.yandex.net/v1/disk/resources?path=" + remotePath;
        log.trace("Запрос информации о файле: {}", infoUrl);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(infoUrl, HttpMethod.GET, entity, String.class);
            log.trace("Информация о файле получена, статус: {}", response.getStatusCode());
        } catch (Exception e) {
            log.error("Ошибка при получении информации о файле: {}", e.getMessage(), e);
            throw new IOException("Не удалось получить информацию о файле", e);
        }

        if (response.getBody() == null) {
            log.error("Пустой ответ от Яндекс.Диска при получении информации о файле");
            throw new IOException("Пустой ответ от Яндекс.Диска при получении информации о файле");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(response.getBody());
            log.trace("JSON информации о файле распарсен");
        } catch (Exception e) {
            log.error("Ошибка парсинга ответа от Яндекс.Диска: {}", e.getMessage(), e);
            throw new IOException("Ошибка парсинга ответа от Яндекс.Диска", e);
        }

        JsonNode publicUrlNode = root.get("public_url");
        if (publicUrlNode == null) {
            log.error("В ответе отсутствует поле public_url: {}", response.getBody());
            throw new IOException("В ответе отсутствует поле public_url");
        }

        String publicUrl = publicUrlNode.asText();
        log.trace("Получена публичная ссылка: {}", publicUrl);

        // Получаем прямую ссылку на скачивание через API
        return getDirectDownloadLink(publicUrl);
    }

    private String getDirectDownloadLink(String publicUrl) throws IOException {

        log.trace("Получение прямой ссылки из публичной: {}", publicUrl);

        if (publicUrl == null || publicUrl.trim().isEmpty()) {
            log.warn("Публичная ссылка пустая");
            throw new IllegalArgumentException("Публичная ссылка не может быть пустой");
        }

        String apiUrl = "https://cloud-api.yandex.net/v1/disk/public/resources/download?public_key=" + publicUrl;
        log.trace("Запрос к API для прямой ссылки: {}", apiUrl);

        HttpEntity<String> entity = new HttpEntity<>(yandexHeaders);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);
            log.trace("Ответ получен, статус: {}", response.getStatusCode());
        } catch (Exception e) {
            log.error("Ошибка при получении прямой ссылки: {}", e.getMessage(), e);
            throw new IOException("Не удалось получить прямую ссылку на файл", e);
        }

        if (response.getBody() == null) {
            log.error("Пустой ответ от Яндекс.Диска при получении прямой ссылки");
            throw new IOException("Пустой ответ от Яндекс.Диска при получении прямой ссылки");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(response.getBody());
            log.trace("JSON ответа распарсен");
        } catch (Exception e) {
            log.error("Ошибка парсинга ответа от Яндекс.Диска: {}", e.getMessage(), e);
            throw new IOException("Ошибка парсинга ответа от Яндекс.Диска", e);
        }

        JsonNode hrefNode = root.get("href");
        if (hrefNode == null) {
            log.error("В ответе отсутствует поле href: {}", response.getBody());
            throw new IOException("В ответе отсутствует поле href");
        }

        String directLink = hrefNode.asText();
        log.trace("Получена прямая ссылка: {}", directLink);
        return directLink;
    }

    public void deleteFile(String remotePath) throws IOException {

        log.info("Удаление файла с Яндекс.Диска: {}", remotePath);

        if (remotePath == null || remotePath.trim().isEmpty()) {
            log.warn("Путь к файлу пустой");
            throw new IllegalArgumentException("Путь к файлу не может быть пустым");
        }

        String url = "https://cloud-api.yandex.net/v1/disk/resources?path=" + remotePath;
        log.debug("Запрос на удаление: {}", url);

        HttpEntity<String> entity = new HttpEntity<>(yandexHeaders);

        try {
            restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
            log.info("Файл успешно удален: {}", remotePath);
        } catch (Exception e) {
            log.error("Ошибка при удалении файла {}: {}", remotePath, e.getMessage(), e);
            throw new IOException("Не удалось удалить файл с Яндекс.Диска", e);
        }
    }
}