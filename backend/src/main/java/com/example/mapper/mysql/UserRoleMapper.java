package com.example.mapper.mysql;

import com.example.entity.UserRole;

import java.util.List;

public interface UserRoleMapper {
    int insertUserRole(UserRole userRole);
    int deleteByUserId(Long userId);
    List<Long> selectRoleIdsByUserId(Long id);
}
