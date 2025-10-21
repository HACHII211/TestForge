package com.example.controller;

import com.example.common.Result;
import com.example.entity.Permission;
import com.example.security.RequiresPermission;
import com.example.service.PermissionService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permissions")
public class PermissionController {

    @Resource
    private PermissionService permissionService;

    @GetMapping
    public Result list() {
        List<Permission> list = permissionService.selectAll();
        return Result.success(list);
    }

    @RequiresPermission("ROLE_MANAGE")
    @PostMapping("/names")
    public Result namesByIds(@RequestBody List<Long> ids) {
        List<String> names = permissionService.selectNamesByIds(ids);
        return Result.success(names);
    }

    @RequiresPermission("ROLE_MANAGE")
    @PostMapping("/by-role-ids")
    public Result namesByRoleIds(@RequestBody List<Integer> roleIds) {
        List<String> names = permissionService.selectNamesByRoleIds(roleIds);
        return Result.success(names);
    }


}
