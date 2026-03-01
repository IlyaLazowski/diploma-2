package com.fakel.controller;

import com.fakel.dto.ArticleDto;
import com.fakel.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    /**
     * GET /api/articles?page=0&size=10
     * GET /api/articles?topic=биатлон
     * GET /api/articles?text=сердце
     * GET /api/articles?date=2026-02-10
     * GET /api/articles?dateFrom=2026-01-01&dateTo=2026-02-01
     * GET /api/articles?tags=медицина,физиология
     */
    @GetMapping
    public Page<ArticleDto> getArticles(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Sort sort = Sort.by("publicationDate").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return articleService.searchArticles(topic, text, date, dateFrom, dateTo, tags, pageable);
    }

    /**
     * GET /api/articles/{id}
     */
    @GetMapping("/{id}")
    public ArticleDto getArticleById(@PathVariable Long id) {
        return articleService.getArticleById(id);
    }
}