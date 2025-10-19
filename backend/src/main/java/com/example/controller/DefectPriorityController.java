package com.example.controller;

import com.example.common.Result;
import com.example.entity.DefectPriority;
import com.example.service.DefectPriorityService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/defect-priorities")
public class DefectPriorityController {

    @Resource
    private DefectPriorityService service;

    @GetMapping
    public Result list() {
        List<DefectPriority> list = service.selectAll();
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result get(@PathVariable Integer id) {
        return Result.success(service.selectById(id));
    }

    @PostMapping
    public Result add(@RequestBody DefectPriority p) {
        service.add(p);
        return Result.success(p);
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @RequestBody DefectPriority p) {
        p.setId(id);
        service.update(p);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        service.delete(id);
        return Result.success();
    }
}
