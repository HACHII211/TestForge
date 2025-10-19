// com/example/mapper/PermissionMapper.java
package com.example.mapper;

import com.example.entity.Permission;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PermissionMapper {

    List<Permission> selectAll();

    List<String> selectNamesByIds(@Param("ids") List<Long> ids);

    List<String> selectNamesByRoleIds(@Param("roleIds") List<Integer> roleIds);

    List<Permission> selectByIds(@Param("ids") List<Integer> ids);

    Permission selectById(Integer id);

    int insert(Permission permission);

    int update(Permission permission);

    int deleteById(Integer id);
}
