package com.fakel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SendMessageRequest {

    @NotNull(message = "ID получателя обязателен")
    private Long recipientId;

    @NotBlank(message = "Тема сообщения обязательна")
    private String topic;

    @NotBlank(message = "Текст сообщения обязателен")
    private String text;

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}