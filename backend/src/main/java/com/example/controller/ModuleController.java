package com.example.controller;

import com.example.security.RequiresPermission;
import com.example.service.ModuleService;
import com.example.common.Result;
import com.example.entity.Module;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/modules")
public class ModuleController {

    @Resource
    private ModuleService moduleService;

    @GetMapping
    public Result selectModules(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        PageInfo<Module> pageInfo;
        if (projectId != null) {
            pageInfo = moduleService.selectByProjectId(projectId, name, pageNum, pageSize);
        } else if (name != null && !name.isEmpty()) {
            pageInfo = moduleService.selectByName(name, pageNum, pageSize);
        } else {
            pageInfo = moduleService.selectByPage(pageNum, pageSize);
        }
        return Result.success(pageInfo);
    }


    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        Module module = moduleService.selectById(id);
        return Result.success(module);
    }

    @RequiresPermission("PROJECT_MANAGE")
    @PostMapping
    public Result addModule(@RequestBody Module module) {
        moduleService.addModule(module);
        return Result.success();
    }

    @RequiresPermission("PROJECT_MANAGE")
    @PutMapping("/{id}")
    public Result updateModule(@PathVariable Integer id, @RequestBody Module module) {
        module.setId(id);
        moduleService.updateModule(module);
        return Result.success();
    }

    @RequiresPermission("PROJECT_MANAGE")
    @DeleteMapping("/{id}")
    public Result deleteModule(@PathVariable Integer id) {
        moduleService.deleteModule(id);
        return Result.success();
    }
}
