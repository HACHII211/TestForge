// 文件: com/example/service/ProjectService.java
package com.example.service;

import com.example.entity.Project;
import com.example.mapper.ProjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    @Resource
    private ProjectMapper projectMapper;

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
