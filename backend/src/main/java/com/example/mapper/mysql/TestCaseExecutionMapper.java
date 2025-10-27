package com.example.mapper.mysql;

import com.example.entity.TestCaseExecution;
import java.util.List;
import java.util.Map;

public interface TestCaseExecutionMapper {

    /**
     * 分页/条件查询（params 可包含 title/testCaseId/projectId/moduleId/status/executedBy/dateFrom/dateTo）
     * PageHelper 负责分页
     */
    List<TestCaseExecution> selectAll(Map<String, Object> params);

    /**
     * 根据 id 查询
     */
    TestCaseExecution selectById(Integer id);

    /**
     * 插入（useGeneratedKeys 回填 id）
     */
    int insertExecution(TestCaseExecution execution);

    /**
     * 更新
     */
    int updateExecution(TestCaseExecution execution);

    /**
     * 删除
     */
    int deleteExecution(Integer id);

    /**
     * 批量删除（传入 id 列表）
     */
    int deleteBatch(List<Integer> ids);
}
