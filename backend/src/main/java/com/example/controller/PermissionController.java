package com.example.controller;

import com.example.service.PermissionService;
import com.example.common.Result;
import com.example.entity.Permission;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/permission")
public class PermissionController {
    @Resource
    private PermissionService permissionService;

    @GetMapping
    public Result selectByPage(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize) {
            PageInfo<Permission> pageInfo;
            pageInfo = permissionService.selectByPage(pageNum, pageSize);
            return Result.success(pageInfo);
    }
}

