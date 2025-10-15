package com.example.controller;

import com.example.service.ProjectService;
import com.example.common.Result;
import com.example.entity.Project;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    @Resource
    private ProjectService projectService;

    /**
     * 分页查询项目，支持按 name 模糊搜索
     */
    @GetMapping
    public Result selectProjects(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        PageInfo<Project> pageInfo;
        if (name != null && !name.isEmpty()) {
            pageInfo = projectService.selectByName(name, pageNum, pageSize);
        } else {
            pageInfo = projectService.selectByPage(pageNum, pageSize);
        }
        return Result.success(pageInfo);
    }

    /**
     * 根据 id 获取单个项目
     */
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        Project project = projectService.selectById(id);
        return Result.success(project);
    }

    /**
     * 新增项目
     */
    @PostMapping
    public Result addProject(@RequestBody Project project) {
        projectService.addProject(project);
        return Result.success();
    }

    /**
     * 更新项目（路径包含 id，保险起见将请求体 id 覆盖为 path id）
     */
    @PutMapping("/{id}")
    public Result updateProject(@PathVariable Integer id, @RequestBody Project project) {
        project.setId(id);
        projectService.updateProject(project);
        return Result.success();
    }

    /**
     * 删除项目
     */
    @DeleteMapping("/{id}")
    public Result deleteProject(@PathVariable Integer id) {
        projectService.deleteProject(id);
        return Result.success();
    }
}
