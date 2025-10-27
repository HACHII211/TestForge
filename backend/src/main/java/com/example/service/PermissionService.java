// com/example/service/PermissionService.java
package com.example.service;

import com.example.entity.Permission;
import com.example.mapper.mysql.PermissionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class PermissionService {

    @Resource
    private PermissionMapper permissionMapper;

    public List<Permission> selectAll() {
        List<Permission> list = permissionMapper.selectAll();
        return list == null ? Collections.emptyList() : list;
    }

    public Permission selectById(Integer id) {
        return permissionMapper.selectById(id);
    }

    public void add(Permission permission) {
        permissionMapper.insert(permission);
    }

    public void update(Permission permission) {
        permissionMapper.update(permission);
    }

    public void delete(Integer id) {
        permissionMapper.deleteById(id);
    }

    public List<String> selectNamesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return permissionMapper.selectNamesByIds(ids);
    }

    public List<String> selectNamesByRoleIds(List<Integer> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return Collections.emptyList();
        return permissionMapper.selectNamesByRoleIds(roleIds);
    }
}
