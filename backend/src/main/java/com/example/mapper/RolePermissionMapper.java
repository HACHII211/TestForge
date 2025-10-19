package com.example.mapper;

import java.util.List;

public interface RolePermissionMapper {

    List<Integer> selectPermissionIdsByRoleId(Integer roleId);

    void deleteByRoleId(Integer roleId);

    void insertRolePermission(Integer roleId, Integer permissionId);

    List<Long> selectPermissionIdsByRoleIds(List<Long> roleIds);

}
