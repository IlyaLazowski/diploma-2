package com.fakel.dto;

import java.time.LocalDate;
import java.util.List;

public class ArticleDto {
    private Long id;
    private String topic;
    private LocalDate publicationDate;
    private String text;
    private List<String> tags;

    public ArticleDto() {}

    public ArticleDto(Long id, String topic, LocalDate publicationDate, String text, List<String> tags) {
        this.id = id;
        this.topic = topic;
        this.publicationDate = publicationDate;
        this.text = text;
        this.tags = tags;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public LocalDate getPublicationDate() { return publicationDate; }
    public void setPublicationDate(LocalDate publicationDate) { this.publicationDate = publicationDate; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}