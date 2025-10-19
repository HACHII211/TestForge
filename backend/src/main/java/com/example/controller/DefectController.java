package com.example.controller;

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

    /**
     * 分页查询缺陷，可按 title 模糊搜索
     */
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

    /**
     * 根据 ID 查询缺陷
     */
    @GetMapping("/{id}")
    public Result selectById(@PathVariable Integer id) {
        Defect defect = defectService.selectById(id);
        return Result.success(defect);
    }

    /**
     * 新增缺陷
     */
    @PostMapping
    public Result addDefect(@RequestBody Defect defect) {
        defectService.addDefect(defect);
        return Result.success();
    }

    /**
     * 删除缺陷
     */
    @DeleteMapping("/{id}")
    public Result deleteDefect(@PathVariable Integer id) {
        defectService.deleteDefect(id);
        return Result.success();
    }

    /**
     * 更新缺陷（整体更新）
     */
    @PutMapping("/{id}")
    public Result updateDefect(@PathVariable Integer id, @RequestBody Defect defect) {
        defect.setId(id);
        defectService.updateDefect(defect);
        return Result.success();
    }
}
