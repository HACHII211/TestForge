package com.example.mapper.mysql;

import com.example.entity.TestPlan;
import java.util.List;
import java.util.Map;

public interface TestPlanMapper {

    /**
     * 条件分页查询（参数放在 map 中：name/projectId/moduleId/ownerId/status/dateFrom/dateTo）
     * PageHelper 在 Service 中启动分页
     */
    List<TestPlan> selectAll(Map<String, Object> params);

    /**
     * 按名称模糊查询（可单独使用）
     */
    List<TestPlan> selectByName(String name);

    /**
     * 根据 id 查询
     */
    TestPlan selectById(Integer id);

    /**
     * 插入并回填 id
     */
    int insertTestPlan(TestPlan plan);

    /**
     * 更新
     */
    int updateTestPlan(TestPlan plan);

    /**
     * 删除
     */
    int deleteTestPlan(Integer id);

    /**
     * 关联表操作：删除计划对应的所有用例（覆盖式操作时会用到）
     */
    int deletePlanTestcases(Integer planId);

    /**
     * 关联表操作：批量插入计划-用例关联（使用 foreach）
     * 参数：map 中包含 planId 与 caseIds（list）
     */
    int insertPlanTestcases(Map<String, Object> params);

    /**
     * 查询某计划下的用例 id 列表（也可以返回用例基本信息）
     */
    List<Integer> selectCaseIdsByPlanId(Integer planId);

    /**
     * 若你希望返回详细用例信息，可以加一个方法（返回 test_case 行）
     */
    // List<TestCase> selectTestCasesByPlanId(Integer planId);
}
