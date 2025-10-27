package com.example.entity;

import java.util.Date;

public class ConversationInfo {

    private Long id;
    private String name;
    private String conversationUuid;
    private Long userId;
    private Boolean isPrimary;
    private String visibility;
    private String description;
    private String tags; // json text
    private Date createdAt;
    private Date updatedAt;

    public ConversationInfo() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getConversationUuid() { return conversationUuid; }
    public void setConversationUuid(String conversationUuid) { this.conversationUuid = conversationUuid; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "ConversationInfo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", conversationUuid='" + conversationUuid + '\'' +
                ", userId=" + userId +
                ", isPrimary=" + isPrimary +
                '}';
    }
}
