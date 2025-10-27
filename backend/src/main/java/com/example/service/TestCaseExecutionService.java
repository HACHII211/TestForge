package com.example.service;

import com.example.entity.TestCaseExecution;
import com.example.mapper.mysql.TestCaseExecutionMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class TestCaseExecutionService {

    @Resource
    private TestCaseExecutionMapper mapper;

    /**
     * 分页+条件查询
     * params: 可以传入 testCaseId, projectId, moduleId, status, executedBy, dateFrom, dateTo
     */
    public PageInfo<TestCaseExecution> selectByPage(Map<String, Object> params, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<TestCaseExecution> list = mapper.selectAll(params == null ? new HashMap<>() : params);
        return PageInfo.of(list);
    }

    public TestCaseExecution selectById(Integer id) {
        return mapper.selectById(id);
    }

    @Transactional
    public void addExecution(TestCaseExecution execution) {
        mapper.insertExecution(execution);
    }

    @Transactional
    public void updateExecution(TestCaseExecution execution) {
        mapper.updateExecution(execution);
    }

    @Transactional
    public void deleteExecution(Integer id) {
        mapper.deleteExecution(id);
    }

    @Transactional
    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        mapper.deleteBatch(ids);
    }
}
