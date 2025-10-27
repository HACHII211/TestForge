package com.example.service;

import com.example.entity.DefectPriority;
import com.example.mapper.mysql.DefectPriorityMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefectPriorityService {

    @Resource
    private DefectPriorityMapper mapper;

    public List<DefectPriority> selectAll() { return mapper.selectAll(); }
    public DefectPriority selectById(Integer id) { return mapper.selectById(id); }
    public void add(DefectPriority p) { mapper.insert(p); }
    public void update(DefectPriority p) { mapper.update(p); }
    public void delete(Integer id) { mapper.deleteById(id); }
}
