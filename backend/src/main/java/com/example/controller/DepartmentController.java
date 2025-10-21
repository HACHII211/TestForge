package com.example.controller;

import com.example.security.RequiresPermission;
import com.example.service.DepartmentService;
import com.example.common.Result;
import com.example.entity.Department;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/department")
public class DepartmentController {

    @Resource
    private DepartmentService departmentService;

    @GetMapping
    public Result selectDepartments(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Department> pageInfo;
        if (name != null && !name.isEmpty()) {
            pageInfo = departmentService.selectByName(name, pageNum, pageSize);
        } else {
            pageInfo = departmentService.selectByPage(pageNum, pageSize);
        }
        return Result.success(pageInfo);
    }

    @GetMapping("/{id}")
    public Result selectById(@PathVariable Integer id) {
        Department dept = departmentService.selectById(id);
        return Result.success(dept);
    }

    @RequiresPermission("DEPARTMENT_MANAGE")
    @PostMapping
    public Result addDepartment(@RequestBody Department department) {
        departmentService.addDepartment(department);
        return Result.success();
    }

    @RequiresPermission("DEPARTMENT_MANAGE")
    @DeleteMapping("/{id}")
    public Result deleteDepartment(@PathVariable Integer id) {
        departmentService.deleteDepartment(id);
        return Result.success();
    }

    @RequiresPermission("DEPARTMENT_MANAGE")
    @PutMapping("/{id}")
    public Result updateDepartment(@PathVariable Integer id, @RequestBody Department department) {
        // 确保 id 一致（优先 path）
        department.setId(id);
        departmentService.updateDepartment(department);
        return Result.success();
    }
}
