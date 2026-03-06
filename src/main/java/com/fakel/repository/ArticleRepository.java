package com.fakel.repository;

import com.fakel.model.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    // Поиск по дате
    Page<Article> findByPublicationDate(LocalDate date, Pageable pageable);

    Page<Article> findByPublicationDateBetween(LocalDate start, LocalDate end, Pageable pageable);

    // Поиск по теме (частичное совпадение)
    Page<Article> findByTopicContainingIgnoreCase(String topic, Pageable pageable);

    // Поиск по тегам
    @Query("SELECT DISTINCT a FROM Article a JOIN a.tags t WHERE t.code IN :tagCodes")
    Page<Article> findByTags(@Param("tagCodes") List<String> tagCodes, Pageable pageable);

    // Комбинированный поиск по дате и тегам
    @Query("SELECT DISTINCT a FROM Article a JOIN a.tags t " +
            "WHERE t.code IN :tagCodes AND a.publicationDate BETWEEN :start AND :end")
    Page<Article> findByTagsAndDateBetween(@Param("tagCodes") List<String> tagCodes,
                                           @Param("start") LocalDate start,
                                           @Param("end") LocalDate end,
                                           Pageable pageable);

    // Поиск по тексту
    Page<Article> findByTextContainingIgnoreCase(String text, Pageable pageable);

    boolean existsByTopic(String topic);
}