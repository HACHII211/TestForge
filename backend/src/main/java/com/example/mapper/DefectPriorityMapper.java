package com.example.mapper;

import com.example.entity.DefectPriority;
import java.util.List;

public interface DefectPriorityMapper {
    List<DefectPriority> selectAll();
    DefectPriority selectById(Integer id);
    int insert(DefectPriority p);
    int update(DefectPriority p);
    int deleteById(Integer id);
}
