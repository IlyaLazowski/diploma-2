package com.fakel.service;

import com.fakel.dto.ArticleDto;
import com.fakel.model.Article;
import com.fakel.model.Tag;
import com.fakel.repository.ArticleRepository;
import com.fakel.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private ArticleService articleService;

    @Captor
    private ArgumentCaptor<Article> articleCaptor;

    private Article testArticle;
    private ArticleDto testArticleDto;
    private LocalDate now;

    @BeforeEach
    void setUp() {
        now = LocalDate.now();

        testArticle = new Article();
        testArticle.setId(1L);
        testArticle.setTopic("Тестовая статья");
        testArticle.setText("Содержание тестовой статьи");
        testArticle.setPublicationDate(now);

        Tag tag1 = new Tag();
        tag1.setId(1L);
        tag1.setCode("тест");

        Tag tag2 = new Tag();
        tag2.setId(2L);
        tag2.setCode("java");

        testArticle.setTags(List.of(tag1, tag2));

        testArticleDto = new ArticleDto(
                1L,
                "Тестовая статья",
                now,
                "Содержание тестовой статьи",
                List.of("тест", "java")
        );
    }

    @Test
    void searchArticles_WithTagsAndDateRange_ShouldCallFindByTagsAndDateBetween() {
        List<String> tags = List.of("тест", "java");
        LocalDate from = now.minusDays(10);
        LocalDate to = now;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Article> expectedPage = new PageImpl<>(List.of(testArticle));

        when(articleRepository.findByTagsAndDateBetween(eq(tags), eq(from), eq(to), eq(pageable)))
                .thenReturn(expectedPage);

        Page<ArticleDto> result = articleService.searchArticles(null, null, null, from, to, tags, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(articleRepository, times(1)).findByTagsAndDateBetween(tags, from, to, pageable);
        verify(articleRepository, never()).findByTags(any(), any());
        verify(articleRepository, never()).findByPublicationDate(any(), any());
    }

    @Test
    void searchArticles_WithTagsOnly_ShouldCallFindByTags() {
        List<String> tags = List.of("тест", "java");
        Pageable pageable = PageRequest.of(0, 10);
        Page<Article> expectedPage = new PageImpl<>(List.of(testArticle));

        when(articleRepository.findByTags(eq(tags), eq(pageable)))
                .thenReturn(expectedPage);

        Page<ArticleDto> result = articleService.searchArticles(null, null, null, null, null, tags, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(articleRepository, times(1)).findByTags(tags, pageable);
        verify(articleRepository, never()).findByTagsAndDateBetween(any(), any(), any(), any());
    }

    @Test
    void searchArticles_WithExactDate_ShouldCallFindByPublicationDate() {
        LocalDate date = now;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Article> expectedPage = new PageImpl<>(List.of(testArticle));

        when(articleRepository.findByPublicationDate(eq(date), eq(pageable)))
                .thenReturn(expectedPage);

        Page<ArticleDto> result = articleService.searchArticles(null, null, date, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(articleRepository, times(1)).findByPublicationDate(date, pageable);
    }

    @Test
    void searchArticles_WithDateRange_ShouldCallFindByPublicationDateBetween() {
        LocalDate from = now.minusDays(10);
        LocalDate to = now;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Article> expectedPage = new PageImpl<>(List.of(testArticle));

        when(articleRepository.findByPublicationDateBetween(eq(from), eq(to), eq(pageable)))
                .thenReturn(expectedPage);

        Page<ArticleDto> result = articleService.searchArticles(null, null, null, from, to, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(articleRepository, times(1)).findByPublicationDateBetween(from, to, pageable);
    }

    @Test
    void searchArticles_WithTopic_ShouldCallFindByTopicContainingIgnoreCase() {
        String topic = "тест";
        Pageable pageable = PageRequest.of(0, 10);
        Page<Article> expectedPage = new PageImpl<>(List.of(testArticle));

        when(articleRepository.findByTopicContainingIgnoreCase(eq(topic), eq(pageable)))
                .thenReturn(expectedPage);

        Page<ArticleDto> result = articleService.searchArticles(topic, null, null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(articleRepository, times(1)).findByTopicContainingIgnoreCase(topic, pageable);
    }

    @Test
    void searchArticles_WithText_ShouldCallFindByTextContainingIgnoreCase() {
        String text = "содержание";
        Pageable pageable = PageRequest.of(0, 10);
        Page<Article> expectedPage = new PageImpl<>(List.of(testArticle));

        when(articleRepository.findByTextContainingIgnoreCase(eq(text), eq(pageable)))
                .thenReturn(expectedPage);

        Page<ArticleDto> result = articleService.searchArticles(null, text, null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(articleRepository, times(1)).findByTextContainingIgnoreCase(text, pageable);
    }

    @Test
    void searchArticles_WithNoFilters_ShouldCallFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Article> expectedPage = new PageImpl<>(List.of(testArticle));

        doReturn(expectedPage).when(articleRepository).findAll(any(Pageable.class));

        Page<ArticleDto> result = articleService.searchArticles(null, null, null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(articleRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void searchArticles_WithInvalidDateRange_ShouldThrowException() {
        LocalDate from = now;
        LocalDate to = now.minusDays(10);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> articleService.searchArticles(null, null, null, from, to, null, PageRequest.of(0, 10))
        );

        assertEquals("Дата начала не может быть позже даты окончания", exception.getMessage());
        verify(articleRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getArticleById_WhenArticleExists_ShouldReturnArticle() {
        Long id = 1L;
        when(articleRepository.findById(id)).thenReturn(Optional.of(testArticle));

        ArticleDto result = articleService.getArticleById(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals("Тестовая статья", result.getTopic());
        assertEquals(now, result.getPublicationDate());
        assertEquals("Содержание тестовой статьи", result.getText());
        assertEquals(2, result.getTags().size());
        assertTrue(result.getTags().contains("тест"));
        assertTrue(result.getTags().contains("java"));

        verify(articleRepository, times(1)).findById(id);
    }

    @Test
    void getArticleById_WhenArticleDoesNotExist_ShouldThrowException() {
        Long id = 999L;
        when(articleRepository.findById(id)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> articleService.getArticleById(id)
        );

        assertEquals("Статья не найдена", exception.getMessage());
        verify(articleRepository, times(1)).findById(id);
    }

    @Test
    void createArticle_WithValidData_ShouldCreateAndReturnArticle() {
        ArticleDto newArticleDto = new ArticleDto(
                null,
                "Новая статья",
                now,
                "Содержание новой статьи",
                List.of("новый", "тест")
        );

        when(articleRepository.existsByTopic("Новая статья")).thenReturn(false);
        when(tagRepository.findByCode("новый")).thenReturn(Optional.empty());
        when(tagRepository.findByCode("тест")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            tag.setId(3L);
            return tag;
        });
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article article = invocation.getArgument(0);
            article.setId(2L);
            return article;
        });

        ArticleDto result = articleService.createArticle(newArticleDto);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("Новая статья", result.getTopic());
        assertEquals(now, result.getPublicationDate());
        assertEquals("Содержание новой статьи", result.getText());
        assertEquals(2, result.getTags().size());

        verify(articleRepository, times(1)).existsByTopic("Новая статья");
        verify(tagRepository, times(1)).findByCode("новый");
        verify(tagRepository, times(1)).findByCode("тест");
        verify(tagRepository, times(2)).save(any(Tag.class));
        verify(articleRepository, times(1)).save(any(Article.class));
    }

    @Test
    void createArticle_WithExistingTopic_ShouldThrowException() {
        when(articleRepository.existsByTopic("Тестовая статья")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> articleService.createArticle(testArticleDto)
        );

        assertEquals("Статья с такой темой уже существует", exception.getMessage());
        verify(articleRepository, times(1)).existsByTopic("Тестовая статья");
        verify(articleRepository, never()).save(any());
    }

    @Test
    void createArticle_WithNullPublicationDate_ShouldSetCurrentDate() {
        ArticleDto newArticleDto = new ArticleDto(
                null,
                "Новая статья",
                null,
                "Содержание новой статьи",
                List.of("тест")
        );

        when(articleRepository.existsByTopic("Новая статья")).thenReturn(false);
        when(tagRepository.findByCode("тест")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            tag.setId(3L);
            return tag;
        });
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article article = invocation.getArgument(0);
            article.setId(2L);
            return article;
        });

        ArticleDto result = articleService.createArticle(newArticleDto);

        assertNotNull(result);
        assertEquals(LocalDate.now(), result.getPublicationDate());

        verify(articleRepository, times(1)).save(articleCaptor.capture());
        Article savedArticle = articleCaptor.getValue();
        assertEquals(LocalDate.now(), savedArticle.getPublicationDate());
    }

    @Test
    void createArticle_WithExistingTags_ShouldUseExistingTags() {
        ArticleDto newArticleDto = new ArticleDto(
                null,
                "Новая статья",
                now,
                "Содержание новой статьи",
                List.of("тест", "java")
        );

        Tag existingTag1 = new Tag();
        existingTag1.setId(1L);
        existingTag1.setCode("тест");

        Tag existingTag2 = new Tag();
        existingTag2.setId(2L);
        existingTag2.setCode("java");

        when(articleRepository.existsByTopic("Новая статья")).thenReturn(false);
        when(tagRepository.findByCode("тест")).thenReturn(Optional.of(existingTag1));
        when(tagRepository.findByCode("java")).thenReturn(Optional.of(existingTag2));
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article article = invocation.getArgument(0);
            article.setId(2L);
            return article;
        });

        ArticleDto result = articleService.createArticle(newArticleDto);

        assertNotNull(result);
        assertEquals(2L, result.getId());

        verify(tagRepository, times(1)).findByCode("тест");
        verify(tagRepository, times(1)).findByCode("java");
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void updateArticle_WithValidData_ShouldUpdateAndReturnArticle() {
        ArticleDto updateDto = new ArticleDto(
                1L,
                "Обновленная тема",
                now.minusDays(1),
                "Обновленное содержание",
                List.of("тест", "обновлено")
        );

        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        when(articleRepository.existsByTopic("Обновленная тема")).thenReturn(false);
        when(tagRepository.findByCode("тест")).thenReturn(Optional.of(testArticle.getTags().get(0)));
        when(tagRepository.findByCode("обновлено")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            tag.setId(3L);
            return tag;
        });
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArticleDto result = articleService.updateArticle(updateDto);

        assertNotNull(result);
        assertEquals("Обновленная тема", result.getTopic());
        assertEquals(now.minusDays(1), result.getPublicationDate());
        assertEquals("Обновленное содержание", result.getText());
        assertEquals(2, result.getTags().size());

        verify(articleRepository, times(1)).findById(1L);
        verify(articleRepository, times(1)).existsByTopic("Обновленная тема");
        verify(tagRepository, times(1)).save(any(Tag.class));
        verify(articleRepository, times(1)).save(any(Article.class));
    }

    @Test
    void updateArticle_WithNullId_ShouldThrowException() {
        ArticleDto updateDto = new ArticleDto(
                null,
                "Обновленная тема",
                now,
                "Содержание",
                List.of("тест")
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> articleService.updateArticle(updateDto)
        );

        assertEquals("ID статьи не может быть null при обновлении", exception.getMessage());
        verify(articleRepository, never()).findById(any());
    }

    @Test
    void updateArticle_WhenArticleNotFound_ShouldThrowException() {
        ArticleDto updateDto = new ArticleDto(
                999L,
                "Обновленная тема",
                now,
                "Содержание",
                List.of("тест")
        );

        when(articleRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> articleService.updateArticle(updateDto)
        );

        assertEquals("Статья не найдена", exception.getMessage());
        verify(articleRepository, times(1)).findById(999L);
    }

    @Test
    void updateArticle_WithExistingTopic_ShouldThrowException() {
        ArticleDto updateDto = new ArticleDto(
                1L,
                "Существующая тема",
                now,
                "Содержание",
                List.of("тест")
        );

        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        when(articleRepository.existsByTopic("Существующая тема")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> articleService.updateArticle(updateDto)
        );

        assertEquals("Статья с такой темой уже существует", exception.getMessage());
        verify(articleRepository, times(1)).existsByTopic("Существующая тема");
        verify(articleRepository, never()).save(any(Article.class));
    }

    @Test
    void updateArticle_WithPartialData_ShouldUpdateOnlyProvidedFields() {
        ArticleDto updateDto = new ArticleDto(
                1L,
                null,
                null,
                "Только текст обновлен",
                null
        );

        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArticleDto result = articleService.updateArticle(updateDto);

        assertNotNull(result);
        assertEquals("Тестовая статья", result.getTopic());
        assertEquals(now, result.getPublicationDate());
        assertEquals("Только текст обновлен", result.getText());
        assertEquals(2, result.getTags().size());

        verify(articleRepository, never()).existsByTopic(any());
        verify(tagRepository, never()).findByCode(any());
    }

    @Test
    void deleteArticle_WhenArticleExists_ShouldDelete() {
        Long id = 1L;
        when(articleRepository.existsById(id)).thenReturn(true);
        doNothing().when(articleRepository).deleteById(id);

        articleService.deleteArticle(id);

        verify(articleRepository, times(1)).existsById(id);
        verify(articleRepository, times(1)).deleteById(id);
    }

    @Test
    void deleteArticle_WhenArticleDoesNotExist_ShouldThrowException() {
        Long id = 999L;
        when(articleRepository.existsById(id)).thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> articleService.deleteArticle(id)
        );

        assertEquals("Статья не найдена", exception.getMessage());
        verify(articleRepository, times(1)).existsById(id);
        verify(articleRepository, never()).deleteById(any());
    }
}