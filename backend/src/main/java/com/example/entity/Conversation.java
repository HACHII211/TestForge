package com.example.entity;

import java.util.Date;

public class Conversation {

    private Long id;
    private String conversationUuid; // UUID as string
    private Long userId;
    private String assistantId;
    private String messages;   // jsonb as string
    private String metadata;   // jsonb as string
    private Date lastActivity;
    private Date createdAt;
    private Date updatedAt;

    public Conversation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getConversationUuid() { return conversationUuid; }
    public void setConversationUuid(String conversationUuid) { this.conversationUuid = conversationUuid; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getAssistantId() { return assistantId; }
    public void setAssistantId(String assistantId) { this.assistantId = assistantId; }

    public String getMessages() { return messages; }
    public void setMessages(String messages) { this.messages = messages; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Date getLastActivity() { return lastActivity; }
    public void setLastActivity(Date lastActivity) { this.lastActivity = lastActivity; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Conversation{" +
                "id=" + id +
                ", conversationUuid='" + conversationUuid + '\'' +
                ", userId=" + userId +
                ", assistantId='" + assistantId + '\'' +
                ", lastActivity=" + lastActivity +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
