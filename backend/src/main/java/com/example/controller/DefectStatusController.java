package com.example.controller;

import com.example.common.Result;
import com.example.entity.DefectStatus;
import com.example.security.RequiresPermission;
import com.example.service.DefectStatusService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/defect-statuses")
public class DefectStatusController {

    @Resource
    private DefectStatusService service;

    @GetMapping
    public Result list() {
        List<DefectStatus> list = service.selectAll();
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result get(@PathVariable Integer id) {
        DefectStatus s = service.selectById(id);
        return Result.success(s);
    }

    @RequiresPermission("DEFECT_MANAGE")
    @PostMapping
    public Result add(@RequestBody DefectStatus status) {
        service.add(status);
        return Result.success(status);
    }

    @RequiresPermission("DEFECT_HANDLE")
    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @RequestBody DefectStatus status) {
        status.setId(id);
        service.update(status);
        return Result.success();
    }

    @RequiresPermission("DEFECT_MANAGE")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        service.delete(id);
        return Result.success();
    }
}
