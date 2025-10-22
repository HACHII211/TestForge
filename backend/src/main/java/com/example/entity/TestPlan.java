package com.example.entity;

import java.util.Date;

public class TestPlan {

    private Integer id;
    private String name;
    private Integer projectId;
    private Integer moduleId;
    private String description;
    private Integer ownerId;
    private String status; // Draft/Active/Completed/Cancelled
    private Date startDate;
    private Date endDate;
    private Date createdAt;
    private Date updatedAt;

    public TestPlan() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }

    public Integer getModuleId() { return moduleId; }
    public void setModuleId(Integer moduleId) { this.moduleId = moduleId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getOwnerId() { return ownerId; }
    public void setOwnerId(Integer ownerId) { this.ownerId = ownerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "TestPlan{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", projectId=" + projectId +
                ", moduleId=" + moduleId +
                ", ownerId=" + ownerId +
                ", status='" + status + '\'' +
                '}';
    }
}
