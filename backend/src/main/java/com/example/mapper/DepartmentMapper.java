package com.example.mapper;

import com.example.entity.Department;
import java.util.List;

public interface DepartmentMapper {
    List<Department> selectAll();

    List<Department> selectByName(String name);

    Department selectById(Integer id);

    void addDepartment(Department department);

    void updateDepartment(Department department);

    void deleteDepartment(Integer id);
}
