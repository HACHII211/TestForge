package com.example.controller;

import com.example.service.TestCaseService;
import com.example.common.Result;
import com.example.entity.TestCase;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/testcases")
public class TestCaseController {

    @Resource
    private TestCaseService testCaseService;

    /**
     * 分页查询用例，支持按 title 模糊搜索
     */
    @GetMapping
    public Result selectTestCases(
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        PageInfo<TestCase> pageInfo;
        if (title != null && !title.isEmpty()) {
            pageInfo = testCaseService.selectByTitle(title, pageNum, pageSize);
        } else {
            pageInfo = testCaseService.selectByPage(pageNum, pageSize);
        }
        return Result.success(pageInfo);
    }

    /**
     * 根据 id 获取单条用例
     */
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        TestCase testCase = testCaseService.selectById(id);
        return Result.success(testCase);
    }

    /**
     * 新增用例
     */
    @PostMapping
    public Result addTestCase(@RequestBody TestCase testCase) {
        testCaseService.addTestCase(testCase);
        return Result.success();
    }

    /**
     * 更新用例（路径包含 id，保险起见将请求体 id 覆盖为 path id）
     */
    @PutMapping("/{id}")
    public Result updateTestCase(@PathVariable Integer id, @RequestBody TestCase testCase) {
        testCase.setId(id);
        testCaseService.updateTestCase(testCase);
        return Result.success();
    }

    /**
     * 删除用例
     */
    @DeleteMapping("/{id}")
    public Result deleteTestCase(@PathVariable Integer id) {
        testCaseService.deleteTestCase(id);
        return Result.success();
    }
}
