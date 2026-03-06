package com.fakel.service;

import com.fakel.dto.ArticleDto;
import com.fakel.model.Article;
import com.fakel.model.Tag;
import com.fakel.repository.ArticleRepository;
import com.fakel.repository.TagRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Validated
public class ArticleService {

    private static final Logger log = LoggerFactory.getLogger(ArticleService.class);

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private TagRepository tagRepository;

    @Transactional(readOnly = true)
    public Page<ArticleDto> searchArticles(
            @Size(max = 256, message = "Тема поиска не длиннее 256 символов")
            String topic,

            @Size(max = 1000, message = "Текст поиска не длиннее 1000 символов")
            String text,

            LocalDate date,

            LocalDate dateFrom,

            LocalDate dateTo,

            @Size(max = 10, message = "Не более 10 тегов для поиска")
            List<@Size(min = 2, max = 64, message = "Тег должен быть от 2 до 64 символов") String> tags,

            Pageable pageable) {

        log.info("Поиск статей с параметрами: topic={}, text={}, date={}, dateFrom={}, dateTo={}, tags={}, page={}, size={}",
                topic, text, date, dateFrom, dateTo, tags, pageable.getPageNumber(), pageable.getPageSize());

        // Валидация диапазона дат
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            log.warn("Некорректный диапазон дат: dateFrom={} > dateTo={}", dateFrom, dateTo);
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        Page<Article> articles;

        // Приоритет: если есть теги
        if (tags != null && !tags.isEmpty()) {
            log.debug("Поиск по тегам: {}", tags);
            if (dateFrom != null && dateTo != null) {
                log.debug("С фильтром по датам: {} - {}", dateFrom, dateTo);
                articles = articleRepository.findByTagsAndDateBetween(tags, dateFrom, dateTo, pageable);
            } else {
                articles = articleRepository.findByTags(tags, pageable);
            }
        }
        // Поиск по дате
        else if (date != null) {
            log.debug("Поиск по дате: {}", date);
            articles = articleRepository.findByPublicationDate(date, pageable);
        }
        else if (dateFrom != null && dateTo != null) {
            log.debug("Поиск по диапазону дат: {} - {}", dateFrom, dateTo);
            articles = articleRepository.findByPublicationDateBetween(dateFrom, dateTo, pageable);
        }
        // Поиск по теме
        else if (topic != null && !topic.isEmpty()) {
            log.debug("Поиск по теме: {}", topic);
            articles = articleRepository.findByTopicContainingIgnoreCase(topic, pageable);
        }
        // Поиск по тексту
        else if (text != null && !text.isEmpty()) {
            log.debug("Поиск по тексту: {}", text);
            articles = articleRepository.findByTextContainingIgnoreCase(text, pageable);
        }
        // Все статьи с пагинацией
        else {
            log.debug("Получение всех статей с пагинацией");
            articles = articleRepository.findAll(pageable);
        }

        log.info("Найдено {} статей", articles.getTotalElements());
        log.debug("Всего страниц: {}", articles.getTotalPages());

        return articles.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public ArticleDto getArticleById(
            @NotNull(message = "ID статьи не может быть null")
            @Positive(message = "ID статьи должен быть положительным")
            Long id) {

        log.info("Получение статьи по ID: {}", id);

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Статья с ID {} не найдена", id);
                    return new RuntimeException("Статья не найдена");
                });

        log.debug("Статья найдена: тема='{}', дата={}", article.getTopic(), article.getPublicationDate());

