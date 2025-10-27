package com.example.service;

import com.example.entity.Module;
import com.example.mapper.mysql.ModuleMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ModuleService {

    @Resource
    private ModuleMapper moduleMapper;

    /**
     * 分页查询所有模块
     */
    public PageInfo<Module> selectByPage(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Module> list = moduleMapper.selectAll();
        return PageInfo.of(list);
    }

    /**
     * 按模块名称模糊查询
     */
    public PageInfo<Module> selectByName(String name, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Module> list = moduleMapper.selectByName(name);
        return PageInfo.of(list);
    }

    /**
     * 按 projectId 查询（可选按 name 过滤）
     */
    public PageInfo<Module> selectByProjectId(Integer projectId, String name, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Module> list = moduleMapper.selectByProjectId(projectId, name);
        return PageInfo.of(list);
    }

    /**
     * 根据 id 查询单个模块
     */
    public Module selectById(Integer id) {
        return moduleMapper.selectById(id);
    }

    /**
     * 新增模块
     */
    public void addModule(Module module) {
        moduleMapper.insert(module);
    }

    /**
     * 更新模块
     */
    public void updateModule(Module module) {
        moduleMapper.update(module);
    }

    /**
     * 删除模块
     */
    public void deleteModule(Integer id) {
        moduleMapper.delete(id);
    }
}
