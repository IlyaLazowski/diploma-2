package com.fakel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CreateArticleRequest {

    @NotBlank(message = "Тема статьи обязательна")
    private String topic;

    @NotBlank(message = "Текст статьи обязателен")
    private String text;

    private List<String> tags; // коды тегов

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}