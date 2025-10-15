package com.example.mapper;

import com.example.entity.Permission;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PermissionMapper {
    @Select("select * from `permission`")
    List<Permission> selectAll();
}
