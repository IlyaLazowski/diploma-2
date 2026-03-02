package com.fakel.dto;

import java.time.LocalDateTime;

public class MessageDto {
    private Long id;
    private Long senderId;
    private String senderName;
    private Long recipientId;
    private String recipientName;
    private String topic;
    private String text;
    private LocalDateTime sentAt;
    private Boolean isReading;

    public MessageDto() {}

    public MessageDto(Long id, Long senderId, String senderName, Long recipientId, String recipientName,
                      String topic, String text, LocalDateTime sentAt, Boolean isReading) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.recipientId = recipientId;
        this.recipientName = recipientName;
        this.topic = topic;
        this.text = text;
        this.sentAt = sentAt;
        this.isReading = isReading;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public Boolean getIsReading() { return isReading; }
    public void setIsReading(Boolean isReading) { this.isReading = isReading; }
}