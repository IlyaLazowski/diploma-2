package com.fakel.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public class DeleteMessagesRequest {

    @NotNull(message = "Список ID сообщений обязателен")
    private List<Long> messageIds;

    public List<Long> getMessageIds() { return messageIds; }
    public void setMessageIds(List<Long> messageIds) { this.messageIds = messageIds; }
}