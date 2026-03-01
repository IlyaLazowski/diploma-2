package com.fakel.service;

import com.fakel.dto.ArticleDto;
import com.fakel.model.Article;
import com.fakel.model.Tag;
import com.fakel.repository.ArticleRepository;
import com.fakel.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private TagRepository tagRepository;

    @Transactional(readOnly = true)
    public Page<ArticleDto> searchArticles(
            String topic,
            String text,
            LocalDate date,
            LocalDate dateFrom,
            LocalDate dateTo,
            List<String> tags,
            Pageable pageable) {

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
    public ArticleDto getArticleById(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Статья не найдена"));
        return convertToDto(article);
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