package com.example.service;

import com.example.entity.Defect;
import com.example.mapper.mysql.DefectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DefectService {

    @Resource
    private DefectMapper defectMapper;

    public PageInfo<Defect> selectByPage(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Defect> list = defectMapper.selectAll();
        return PageInfo.of(list);
    }

    public PageInfo<Defect> selectByFilters(Defect defect, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Defect> list = defectMapper.selectByFilters(defect);
        return PageInfo.of(list);
    }

    public Defect selectById(Integer id) {
        return defectMapper.selectById(id);
    }

    @Transactional
    public void addDefect(Defect defect) {
        defectMapper.insertDefect(defect);
    }

    @Transactional
    public void updateDefect(Defect defect) {
        defectMapper.updateDefect(defect);
    }

    @Transactional
    public void deleteDefect(Integer id) {
        defectMapper.deleteDefect(id);
    }

}
