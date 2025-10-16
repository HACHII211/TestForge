package com.example.mapper;

import com.example.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProjectUserMapper {

    // 查询某项目成员（分页）
    List<User> selectUsersByProjectId(@Param("projectId") Integer projectId);

    // 统计成员是否已存在
    Integer countUserInProject(@Param("projectId") Integer projectId, @Param("userId") Integer userId);

    // 添加成员
    void insertProjectUser(@Param("projectId") Integer projectId, @Param("userId") Integer userId);

    // 移除成员
    void deleteProjectUser(@Param("projectId") Integer projectId, @Param("userId") Integer userId);

    // 批量移除成员
    void deleteProjectUserBatch(@Param("projectId") Integer projectId, @Param("userIds") List<Integer> userIds);
}
