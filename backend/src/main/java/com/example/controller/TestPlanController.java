package com.example.controller;

import com.example.common.Result;
import com.example.entity.TestPlan;
import com.example.service.TestPlanService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test-plans")
public class TestPlanController {

    @Resource
    private TestPlanService planService;

    @GetMapping
    public Result list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) Integer moduleId,
            @RequestParam(required = false) Integer ownerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Map<String, Object> params = new HashMap<>();
        if (name != null && !name.isEmpty()) params.put("name", name);
        if (projectId != null) params.put("projectId", projectId);
        if (moduleId != null) params.put("moduleId", moduleId);
        if (ownerId != null) params.put("ownerId", ownerId);
        if (status != null && !status.isEmpty()) params.put("status", status);
        if (dateFrom != null && !dateFrom.isEmpty()) params.put("dateFrom", dateFrom);
        if (dateTo != null && !dateTo.isEmpty()) params.put("dateTo", dateTo);

        PageInfo<TestPlan> pageInfo = planService.selectByPage(params, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        TestPlan p = planService.selectById(id);
        return Result.success(p);
    }

    @PostMapping
    public Result add(@RequestBody TestPlan plan) {
        planService.addTestPlan(plan);
        return Result.success(plan);
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @RequestBody TestPlan plan) {
        plan.setId(id);
        planService.updateTestPlan(plan);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        planService.deleteTestPlan(id);
        return Result.success();
    }

    /**
     * 覆盖式设置计划下的用例（请求体为用例 id 列表）
     * POST /test-plans/{id}/testcases  body: [1,2,3]
     */
    @PostMapping("/{id}/testcases")
    public Result setTestCases(@PathVariable Integer id, @RequestBody List<Integer> caseIds) {
        planService.setTestCasesForPlan(id, caseIds);
        return Result.success();
    }

    /**
     * 查询计划下的用例 id 列表
     */
    @GetMapping("/{id}/testcases")
    public Result getTestCases(@PathVariable Integer id) {
        List<Integer> ids = planService.getTestCaseIdsByPlanId(id);
        return Result.success(ids);
    }
}
