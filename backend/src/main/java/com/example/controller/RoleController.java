package com.example.controller;

import com.example.service.RoleService;
import com.example.common.Result;
import com.example.entity.Role;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/role")
public class RoleController {

    @Resource
    private RoleService roleService;

    @GetMapping
    public Result selectRoles(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Role> pageInfo;
        if (name != null && !name.isEmpty()) {
            pageInfo = roleService.selectByName(name, pageNum, pageSize);
        } else {
            pageInfo = roleService.selectByPage(pageNum, pageSize);
        }
        return Result.success(pageInfo);
    }

    @GetMapping("/{id}")
    public Result selectById(@PathVariable Integer id) {
        Role role = roleService.selectById(id);
        return Result.success(role);
    }

    @PostMapping
    public Result addRole(@RequestBody Role role) {
        roleService.addRole(role);
        // 如果前端同时传 permissionIds，Service 可选择在 add 时处理（此处仅新增 role）
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result deleteRole(@PathVariable Integer id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result updateRole(@PathVariable Integer id, @RequestBody Role role) {
        role.setId(id);
        roleService.updateRole(role);
        return Result.success();
    }

    @GetMapping("/{id}/permissions")
    public Result getRolePermissions(@PathVariable Integer id) {
        List<Integer> perms = roleService.getPermissionIdsByRoleId(id);
        return Result.success(perms);
    }

    @PostMapping("/{id}/permissions")
    public Result setRolePermissions(@PathVariable Integer id, @RequestBody List<Integer> permissionIds) {
        roleService.setPermissionsForRole(id, permissionIds);
        return Result.success();
    }
}
