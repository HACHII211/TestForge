// 文件: com/example/service/ProjectService.java
package com.example.service;

import com.example.entity.Project;
import com.example.entity.User;
import com.example.mapper.ProjectMapper;
import com.example.mapper.ProjectUserMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProjectUserMapper projectUserMapper;
    /**
     * 分页查询所有项目
     */
    public PageInfo<Project> selectByPage(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Project> list = projectMapper.selectAll();
        return PageInfo.of(list);
    }

    /**
     * 按项目名称模糊搜索
     */
    public PageInfo<Project> selectByName(String name, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Project> list = projectMapper.selectByName(name);
        return PageInfo.of(list);
    }


    public PageInfo<User> selectProjectUsers(Integer projectId, Integer pageNum, Integer pageSize) {
        if (projectId == null) return PageInfo.of(List.of());
        PageHelper.startPage(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        List<User> list = projectUserMapper.selectUsersByProjectId(projectId);
        return PageInfo.of(list);
    }
    public void addUserToProject(Integer projectId, Integer userId) {
        if (projectId == null || userId == null) return;
        // 先检查是否已存在
        Integer exists = projectUserMapper.countUserInProject(projectId, userId);
        if (exists == null || exists == 0) {
            projectUserMapper.insertProjectUser(projectId, userId);
        }
    }

    // 移除成员
    public void removeUserFromProject(Integer projectId, Integer userId) {
        if (projectId == null || userId == null) return;
        projectUserMapper.deleteProjectUser(projectId, userId);
    }

    // 批量移除成员
    public void removeUsersFromProjectBatch(Integer projectId, List<Integer> userIds) {
        if (projectId == null || userIds == null || userIds.isEmpty()) return;
        projectUserMapper.deleteProjectUserBatch(projectId, userIds);
    }

    /**
     * 根据 id 查询单个项目
     */
    public Project selectById(Integer id) {
        return projectMapper.selectById(id);
    }

    /**
     * 新增项目
     */
    public void addProject(Project project) {
        projectMapper.insert(project);
    }

    /**
     * 更新项目
     */
    public void updateProject(Project project) {
        projectMapper.update(project);
    }

    /**
     * 删除项目
     */
    public void deleteProject(Integer id) {
        projectMapper.delete(id);
    }
}
