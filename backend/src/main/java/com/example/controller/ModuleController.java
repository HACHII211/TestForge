package com.example.controller;

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

    /**
     * 分页查询模块，支持按 name 模糊搜索，以及按 projectId 过滤
     * 示例：
     * GET /modules?projectId=1&name=auth&pageNum=1&pageSize=10
     */
    @GetMapping
    public Result selectModules(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        PageInfo<Module> pageInfo;
        if (projectId != null) {
            // 当提供 projectId 时，优先按 projectId 查询（可再加 name 过滤）
            pageInfo = moduleService.selectByProjectId(projectId, name, pageNum, pageSize);
        } else if (name != null && !name.isEmpty()) {
            pageInfo = moduleService.selectByName(name, pageNum, pageSize);
        } else {
            pageInfo = moduleService.selectByPage(pageNum, pageSize);
        }
        return Result.success(pageInfo);
    }

    /**
     * 根据 id 获取单个模块
     */
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        Module module = moduleService.selectById(id);
        return Result.success(module);
    }

    /**
     * 新增模块
     */
    @PostMapping
    public Result addModule(@RequestBody Module module) {
        moduleService.addModule(module);
        return Result.success();
    }

    /**
     * 更新模块（路径包含 id，将请求体 id 覆盖为 path id）
     */
    @PutMapping("/{id}")
    public Result updateModule(@PathVariable Integer id, @RequestBody Module module) {
        module.setId(id);
        moduleService.updateModule(module);
        return Result.success();
    }

    /**
     * 删除模块
     */
    @DeleteMapping("/{id}")
    public Result deleteModule(@PathVariable Integer id) {
        moduleService.deleteModule(id);
        return Result.success();
    }
}
