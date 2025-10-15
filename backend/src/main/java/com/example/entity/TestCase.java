package com.example.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class TestCase implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer projectId;    // 关联项目 ID，可空
    private Integer moduleId;     // 关联模块 ID，可空
    private String title;         // 用例标题
    private String description;   // 用例描述
    private String preCondition;  // 前置条件
    private String steps;         // 执行步骤（可按行或 JSON 存储）
    private String expectedResult;// 预期结果
    private Integer createdBy;    // 创建人 ID，可空
    private Date createdAt;
    private Date updatedAt;
    private String status;   // 用例状态
    private String priority; // 用例优先级
    /** 非持久化字段（用于前端回显或接口需要的额外信息） */
    private String projectName;    // 方便前端展示
    private String moduleName;     // 方便前端展示
    private String createdByName;  // 创建人用户名/昵称
    private List<Integer> defectIds; // 关联的缺陷 id 列表（非持久化）


// 在 非持久化字段 之后或合适位置添加 getter/setter

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public TestCase() {}

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

    public Integer getModuleId() {
        return moduleId;
    }

    public void setModuleId(Integer moduleId) {
        this.moduleId = moduleId;
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

    public String getPreCondition() {
        return preCondition;
    }

    public void setPreCondition(String preCondition) {
        this.preCondition = preCondition;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
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

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public List<Integer> getDefectIds() {
        return defectIds;
    }

    public void setDefectIds(List<Integer> defectIds) {
        this.defectIds = defectIds;
    }

    @Override
    public String toString() {
        return "TestCase{" +
                "id=" + id +
                ", projectId=" + projectId +
                ", moduleId=" + moduleId +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", preCondition='" + preCondition + '\'' +
                ", steps='" + steps + '\'' +
                ", expectedResult='" + expectedResult + '\'' +
                ", createdBy=" + createdBy +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", status='" + status + '\'' +
                ", priority='" + priority + '\'' +
                ", projectName='" + projectName + '\'' +
                ", moduleName='" + moduleName + '\'' +
                ", createdByName='" + createdByName + '\'' +
                ", defectIds=" + defectIds +
                '}';
    }
}
