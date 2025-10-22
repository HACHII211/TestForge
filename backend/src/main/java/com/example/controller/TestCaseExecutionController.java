package com.example.controller;

import com.example.common.Result;
import com.example.entity.TestCaseExecution;
import com.example.service.TestCaseExecutionService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/executions")
public class TestCaseExecutionController {

    @Resource
    private TestCaseExecutionService executionService;

    @GetMapping
    public Result list(
            @RequestParam(required = false) Integer testCaseId,
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) Integer moduleId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer executedBy,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Map<String, Object> params = new HashMap<>();
        if (testCaseId != null) params.put("testCaseId", testCaseId);
        if (projectId != null) params.put("projectId", projectId);
        if (moduleId != null) params.put("moduleId", moduleId);
        if (status != null && !status.isEmpty()) params.put("status", status);
        if (executedBy != null) params.put("executedBy", executedBy);
        if (dateFrom != null && !dateFrom.isEmpty()) params.put("dateFrom", dateFrom);
        if (dateTo != null && !dateTo.isEmpty()) params.put("dateTo", dateTo);

        PageInfo<TestCaseExecution> pageInfo = executionService.selectByPage(params, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        TestCaseExecution ex = executionService.selectById(id);
        return Result.success(ex);
    }

    @PostMapping
    public Result add(@RequestBody TestCaseExecution execution) {
        executionService.addExecution(execution);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @RequestBody TestCaseExecution execution) {
        execution.setId(id);
        executionService.updateExecution(execution);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        executionService.deleteExecution(id);
        return Result.success();
    }

    @DeleteMapping("/deleteBatch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        executionService.deleteBatch(ids);
        return Result.success();
    }
}
