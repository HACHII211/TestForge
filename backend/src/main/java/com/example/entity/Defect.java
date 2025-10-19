package com.example.entity;

import java.util.Date;

public class Defect {

    private Integer id;            // 缺陷ID
    private String title;          // 标题
    private String description;    // 描述
    private Integer statusId;      // 状态ID
    private Integer priorityId;    // 优先级ID
    private Integer createdBy;     // 提交人ID
    private Integer assignedTo;    // 处理人ID
    private Date createdAt;        // 创建时间
    private Date updatedAt;        // 更新时间
    private Integer projectId;
    private Integer moduleId;

    public Defect() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStatusId() {
        return statusId;
    }

    public void setStatusId(Integer statusId) {
        this.statusId = statusId;
    }

    public Integer getPriorityId() {
        return priorityId;
    }

    public void setPriorityId(Integer priorityId) {
        this.priorityId = priorityId;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Integer assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) { this.projectId = projectId; }

    public Integer getModuleId() { return moduleId; }

    public void setModuleId(Integer moduleId) { this.moduleId = moduleId; }
    @Override
    public String toString() {
        return "Defect{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", statusId=" + statusId +
                ", priorityId=" + priorityId +
                ", createdBy=" + createdBy +
                ", assignedTo=" + assignedTo +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
