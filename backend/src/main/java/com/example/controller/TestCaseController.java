package com.example.controller;

import com.example.security.RequiresPermission;
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

    @RequiresPermission("TESTCASE_MANAGE")
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        TestCase testCase = testCaseService.selectById(id);
        return Result.success(testCase);
    }

    @RequiresPermission("TESTCASE_MANAGE")
    @PostMapping
    public Result addTestCase(@RequestBody TestCase testCase) {
        testCaseService.addTestCase(testCase);
        return Result.success();
    }

    @RequiresPermission("TESTCASE_MANAGE")
    @PutMapping("/{id}")
    public Result updateTestCase(@PathVariable Integer id, @RequestBody TestCase testCase) {
        testCase.setId(id);
        testCaseService.updateTestCase(testCase);
        return Result.success();
    }

    @RequiresPermission("TESTCASE_MANAGE")
    @DeleteMapping("/{id}")
    public Result deleteTestCase(@PathVariable Integer id) {
        testCaseService.deleteTestCase(id);
        return Result.success();
    }
}