        return convertToDto(article);
    }

    @Transactional
    public ArticleDto createArticle(@Valid ArticleDto articleDto) {
        log.info("Создание новой статьи: тема='{}'", articleDto.getTopic());
        log.debug("DTO статьи: {}", articleDto);

        // Проверка на существующую тему
        if (articleRepository.existsByTopic(articleDto.getTopic())) {
            log.warn("Статья с темой '{}' уже существует", articleDto.getTopic());
            throw new RuntimeException("Статья с такой темой уже существует");
        }

        Article article = new Article();
        article.setTopic(articleDto.getTopic());
        article.setPublicationDate(articleDto.getPublicationDate() != null ?
                articleDto.getPublicationDate() : LocalDate.now());
        article.setText(articleDto.getText());

        // Добавление тегов
        if (articleDto.getTags() != null && !articleDto.getTags().isEmpty()) {
            log.debug("Добавление тегов: {}", articleDto.getTags());
            List<Tag> tags = articleDto.getTags().stream()
                    .map(tagCode -> {
                        log.trace("Обработка тега: {}", tagCode);
                        return tagRepository.findByCode(tagCode)
                                .orElseGet(() -> {
                                    log.debug("Создание нового тега: {}", tagCode);
                                    Tag newTag = new Tag();
                                    newTag.setCode(tagCode);
                                    return tagRepository.save(newTag);
                                });
                    })
                    .collect(Collectors.toList());
            article.setTags(tags);
        }

        Article saved = articleRepository.save(article);
        log.info("Статья успешно создана с ID: {}", saved.getId());
        log.debug("Созданная статья: тема='{}', дата={}, теги={}",
                saved.getTopic(), saved.getPublicationDate(),
                saved.getTags().stream().map(Tag::getCode).collect(Collectors.toList()));

        return convertToDto(saved);
    }

    @Transactional
    public ArticleDto updateArticle(@Valid ArticleDto articleDto) {
        log.info("Обновление статьи с ID: {}", articleDto.getId());

        if (articleDto.getId() == null) {
            log.warn("Попытка обновления статьи без ID");
            throw new IllegalArgumentException("ID статьи не может быть null при обновлении");
        }

        Article article = articleRepository.findById(articleDto.getId())
                .orElseThrow(() -> {
                    log.warn("Статья с ID {} не найдена для обновления", articleDto.getId());
                    return new RuntimeException("Статья не найдена");
                });

        log.debug("Текущие данные статьи: тема='{}', дата={}, теги={}",
                article.getTopic(), article.getPublicationDate(),
                article.getTags().stream().map(Tag::getCode).collect(Collectors.toList()));

        // Обновление полей
        if (articleDto.getTopic() != null && !articleDto.getTopic().equals(article.getTopic())) {
            log.debug("Обновление темы с '{}' на '{}'", article.getTopic(), articleDto.getTopic());
            // Проверка уникальности новой темы
            if (articleRepository.existsByTopic(articleDto.getTopic())) {
                log.warn("Статья с темой '{}' уже существует", articleDto.getTopic());
                throw new RuntimeException("Статья с такой темой уже существует");
            }
            article.setTopic(articleDto.getTopic());
        }

        if (articleDto.getText() != null) {
            log.debug("Обновление текста статьи");
            article.setText(articleDto.getText());
        }

        if (articleDto.getPublicationDate() != null) {
            log.debug("Обновление даты публикации с {} на {}", article.getPublicationDate(), articleDto.getPublicationDate());
            article.setPublicationDate(articleDto.getPublicationDate());
        }

        // Обновление тегов
        if (articleDto.getTags() != null) {
            log.debug("Обновление тегов: {}", articleDto.getTags());
            List<Tag> tags = articleDto.getTags().stream()
                    .map(tagCode -> {
                        log.trace("Обработка тега: {}", tagCode);
                        return tagRepository.findByCode(tagCode)
                                .orElseGet(() -> {
                                    log.debug("Создание нового тега: {}", tagCode);
                                    Tag newTag = new Tag();
                                    newTag.setCode(tagCode);
                                    return tagRepository.save(newTag);
                                });
                    })
                    .collect(Collectors.toList());
            article.setTags(tags);
        }

        Article updated = articleRepository.save(article);
        log.info("Статья с ID {} успешно обновлена", updated.getId());
        log.debug("Обновленные данные: тема='{}', дата={}, теги={}",
                updated.getTopic(), updated.getPublicationDate(),
                updated.getTags().stream().map(Tag::getCode).collect(Collectors.toList()));

        return convertToDto(updated);
    }

    @Transactional
    public void deleteArticle(
            @NotNull(message = "ID статьи не может быть null")
            @Positive(message = "ID статьи должен быть положительным")
            Long id) {

        log.info("Удаление статьи с ID: {}", id);

        if (!articleRepository.existsById(id)) {
            log.warn("Статья с ID {} не найдена для удаления", id);
            throw new RuntimeException("Статья не найдена");
        }

        articleRepository.deleteById(id);
        log.info("Статья с ID {} успешно удалена", id);
    }

    private ArticleDto convertToDto(Article article) {
        List<String> tagCodes = article.getTags().stream()
                .map(Tag::getCode)
                .collect(Collectors.toList());

        return new ArticleDto(
                article.getId(),
                article.getTopic(),
                article.getPublicationDate(),
                article.getText(),
                tagCodes
        );
    }
}