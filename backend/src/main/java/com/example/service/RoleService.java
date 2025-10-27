package com.example.service;

import com.example.entity.Role;
import com.example.mapper.mysql.RoleMapper;
import com.example.mapper.mysql.RolePermissionMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private RolePermissionMapper rolePermissionMapper;

    public PageInfo<Role> selectByPage(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Role> list = roleMapper.selectAll();
        return PageInfo.of(list);
    }

    public PageInfo<Role> selectByName(String name, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Role> list = roleMapper.selectByName(name);
        return PageInfo.of(list);
    }

    public Role selectById(Integer id) {
        Role role = roleMapper.selectById(id);
        // 可同时加载权限 id 列表（若需要）
        List<Integer> perms = rolePermissionMapper.selectPermissionIdsByRoleId(id);
        role.setPermissionIds(perms);
        return role;
    }

    public void addRole(Role role) {
        roleMapper.addRole(role);
        // 如果前端传 permissionIds，保存 role 后可以插入 role_permission
        if (role.getPermissionIds() != null && !role.getPermissionIds().isEmpty()) {
            Integer roleId = role.getId(); // 依赖 mapper useGeneratedKeys 回填 id
            rolePermissionMapper.deleteByRoleId(roleId);
            for (Integer pid : role.getPermissionIds()) {
                rolePermissionMapper.insertRolePermission(roleId, pid);
            }
        }
    }

    public void updateRole(Role role) {
        roleMapper.updateRole(role);
        // 若传入 permissionIds，则更新 role_permission
        if (role.getPermissionIds() != null) {
            Integer roleId = role.getId();
            rolePermissionMapper.deleteByRoleId(roleId);
            for (Integer pid : role.getPermissionIds()) {
                rolePermissionMapper.insertRolePermission(roleId, pid);
            }
        }
    }

    public void deleteRole(Integer id) {
        // 删除角色，同时删除 role_permission 中的映射
        rolePermissionMapper.deleteByRoleId(id);
        roleMapper.deleteRole(id);
    }

    /**
     * 查询角色对应的 permission id 列表
     */
    public List<Integer> getPermissionIdsByRoleId(Integer roleId) {
        return rolePermissionMapper.selectPermissionIdsByRoleId(roleId);
    }

    /**
     * 覆盖式设置角色权限（事务）
     */
    @Transactional
    public void setPermissionsForRole(Integer roleId, List<Integer> permissionIds) {
        // 先删后增（覆盖）
        rolePermissionMapper.deleteByRoleId(roleId);
        if (permissionIds == null || permissionIds.isEmpty()) return;
        for (Integer pid : permissionIds) {
            rolePermissionMapper.insertRolePermission(roleId, pid);
        }
    }
}
