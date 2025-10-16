package com.example.mapper;

import com.example.entity.UserRole;

public interface UserRoleMapper {
    int insertUserRole(UserRole userRole);
    int deleteByUserId(Integer userId);
}
