package com.example.controller;

import com.example.security.RequiresPermission;
import com.example.service.DefectService;
import com.example.common.Result;
import com.example.entity.Defect;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/defect")
public class DefectController {

    @Resource
    private DefectService defectService;

    @GetMapping
    public Result selectDefects(
            Defect defect,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        PageInfo<Defect> pageInfo;
        if (defect != null) {
            pageInfo = defectService.selectByFilters(defect, pageNum, pageSize);
        } else {
            pageInfo = defectService.selectByPage(pageNum, pageSize);
        }
        return Result.success(pageInfo);
    }

    @GetMapping("/{id}")
    public Result selectById(@PathVariable Integer id) {
        Defect defect = defectService.selectById(id);
        return Result.success(defect);
    }

    @RequiresPermission("DEFECT_MANAGE")
    @PostMapping
    public Result addDefect(@RequestBody Defect defect) {
        defectService.addDefect(defect);
        return Result.success();
    }

    @RequiresPermission("DEFECT_MANAGE")
    @DeleteMapping("/{id}")
    public Result deleteDefect(@PathVariable Integer id) {
        defectService.deleteDefect(id);
        return Result.success();
    }

    @RequiresPermission("DEFECT_HANDLE")
    @PutMapping("/{id}")
    public Result updateDefect(@PathVariable Integer id, @RequestBody Defect defect) {
        defect.setId(id);
        defectService.updateDefect(defect);
        return Result.success();
    }
}
