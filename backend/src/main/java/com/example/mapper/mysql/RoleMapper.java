package com.example.mapper.mysql;

import com.example.entity.Role;

import java.util.List;

public interface RoleMapper {
    List<Role> selectAll();

    List<Role> selectByName(String name);

    Role selectById(Integer id);

    void addRole(Role role);

    void updateRole(Role role);

    void deleteRole(Integer id);
}
