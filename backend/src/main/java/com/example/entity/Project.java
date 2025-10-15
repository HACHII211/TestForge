package com.example.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Project implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;           // 项目ID
    private String name;          // 项目名称
    private String description;   // 项目描述
    private Date createdAt;       // 创建时间

    /** 非持久化字段，用于前端展示或接口返回 */
    private List<Integer> moduleIds;  // 项目下模块ID列表
    private List<String> moduleNames; // 项目下模块名称列表
    private List<Integer> userIds;    // 参与该项目的员工ID列表
    private List<String> userNames;   // 参与该项目的员工名称列表

    public Project() {}

    // ===== Getter / Setter =====
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public List<Integer> getModuleIds() {
        return moduleIds;
    }

    public void setModuleIds(List<Integer> moduleIds) {
        this.moduleIds = moduleIds;
    }

    public List<String> getModuleNames() {
        return moduleNames;
    }

    public void setModuleNames(List<String> moduleNames) {
        this.moduleNames = moduleNames;
    }

    public List<Integer> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Integer> userIds) {
        this.userIds = userIds;
    }

    public List<String> getUserNames() {
        return userNames;
    }

    public void setUserNames(List<String> userNames) {
        this.userNames = userNames;
    }

    @Override
    public String toString() {
        return "Project{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", moduleIds=" + moduleIds +
                ", moduleNames=" + moduleNames +
                ", userIds=" + userIds +
                ", userNames=" + userNames +
                '}';
    }
}
