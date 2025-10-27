package com.example.service;

import com.example.entity.DefectStatus;
import com.example.mapper.mysql.DefectStatusMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefectStatusService {

    @Resource
    private DefectStatusMapper mapper;

    public List<DefectStatus> selectAll() {
        return mapper.selectAll();
    }

    public DefectStatus selectById(Integer id) {
        return mapper.selectById(id);
    }

    public void add(DefectStatus status) {
        mapper.insert(status);
    }

    public void update(DefectStatus status) {
        mapper.update(status);
    }

    public void delete(Integer id) {
        mapper.deleteById(id);
    }
}
