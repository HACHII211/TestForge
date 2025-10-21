package com.example.controller;

import com.example.security.RequiresPermission;
import com.example.service.ProjectService;
import com.example.common.Result;
import com.example.entity.Project;
import com.example.entity.User;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    @Resource
    private ProjectService projectService;

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


    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        Project project = projectService.selectById(id);
        return Result.success(project);
    }

    @GetMapping("/{id}/users")
    public Result getProjectUsersByProjectId(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        PageInfo<User> pageInfo = projectService.selectProjectUsers(id, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @RequiresPermission("PROJECT_MANAGE")
    @PostMapping("/{id}/users")
    public Result addProjectUser(@PathVariable Integer id, @RequestBody User user) {
        projectService.addUserToProject(id, user.getId());
        return Result.success();
    }

    @RequiresPermission("PROJECT_MANAGE")
    @DeleteMapping("/{id}/users/{userId}")
    public Result removeProjectUser(@PathVariable Integer id, @PathVariable Long userId) {
        projectService.removeUserFromProject(id, userId);
        return Result.success();
    }

    @RequiresPermission("PROJECT_MANAGE")
    @DeleteMapping("/{id}/users")
    public Result removeProjectUsersBatch(@PathVariable Integer id, @RequestBody List<Long> userIds) {
        projectService.removeUsersFromProjectBatch(id, userIds);
        return Result.success();
    }

    @RequiresPermission("PROJECT_MANAGE")
    @PostMapping
    public Result addProject(@RequestBody Project project) {
        projectService.addProject(project);
        return Result.success();
    }

    @RequiresPermission("PROJECT_MANAGE")
    @PutMapping("/{id}")
    public Result updateProject(@PathVariable Integer id, @RequestBody Project project) {
        project.setId(id);
        projectService.updateProject(project);
        return Result.success();
    }

    @RequiresPermission("PROJECT_MANAGE")
    @DeleteMapping("/{id}")
    public Result deleteProject(@PathVariable Integer id) {
        projectService.deleteProject(id);
        return Result.success();
    }
}
