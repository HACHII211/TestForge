package com.example.controller;

import com.example.common.Result;
import com.example.entity.DefectStatus;
import com.example.service.DefectStatusService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/defect-statuses")
public class DefectStatusController {

    @Resource
    private DefectStatusService service;

    /**
     * 获取全部缺陷状态（不分页）
     */
    @GetMapping
    public Result list() {
        List<DefectStatus> list = service.selectAll();
        return Result.success(list);
    }

    /**
     * 根据 id 查询单条
     */
    @GetMapping("/{id}")
    public Result get(@PathVariable Integer id) {
        DefectStatus s = service.selectById(id);
        return Result.success(s);
    }

    /**
     * 新增状态
     */
    @PostMapping
    public Result add(@RequestBody DefectStatus status) {
        service.add(status);
        return Result.success(status);
    }

    /**
     * 更新状态
     */
    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @RequestBody DefectStatus status) {
        status.setId(id);
        service.update(status);
        return Result.success();
    }

    /**
     * 删除状态
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        service.delete(id);
        return Result.success();
    }
}
