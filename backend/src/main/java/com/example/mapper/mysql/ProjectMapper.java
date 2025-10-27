package com.example.mapper.mysql;

import com.example.entity.Project;
import java.util.List;

public interface ProjectMapper {
    List<Project> selectAll();

    List<Project> selectByName(String name);

    Project selectById(Integer id);

    void insert(Project project);

    void update(Project project);

    void delete(Integer id);
}
