package com.example.entity;

import java.util.Date;

public class TestCaseExecution {

    private Integer id;
    private Integer testCaseId;      // 用例ID
    private Integer executedBy;      // 执行人ID
    private Date executedAt;         // 执行时间
    private String status;           // 执行状态（Passed/Failed/Blocked/...）
    private String remarks;          // 执行备注
    private String environment;      // 执行环境
    private String actualResult;     // 实际结果

    // 以下为便利字段（查询时 LEFT JOIN 返回，前端展示用）
    private String testCaseTitle;
    private Integer projectId;
    private String projectName;
    private Integer moduleId;
    private String moduleName;
    private String executedByName;
    private String createdByName;

    public TestCaseExecution() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getTestCaseId() { return testCaseId; }
    public void setTestCaseId(Integer testCaseId) { this.testCaseId = testCaseId; }

    public Integer getExecutedBy() { return executedBy; }
    public void setExecutedBy(Integer executedBy) { this.executedBy = executedBy; }

    public Date getExecutedAt() { return executedAt; }
    public void setExecutedAt(Date executedAt) { this.executedAt = executedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public String getActualResult() { return actualResult; }
    public void setActualResult(String actualResult) { this.actualResult = actualResult; }

    public String getTestCaseTitle() { return testCaseTitle; }
    public void setTestCaseTitle(String testCaseTitle) { this.testCaseTitle = testCaseTitle; }

    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public Integer getModuleId() { return moduleId; }
    public void setModuleId(Integer moduleId) { this.moduleId = moduleId; }

    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }

    public String getExecutedByName() { return executedByName; }
    public void setExecutedByName(String executedByName) { this.executedByName = executedByName; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    @Override
    public String toString() {
        return "TestCaseExecution{" +
                "id=" + id +
                ", testCaseId=" + testCaseId +
                ", executedBy=" + executedBy +
                ", executedAt=" + executedAt +
                ", status='" + status + '\'' +
                ", environment='" + environment + '\'' +
                '}';
    }
}
