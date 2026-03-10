package com.fakel.exception;

import com.fakel.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ============= 400 - BAD REQUEST =============

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Неверный аргумент: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Неверные данные запроса", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.error("Ошибка валидации: {}", errors);
        return buildResponse(HttpStatus.BAD_REQUEST, "Ошибка валидации", errors.toString());
    }

    // ============= 401 - UNAUTHORIZED =============

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        log.error("Ошибка аутентификации: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "Необходима авторизация", "Неверный логин или пароль");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        log.error("Неверные учетные данные: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "Ошибка входа", "Неверный логин или пароль");
    }

    // ============= 403 - FORBIDDEN =============

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.error("Доступ запрещен: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "Доступ запрещен", "У вас нет прав для выполнения этой операции");
    }

    // ============= 404 - NOT FOUND / 400 - BAD REQUEST / 500 - SERVER ERROR =============
    // БЫСТРЫЙ ФИКС: универсальный обработчик для всех RuntimeException

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

        // Проверка на 404 Not Found
        if (message.contains("не найден") ||
                message.contains("не найдена") ||
                message.contains("не найдено") ||
                message.contains("not found") ||
                message.contains("не существует") ||
                message.contains("отсутствует") ||
                message.contains("не указан") ||
                message.contains("не найдены")) {

            log.info("404 Not Found: {}", ex.getMessage());
            return buildResponse(HttpStatus.NOT_FOUND, "Объект не найден", ex.getMessage());
        }

        // Проверка на 400 Bad Request
        if (message.contains("должен быть") ||
                message.contains("не может быть") ||
                message.contains("некорректный") ||
                message.contains("обязателен") ||
                message.contains("положительным") ||
                message.contains("не может быть пустым") ||
                message.contains("не может быть null") ||
                message.contains("допустимые") ||
                message.contains("формате")) {

            log.warn("400 Bad Request: {}", ex.getMessage());
            return buildResponse(HttpStatus.BAD_REQUEST, "Неверные данные запроса", ex.getMessage());
        }

        // Все остальное - 500 Internal Server Error
        log.error("500 Internal Server Error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера",
                "Произошла непредвиденная ошибка. Попробуйте позже.");
    }

    // ============= 413 - PAYLOAD TOO LARGE =============

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        log.error("Файл слишком большой: {}", ex.getMessage());
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "Файл слишком большой", "Максимальный размер файла 10MB");
    }

    // ============= 500 - INTERNAL SERVER ERROR (для всех остальных исключений) =============

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Необработанная ошибка: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера",
                "Произошла непредвиденная ошибка. Попробуйте позже.");
    }

    // ============= ОШИБКИ СЕТИ И ВНЕШНИХ API =============

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpClientError(HttpClientErrorException ex) {
        log.error("Ошибка HTTP клиента: {} - {}", ex.getStatusCode(), ex.getMessage());

        if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
            return buildResponse(HttpStatus.BAD_GATEWAY, "Ошибка внешнего сервиса",
                    "Внешний сервис вернул ошибку доступа. Проверьте токены.");
        }
        if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return buildResponse(HttpStatus.BAD_GATEWAY, "Ошибка внешнего сервиса",
                    "Истек срок действия токена внешнего сервиса.");
        }
        return buildResponse(HttpStatus.BAD_GATEWAY, "Ошибка внешнего сервиса", ex.getMessage());
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleResourceAccess(ResourceAccessException ex) {
        log.error("Ошибка доступа к ресурсу: {}", ex.getMessage());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Сервис недоступен",
                "Не удалось подключиться к внешнему сервису. Проверьте сетевое соединение.");
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIO(IOException ex) {
        log.error("Ошибка ввода-вывода: {}", ex.getMessage());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка файловой системы",
                "Не удалось обработать файл. Попробуйте еще раз.");
    }

    // ============= ВСПОМОГАТЕЛЬНЫЙ МЕТОД =============

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error, String message) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                message
        );
        return new ResponseEntity<>(response, status);
    }
}