package com.example.service;

import com.example.entity.Department;
import com.example.mapper.DepartmentMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    @Resource
    private DepartmentMapper departmentMapper;

    public PageInfo<Department> selectByPage(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Department> list = departmentMapper.selectAll();
        return PageInfo.of(list);
    }

    public PageInfo<Department> selectByName(String name, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Department> list = departmentMapper.selectByName(name);
        return PageInfo.of(list);
    }

    public Department selectById(Integer id) {
        return departmentMapper.selectById(id);
    }

    public void addDepartment(Department department) {
        departmentMapper.addDepartment(department);
    }

    public void deleteDepartment(Integer id) {
        departmentMapper.deleteDepartment(id);
    }

    public void updateDepartment(Department department) {
        departmentMapper.updateDepartment(department);
    }
}
