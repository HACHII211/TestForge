package com.example.mapper.mysql;

import com.example.entity.Module;
import java.util.List;

public interface ModuleMapper {
    List<Module> selectAll();

    List<Module> selectByName(String name);

    List<Module> selectByProjectId(Integer projectId, String name);

    Module selectById(Integer id);

    void insert(Module module);

    void update(Module module);

    void delete(Integer id);
}
