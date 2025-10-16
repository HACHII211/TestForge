package com.example.entity;

import java.io.Serializable;
import java.util.Date;

public class Module implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer projectId;   // 所属项目 ID，可空
    private String name;         // 模块名称
    private String description;  // 模块描述
    private Date createdAt;      // 创建时间

    /** 非持久化字段，用于前端显示 */
    private String projectName;  // 所属项目名称

    public Module() {}

    // getter / setter
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @Override
    public String toString() {
        return "Module{" +
                "id=" + id +
                ", projectId=" + projectId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", projectName='" + projectName + '\'' +
                '}';
    }
}
