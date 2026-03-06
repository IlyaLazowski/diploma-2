package com.fakel.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public class UpdateMessageStatusRequest {

    @NotNull(message = "Список ID сообщений обязателен")
    private List<Long> messageIds;

    @NotNull(message = "Статус прочтения обязателен")
    private Boolean isReading;

    public List<Long> getMessageIds() {
        return messageIds;
    }

    public void setMessageIds(List<Long> messageIds) {
        this.messageIds = messageIds;
    }

    public Boolean getIsReading() {
        return isReading;
    }

    public void setIsReading(Boolean isReading) {
        this.isReading = isReading;
    }
}