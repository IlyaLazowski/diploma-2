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

        // Валидация диапазона дат
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        Page<Article> articles;

        // Приоритет: если есть теги
        if (tags != null && !tags.isEmpty()) {
            if (dateFrom != null && dateTo != null) {
                articles = articleRepository.findByTagsAndDateBetween(tags, dateFrom, dateTo, pageable);
            } else {
                articles = articleRepository.findByTags(tags, pageable);
            }
        }
        // Поиск по дате
        else if (date != null) {
            articles = articleRepository.findByPublicationDate(date, pageable);
        }
        else if (dateFrom != null && dateTo != null) {
            articles = articleRepository.findByPublicationDateBetween(dateFrom, dateTo, pageable);
        }
        // Поиск по теме
        else if (topic != null && !topic.isEmpty()) {
            articles = articleRepository.findByTopicContainingIgnoreCase(topic, pageable);
        }
        // Поиск по тексту
        else if (text != null && !text.isEmpty()) {
            articles = articleRepository.findByTextContainingIgnoreCase(text, pageable);
        }
        // Все статьи с пагинацией
        else {
            articles = articleRepository.findAll(pageable);
        }

        return articles.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public ArticleDto getArticleById(
            @NotNull(message = "ID статьи не может быть null")
            @Positive(message = "ID статьи должен быть положительным")
            Long id) {

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Статья не найдена"));

        return convertToDto(article);
    }

    @Transactional
    public ArticleDto createArticle(@Valid ArticleDto articleDto) {
        // Проверка на существующую тему
        if (articleRepository.existsByTopic(articleDto.getTopic())) {
            throw new RuntimeException("Статья с такой темой уже существует");
        }

        Article article = new Article();
        article.setTopic(articleDto.getTopic());
        article.setPublicationDate(articleDto.getPublicationDate() != null ?
                articleDto.getPublicationDate() : LocalDate.now());
        article.setText(articleDto.getText());

        // Добавление тегов
        if (articleDto.getTags() != null && !articleDto.getTags().isEmpty()) {
            List<Tag> tags = articleDto.getTags().stream()
                    .map(tagCode -> tagRepository.findByCode(tagCode)
                            .orElseGet(() -> {
                                Tag newTag = new Tag();
                                newTag.setCode(tagCode);
                                return tagRepository.save(newTag);
                            }))
                    .collect(Collectors.toList());
            article.setTags(tags);
        }

        Article saved = articleRepository.save(article);
        return convertToDto(saved);
    }

    @Transactional
    public ArticleDto updateArticle(@Valid ArticleDto articleDto) {
        if (articleDto.getId() == null) {
            throw new IllegalArgumentException("ID статьи не может быть null при обновлении");
        }

        Article article = articleRepository.findById(articleDto.getId())
                .orElseThrow(() -> new RuntimeException("Статья не найдена"));

        // Обновление полей
        if (articleDto.getTopic() != null && !articleDto.getTopic().equals(article.getTopic())) {
            // Проверка уникальности новой темы
            if (articleRepository.existsByTopic(articleDto.getTopic())) {
                throw new RuntimeException("Статья с такой темой уже существует");
            }
            article.setTopic(articleDto.getTopic());
        }

        if (articleDto.getText() != null) {
            article.setText(articleDto.getText());
        }

        if (articleDto.getPublicationDate() != null) {
            article.setPublicationDate(articleDto.getPublicationDate());
        }

        // Обновление тегов
        if (articleDto.getTags() != null) {
            List<Tag> tags = articleDto.getTags().stream()
                    .map(tagCode -> tagRepository.findByCode(tagCode)
                            .orElseGet(() -> {
                                Tag newTag = new Tag();
                                newTag.setCode(tagCode);
                                return tagRepository.save(newTag);
                            }))
                    .collect(Collectors.toList());
            article.setTags(tags);
        }

        Article updated = articleRepository.save(article);
        return convertToDto(updated);
    }

    @Transactional
    public void deleteArticle(
            @NotNull(message = "ID статьи не может быть null")
            @Positive(message = "ID статьи должен быть положительным")
            Long id) {

        if (!articleRepository.existsById(id)) {
            throw new RuntimeException("Статья не найдена");
        }

        articleRepository.deleteById(id);
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