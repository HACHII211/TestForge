package com.example.service;

import com.example.entity.TestPlan;
import com.example.mapper.TestPlanMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TestPlanService {

    @Resource
    private TestPlanMapper testPlanMapper;

    /**
     * 分页+条件查询
     * params: name, projectId, moduleId, ownerId, status, dateFrom, dateTo
     */
    public PageInfo<TestPlan> selectByPage(Map<String, Object> params, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<TestPlan> list = testPlanMapper.selectAll(params == null ? new HashMap<>() : params);
        return PageInfo.of(list);
    }

    public List<TestPlan> selectByName(String name) {
        return testPlanMapper.selectByName(name);
    }

    public TestPlan selectById(Integer id) {
        return testPlanMapper.selectById(id);
    }

    @Transactional
    public void addTestPlan(TestPlan plan) {
        testPlanMapper.insertTestPlan(plan);
    }

    @Transactional
    public void updateTestPlan(TestPlan plan) {
        testPlanMapper.updateTestPlan(plan);
    }

    @Transactional
    public void deleteTestPlan(Integer id) {
        // 删除计划时同时删除 plan-testcase 关联（如果需要）
        testPlanMapper.deletePlanTestcases(id);
        testPlanMapper.deleteTestPlan(id);
    }

    /**
     * 覆盖式设置计划下的用例：
     * 删除已有关联并插入新的 caseId 列表（事务中）
     */
    @Transactional
    public void setTestCasesForPlan(Integer planId, List<Integer> caseIds) {
        // 先删
        testPlanMapper.deletePlanTestcases(planId);
        if (caseIds == null || caseIds.isEmpty()) return;
        Map<String, Object> params = new HashMap<>();
        params.put("planId", planId);
        params.put("caseIds", caseIds);
        testPlanMapper.insertPlanTestcases(params);
    }

    public List<Integer> getTestCaseIdsByPlanId(Integer planId) {
        return testPlanMapper.selectCaseIdsByPlanId(planId);
    }
}
